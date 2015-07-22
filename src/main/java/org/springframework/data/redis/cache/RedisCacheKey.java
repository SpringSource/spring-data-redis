/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.redis.cache;

import static org.springframework.util.Assert.*;

import java.util.Arrays;

import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * @author Christoph Strobl
 * @since 1.5
 */
public class RedisCacheKey {

	private final byte[] keyBytes;
	private final boolean hasPrefix;

	private RedisCacheKey(byte[] keyBytes, boolean hasPrefix) {
		this.keyBytes = keyBytes;
		this.hasPrefix = hasPrefix;
	}

	/**
	 * Get the {@link Byte} representation of the given key element using prefix if available.
	 */
	public byte[] getKeyBytes() {
		return keyBytes;
	}

	/**
	 * @return true if prefix is not empty.
	 */
	public boolean hasPrefix() {
		return hasPrefix;
	}

	/**
	 * @param keyElement must not be {@literal null}.
	 */
	public static RedisCacheKeyBuilder builder(final Object keyElement) {
		notNull(keyElement, "KeyElement must not be null!");
		return new RedisCacheKeyBuilder(keyElement);
	}

	public static class RedisCacheKeyBuilder {
		private Object keyElement;
		@SuppressWarnings("rawtypes")//
		private RedisSerializer serializer;
		private byte[] prefix;

		private RedisCacheKeyBuilder(Object keyElement) {
			this.keyElement = keyElement;
		}

		/**
		 * Use the given prefix when generating key.
		 *
		 * @param prefix can be {@literal null}.
		 * @return
		 */
		public RedisCacheKeyBuilder usePrefix(byte[] prefix) {
			this.prefix = prefix;
			return this;
		}

		/**
		 * Use {@link RedisSerializer} for converting the key into its {@link Byte} representation.
		 *
		 * @param serializer can be {@literal null}.
		 * @return
		 */
		public RedisCacheKeyBuilder withKeySerializer(RedisSerializer<?> serializer) {
			this.serializer = serializer;
			return this;
		}

		public RedisCacheKey build() {
			return new RedisCacheKey(getKeyBytes(), hasPrefix());
		}

		private boolean hasPrefix() {
			return (prefix != null && prefix.length > 0);
		}

		private byte[] getKeyBytes() {

			byte[] rawKey = serializeKeyElement();
			if (!hasPrefix()) {
				return rawKey;
			}

			byte[] prefixedKey = Arrays.copyOf(prefix, prefix.length + rawKey.length);
			System.arraycopy(rawKey, 0, prefixedKey, prefix.length, rawKey.length);

			return prefixedKey;
		}

		@SuppressWarnings("unchecked")
		private byte[] serializeKeyElement() {

			if (serializer == null && keyElement instanceof byte[]) {
				return (byte[]) keyElement;
			}

			return serializer.serialize(keyElement);
		}
	}
}
