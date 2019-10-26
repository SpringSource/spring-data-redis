/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.redis.cache;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link RedisCacheWriter} implementation capable of reading/writing binary data from/to Redis in {@literal standalone}
 * and {@literal cluster} environments. Works upon a given {@link RedisConnectionFactory} to obtain the actual
 * {@link RedisConnection}. <br />
 * {@link DefaultRedisCacheWriter} can be used in
 * {@link RedisCacheWriter#lockingRedisCacheWriter(RedisConnectionFactory) locking} or
 * {@link RedisCacheWriter#nonLockingRedisCacheWriter(RedisConnectionFactory) non-locking} mode. While
 * {@literal non-locking} aims for maximum performance it may result in overlapping, non atomic, command execution for
 * operations spanning multiple Redis interactions like {@code putIfAbsent}. The {@literal locking} counterpart prevents
 * command overlap by setting an explicit lock key and checking against presence of this key which leads to additional
 * requests and potential command wait times.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Joongsoo Park
 * @since 2.0
 */
class DefaultRedisCacheWriter implements RedisCacheWriter {

	private final RedisConnectionFactory connectionFactory;
	private final Duration sleepTime;

	/**
	 * @param connectionFactory must not be {@literal null}.
	 */
	DefaultRedisCacheWriter(RedisConnectionFactory connectionFactory) {
		this(connectionFactory, Duration.ZERO);
	}

	/**
	 * @param connectionFactory must not be {@literal null}.
	 * @param sleepTime sleep time between lock request attempts. Must not be {@literal null}. Use {@link Duration#ZERO}
	 *          to disable locking.
	 */
	DefaultRedisCacheWriter(RedisConnectionFactory connectionFactory, Duration sleepTime) {

		Assert.notNull(connectionFactory, "ConnectionFactory must not be null!");
		Assert.notNull(sleepTime, "SleepTime must not be null!");

		this.connectionFactory = connectionFactory;
		this.sleepTime = sleepTime;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.cache.RedisCacheWriter#put(java.lang.String, byte[], byte[], java.time.Duration)
	 */
	@Override
	public void put(String name, byte[] key, byte[] value, @Nullable Duration ttl) {

		Assert.notNull(name, "Name must not be null!");
		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		execute(connection -> {

			if (shouldExpireWithin(ttl)) {
				connection.set(key, value, Expiration.from(ttl.toMillis(), TimeUnit.MILLISECONDS), SetOption.upsert());
			} else {
				connection.set(key, value);
			}

			return "OK";
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.cache.RedisCacheWriter#get(java.lang.String, byte[])
	 */
	@Override
	public byte[] get(String name, byte[] key) {

		Assert.notNull(name, "Name must not be null!");
		Assert.notNull(key, "Key must not be null!");

		return execute(connection -> connection.get(key));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.cache.RedisCacheWriter#putIfAbsent(java.lang.String, byte[], byte[], java.time.Duration)
	 */
	@Override
	public byte[] putIfAbsent(String name, byte[] key, byte[] value, @Nullable Duration ttl) {

		Assert.notNull(name, "Name must not be null!");
		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		return execute(connection -> {
			boolean shouldExpireWithin = shouldExpireWithin(ttl);
			Long ttlMillis = shouldExpireWithin ? ttl.toMillis() : null;

			return connection.eval((
							"if (redis.call('setNX', KEYS[1], ARGV[1]) == 1) then " +
								"if (ARGV[2] == 'true') then " +
									"redis.call('pExpire', KEYS[1], ARGV[3]); " +
								"end; " +
								"return nil; " +
							"else " +
								"return redis.call('get', KEYS[1]); " +
							"end;"
					).getBytes(), ReturnType.VALUE, 1, key, value,
					String.valueOf(shouldExpireWithin).getBytes(),
					String.valueOf(ttlMillis).getBytes()
			);
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.cache.RedisCacheWriter#remove(java.lang.String, byte[])
	 */
	@Override
	public void remove(String name, byte[] key) {

		Assert.notNull(name, "Name must not be null!");
		Assert.notNull(key, "Key must not be null!");

		execute(connection -> connection.del(key));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.cache.RedisCacheWriter#clean(java.lang.String, byte[])
	 */
	@Override
	public void clean(String name, byte[] pattern) {

		Assert.notNull(name, "Name must not be null!");
		Assert.notNull(pattern, "Pattern must not be null!");

		execute(connection -> {
			if (isLockingCacheWriter()) {
				connection.eval((
						"local k = unpack(redis.call('keys', ARGV[1])); " +
						"if (k ~= nil) then " +
							"return redis.call('del', k); " +
						"end; " +
						"return 0;"
				).getBytes(), ReturnType.INTEGER, 0, pattern);
			} else {
				byte[][] keys = Optional.ofNullable(connection.keys(pattern)).orElse(Collections.emptySet())
						.toArray(new byte[0][]);

				if (keys.length > 0) {
					connection.del(keys);
				}
			}

			return "OK";
		});
	}

	/**
	 * @return {@literal true} if {@link RedisCacheWriter} uses locks.
	 */
	private boolean isLockingCacheWriter() {
		return !sleepTime.isZero() && !sleepTime.isNegative();
	}

	private <T> T execute(Function<RedisConnection, T> callback) {

		RedisConnection connection = connectionFactory.getConnection();
		try {
			return callback.apply(connection);
		} finally {
			connection.close();
		}
	}

	private static boolean shouldExpireWithin(@Nullable Duration ttl) {
		return ttl != null && !ttl.isZero() && !ttl.isNegative();
	}
}
