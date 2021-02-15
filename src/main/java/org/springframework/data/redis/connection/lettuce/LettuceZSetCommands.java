/*
 * Copyright 2017-2021 the original author or authors.
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
package org.springframework.data.redis.connection.lettuce;

import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.ScoredValueScanCursor;
import io.lettuce.core.ZStoreArgs;
import io.lettuce.core.api.async.RedisSortedSetAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;

import java.util.List;
import java.util.Set;

import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.connection.convert.Converters;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.KeyBoundCursor;
import org.springframework.data.redis.core.ScanIteration;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Andrey Shlykov
 * @since 2.0
 */
class LettuceZSetCommands implements RedisZSetCommands {

	private final LettuceConnection connection;

	LettuceZSetCommands(LettuceConnection connection) {
		this.connection = connection;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zAdd(byte[], double, byte[])
	 */
	@Override
	public Boolean zAdd(byte[] key, double score, byte[] value) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		return connection.invoke().from(RedisSortedSetAsyncCommands::zadd, key, score, value)
				.get(LettuceConverters.longToBoolean());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zAdd(byte[], java.util.Set)
	 */
	@Override
	public Long zAdd(byte[] key, Set<Tuple> tuples) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(tuples, "Tuples must not be null!");

		return connection.invoke().just(RedisSortedSetAsyncCommands::zadd, key,
				LettuceConverters.toObjects(tuples).toArray());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRem(byte[], byte[][])
	 */
	@Override
	public Long zRem(byte[] key, byte[]... values) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(values, "Values must not be null!");
		Assert.noNullElements(values, "Values must not contain null elements!");

		return connection.invoke().just(RedisSortedSetAsyncCommands::zrem, key, values);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zIncrBy(byte[], double, byte[])
	 */
	@Override
	public Double zIncrBy(byte[] key, double increment, byte[] value) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		return connection.invoke().just(RedisSortedSetAsyncCommands::zincrby, key, increment, value);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRank(byte[], byte[])
	 */
	@Override
	public Long zRank(byte[] key, byte[] value) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		return connection.invoke().just(RedisSortedSetAsyncCommands::zrank, key, value);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRank(byte[], byte[])
	 */
	@Override
	public Long zRevRank(byte[] key, byte[] value) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().just(RedisSortedSetAsyncCommands::zrevrank, key, value);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRange(byte[], long, long)
	 */
	@Override
	public Set<byte[]> zRange(byte[] key, long start, long end) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zrange, key, start, end).toSet();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeWithScores(byte[], long, long)
	 */
	@Override
	public Set<Tuple> zRangeWithScores(byte[] key, long start, long end) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zrangeWithScores, key, start, end)
				.toSet(LettuceConverters::toTuple);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeByScoreWithScores(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range, org.springframework.data.redis.connection.RedisZSetCommands.Limit)
	 */
	@Override
	public Set<Tuple> zRangeByScoreWithScores(byte[] key, Range range, Limit limit) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range for ZRANGEBYSCOREWITHSCORES must not be null!");
		Assert.notNull(limit, "Limit must not be null!");

				if (limit.isUnlimited()) {
					return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zrangebyscoreWithScores, key,
							LettuceConverters.<Number> toRange(range)).toSet(LettuceConverters::toTuple);
				}

				return connection.invoke()
						.fromMany(RedisSortedSetAsyncCommands::zrangebyscoreWithScores, key,
								LettuceConverters.<Number> toRange(range), LettuceConverters.toLimit(limit))
						.toSet(LettuceConverters::toTuple);

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRange(byte[], long, long)
	 */
	@Override
	public Set<byte[]> zRevRange(byte[] key, long start, long end) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zrevrange, key, start, end)
				.toSet(Converters.identityConverter());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRangeWithScores(byte[], long, long)
	 */
	@Override
	public Set<Tuple> zRevRangeWithScores(byte[] key, long start, long end) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zrevrangeWithScores, key, start, end)
				.toSet(LettuceConverters::toTuple);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRangeByScore(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range, org.springframework.data.redis.connection.RedisZSetCommands.Limit)
	 */
	@Override
	public Set<byte[]> zRevRangeByScore(byte[] key, Range range, Limit limit) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range for ZREVRANGEBYSCORE must not be null!");
		Assert.notNull(limit, "Limit must not be null!");

				if (limit.isUnlimited()) {

					return connection.invoke()
							.fromMany(RedisSortedSetAsyncCommands::zrevrangebyscore, key, LettuceConverters.<Number> toRange(range))
							.toSet();
				}

				return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zrevrangebyscore, key,
						LettuceConverters.<Number> toRange(range), LettuceConverters.toLimit(limit)).toSet();

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRangeByScoreWithScores(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range, org.springframework.data.redis.connection.RedisZSetCommands.Limit)
	 */
	@Override
	public Set<Tuple> zRevRangeByScoreWithScores(byte[] key, Range range, Limit limit) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range for ZREVRANGEBYSCOREWITHSCORES must not be null!");
		Assert.notNull(limit, "Limit must not be null!");

				if (limit.isUnlimited()) {
					return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zrevrangebyscoreWithScores, key,
							LettuceConverters.<Number> toRange(range)).toSet(LettuceConverters::toTuple);
				}

				return connection.invoke()
						.fromMany(RedisSortedSetAsyncCommands::zrevrangebyscoreWithScores, key,
								LettuceConverters.<Number> toRange(range), LettuceConverters.toLimit(limit))
						.toSet(LettuceConverters::toTuple);

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zCount(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range)
	 */
	@Override
	public Long zCount(byte[] key, Range range) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().just(RedisSortedSetAsyncCommands::zcount, key,
				LettuceConverters.<Number> toRange(range));
	}

	/*
	* (non-Javadoc)
	* @see org.springframework.data.redis.connection.RedisZSetCommands#zLexCount(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range)
	*/
	@Override
	public Long zLexCount(byte[] key, Range range) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range must not be null!");

		return connection.invoke().just(RedisSortedSetAsyncCommands::zlexcount, key,
				LettuceConverters.<byte[]> toRange(range, true));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zCard(byte[])
	 */
	@Override
	public Long zCard(byte[] key) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().just(RedisSortedSetAsyncCommands::zcard, key);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zScore(byte[], byte[])
	 */
	@Override
	public Double zScore(byte[] key, byte[] value) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		return connection.invoke().just(RedisSortedSetAsyncCommands::zscore, key, value);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRemRange(byte[], long, long)
	 */
	@Override
	public Long zRemRange(byte[] key, long start, long end) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().just(RedisSortedSetAsyncCommands::zremrangebyrank, key, start, end);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRemRangeByLex(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range)
	 */
	@Override
	public Long zRemRangeByLex(byte[] key, Range range) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range must not be null for ZREMRANGEBYLEX!");

		return connection.invoke().just(RedisSortedSetAsyncCommands::zremrangebylex, key,
				LettuceConverters.<byte[]> toRange(range, true));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRemRangeByScore(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range)
	 */
	@Override
	public Long zRemRangeByScore(byte[] key, Range range) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range for ZREMRANGEBYSCORE must not be null!");

		return connection.invoke().just(RedisSortedSetAsyncCommands::zremrangebyscore, key,
				LettuceConverters.<Number> toRange(range));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zUnionStore(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Aggregate, org.springframework.data.redis.connection.RedisZSetCommands.Weights, byte[][])
	 */
	@Override
	public Long zUnionStore(byte[] destKey, Aggregate aggregate, Weights weights, byte[]... sets) {

		Assert.notNull(destKey, "Destination key must not be null!");
		Assert.notNull(sets, "Source sets must not be null!");
		Assert.noNullElements(sets, "Source sets must not contain null elements!");
		Assert.isTrue(weights.size() == sets.length, () -> String
				.format("The number of weights (%d) must match the number of source sets (%d)!", weights.size(), sets.length));

		ZStoreArgs storeArgs = zStoreArgs(aggregate, weights);

		return connection.invoke().just(RedisSortedSetAsyncCommands::zunionstore, destKey, storeArgs, sets);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zUnionStore(byte[], byte[][])
	 */
	@Override
	public Long zUnionStore(byte[] destKey, byte[]... sets) {

		Assert.notNull(destKey, "Destination key must not be null!");
		Assert.notNull(sets, "Source sets must not be null!");
		Assert.noNullElements(sets, "Source sets must not contain null elements!");

		return connection.invoke().just(RedisSortedSetAsyncCommands::zunionstore, destKey, sets);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zInterStore(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Aggregate, org.springframework.data.redis.connection.RedisZSetCommands.Weights, byte[][])
	 */
	@Override
	public Long zInterStore(byte[] destKey, Aggregate aggregate, Weights weights, byte[]... sets) {

		Assert.notNull(destKey, "Destination key must not be null!");
		Assert.notNull(sets, "Source sets must not be null!");
		Assert.noNullElements(sets, "Source sets must not contain null elements!");
		Assert.isTrue(weights.size() == sets.length, () -> String
				.format("The number of weights (%d) must match the number of source sets (%d)!", weights.size(), sets.length));

		ZStoreArgs storeArgs = zStoreArgs(aggregate, weights);

		return connection.invoke().just(RedisSortedSetAsyncCommands::zinterstore, destKey, storeArgs, sets);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zInterStore(byte[], byte[][])
	 */
	@Override
	public Long zInterStore(byte[] destKey, byte[]... sets) {

		Assert.notNull(destKey, "Destination key must not be null!");
		Assert.notNull(sets, "Source sets must not be null!");
		Assert.noNullElements(sets, "Source sets must not contain null elements!");

		return connection.invoke().just(RedisSortedSetAsyncCommands::zinterstore, destKey, sets);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zScan(byte[], org.springframework.data.redis.core.ScanOptions)
	 */
	@Override
	public Cursor<Tuple> zScan(byte[] key, ScanOptions options) {
		return zScan(key, 0L, options);
	}

	/**
	 * @since 1.4
	 * @param key
	 * @param cursorId
	 * @param options
	 * @return
	 */
	public Cursor<Tuple> zScan(byte[] key, long cursorId, ScanOptions options) {

		Assert.notNull(key, "Key must not be null!");

		return new KeyBoundCursor<Tuple>(key, cursorId, options) {

			@Override
			protected ScanIteration<Tuple> doScan(byte[] key, long cursorId, ScanOptions options) {

				if (connection.isQueueing() || connection.isPipelined()) {
					throw new UnsupportedOperationException("'ZSCAN' cannot be called in pipeline / transaction mode.");
				}

				io.lettuce.core.ScanCursor scanCursor = connection.getScanCursor(cursorId);
				ScanArgs scanArgs = LettuceConverters.toScanArgs(options);

				ScoredValueScanCursor<byte[]> scoredValueScanCursor = connection.invoke()
						.just(RedisSortedSetAsyncCommands::zscan, key, scanCursor, scanArgs);
				String nextCursorId = scoredValueScanCursor.getCursor();

				List<ScoredValue<byte[]>> result = scoredValueScanCursor.getValues();

				List<Tuple> values = connection.failsafeReadScanValues(result, LettuceConverters.scoredValuesToTupleList());
				return new ScanIteration<>(Long.valueOf(nextCursorId), values);
			}

			@Override
			protected void doClose() {
				LettuceZSetCommands.this.connection.close();
			}

		}.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeByScore(byte[], java.lang.String, java.lang.String)
	 */
	@Override
	public Set<byte[]> zRangeByScore(byte[] key, String min, String max) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zrangebyscore, key, min, max).toSet();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeByScore(byte[], java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public Set<byte[]> zRangeByScore(byte[] key, String min, String max, long offset, long count) {

		Assert.notNull(key, "Key must not be null!");

		return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zrangebyscore, key, min, max, offset, count)
				.toSet();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeByScore(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range, org.springframework.data.redis.connection.RedisZSetCommands.Limit)
	 */
	@Override
	public Set<byte[]> zRangeByScore(byte[] key, Range range, Limit limit) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range for ZRANGEBYSCORE must not be null!");
		Assert.notNull(limit, "Limit must not be null!");

		if (limit.isUnlimited()) {
			return connection.invoke()
					.fromMany(RedisSortedSetAsyncCommands::zrangebyscore, key, LettuceConverters.<Number> toRange(range)).toSet();
		}

		return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zrangebyscore, key,
				LettuceConverters.<Number> toRange(range), LettuceConverters.toLimit(limit)).toSet();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeByLex(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range, org.springframework.data.redis.connection.RedisZSetCommands.Limit)
	 */
	@Override
	public Set<byte[]> zRangeByLex(byte[] key, Range range, Limit limit) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range for ZRANGEBYLEX must not be null!");
		Assert.notNull(limit, "Limit must not be null!");

		if (limit.isUnlimited()) {
			return connection.invoke()
					.fromMany(RedisSortedSetAsyncCommands::zrangebylex, key, LettuceConverters.<byte[]> toRange(range, true))
					.toSet();
		}

		return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zrangebylex, key,
				LettuceConverters.<byte[]> toRange(range, true), LettuceConverters.toLimit(limit)).toSet();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRangeByLex(byte[], org.springframework.data.redis.connection.RedisZSetCommands.Range, org.springframework.data.redis.connection.RedisZSetCommands.Limit)
	 */
	@Override
	public Set<byte[]> zRevRangeByLex(byte[] key, Range range, Limit limit) {

		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(range, "Range for ZREVRANGEBYLEX must not be null!");
		Assert.notNull(limit, "Limit must not be null!");

		if (limit.isUnlimited()) {
			return connection.invoke()
					.fromMany(RedisSortedSetAsyncCommands::zrevrangebylex, key, LettuceConverters.<byte[]> toRange(range, true))
					.toSet();
		}

		return connection.invoke().fromMany(RedisSortedSetAsyncCommands::zrevrangebylex, key,
				LettuceConverters.<byte[]> toRange(range, true), LettuceConverters.toLimit(limit)).toSet();
	}

	public RedisClusterCommands<byte[], byte[]> getConnection() {
		return connection.getConnection();
	}

	private static ZStoreArgs zStoreArgs(@Nullable Aggregate aggregate, Weights weights) {

		ZStoreArgs args = new ZStoreArgs();

		if (aggregate != null) {
			switch (aggregate) {
				case MIN:
					args.min();
					break;
				case MAX:
					args.max();
					break;
				default:
					args.sum();
					break;
			}
		}

		args.weights(weights.toArray());

		return args;
	}

}
