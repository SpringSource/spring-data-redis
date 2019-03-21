/*
 * Copyright 2011-2016 the original author or authors.
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

package org.springframework.data.redis.core;

import java.util.Collection;
import java.util.Set;

import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisZSetCommands.Limit;
import org.springframework.data.redis.connection.RedisZSetCommands.Range;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

/**
 * Default implementation for {@link BoundZSetOperations}.
 * 
 * @author Costin Leau
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class DefaultBoundZSetOperations<K, V> extends DefaultBoundKeyOperations<K>
		implements BoundZSetOperations<K, V> {

	private final ZSetOperations<K, V> ops;

	/**
	 * Constructs a new {@link DefaultBoundZSetOperations} instance.
	 * 
	 * @param key must not be {@literal null}.
	 * @param operations must not be {@literal null}.
	 */
	public DefaultBoundZSetOperations(K key, RedisOperations<K, V> operations) {

		super(key, operations);
		this.ops = operations.opsForZSet();
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#add(java.lang.Object, double)
	 */
	public Boolean add(V value, double score) {
		return ops.add(getKey(), value, score);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#add(java.util.Set)
	 */
	public Long add(Set<TypedTuple<V>> tuples) {
		return ops.add(getKey(), tuples);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#incrementScore(java.lang.Object, double)
	 */
	public Double incrementScore(V value, double delta) {
		return ops.incrementScore(getKey(), value, delta);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#getOperations()
	 */
	public RedisOperations<K, V> getOperations() {
		return ops.getOperations();
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#intersectAndStore(java.lang.Object, java.lang.Object)
	 */
	public void intersectAndStore(K otherKey, K destKey) {
		ops.intersectAndStore(getKey(), otherKey, destKey);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#intersectAndStore(java.util.Collection, java.lang.Object)
	 */
	public void intersectAndStore(Collection<K> otherKeys, K destKey) {
		ops.intersectAndStore(getKey(), otherKeys, destKey);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#range(long, long)
	 */
	public Set<V> range(long start, long end) {
		return ops.range(getKey(), start, end);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#rangeByScore(double, double)
	 */
	public Set<V> rangeByScore(double min, double max) {
		return ops.rangeByScore(getKey(), min, max);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#rangeByScoreWithScores(double, double)
	 */
	public Set<TypedTuple<V>> rangeByScoreWithScores(double min, double max) {
		return ops.rangeByScoreWithScores(getKey(), min, max);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#rangeWithScores(long, long)
	 */
	public Set<TypedTuple<V>> rangeWithScores(long start, long end) {
		return ops.rangeWithScores(getKey(), start, end);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#reverseRangeByScore(double, double)
	 */
	public Set<V> reverseRangeByScore(double min, double max) {
		return ops.reverseRangeByScore(getKey(), min, max);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#reverseRangeByScoreWithScores(double, double)
	 */
	public Set<TypedTuple<V>> reverseRangeByScoreWithScores(double min, double max) {
		return ops.reverseRangeByScoreWithScores(getKey(), min, max);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#reverseRangeWithScores(long, long)
	 */
	public Set<TypedTuple<V>> reverseRangeWithScores(long start, long end) {
		return ops.reverseRangeWithScores(getKey(), start, end);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#rangeByLex(org.springframework.data.redis.connection.RedisZSetCommands.Range)
	 */
	@Override
	public Set<V> rangeByLex(Range range) {
		return rangeByLex(range, null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#rangeByLex(org.springframework.data.redis.connection.RedisZSetCommands.Range, org.springframework.data.redis.connection.RedisZSetCommands.Limit)
	 */
	@Override
	public Set<V> rangeByLex(Range range, Limit limit) {
		return ops.rangeByLex(getKey(), range, limit);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#rank(java.lang.Object)
	 */
	public Long rank(Object o) {
		return ops.rank(getKey(), o);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#reverseRank(java.lang.Object)
	 */
	public Long reverseRank(Object o) {
		return ops.reverseRank(getKey(), o);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#score(java.lang.Object)
	 */
	public Double score(Object o) {
		return ops.score(getKey(), o);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#remove(java.lang.Object[])
	 */
	public Long remove(Object... values) {
		return ops.remove(getKey(), values);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#removeRange(long, long)
	 */
	public void removeRange(long start, long end) {
		ops.removeRange(getKey(), start, end);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#removeRangeByScore(double, double)
	 */
	public void removeRangeByScore(double min, double max) {
		ops.removeRangeByScore(getKey(), min, max);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#reverseRange(long, long)
	 */
	public Set<V> reverseRange(long start, long end) {
		return ops.reverseRange(getKey(), start, end);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#count(double, double)
	 */
	public Long count(double min, double max) {
		return ops.count(getKey(), min, max);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#size()
	 */
	@Override
	public Long size() {
		return zCard();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#zCard()
	 */
	@Override
	public Long zCard() {
		return ops.zCard(getKey());
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#unionAndStore(java.lang.Object, java.lang.Object)
	 */
	public void unionAndStore(K otherKey, K destKey) {
		ops.unionAndStore(getKey(), otherKey, destKey);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#unionAndStore(java.util.Collection, java.lang.Object)
	 */
	public void unionAndStore(Collection<K> otherKeys, K destKey) {
		ops.unionAndStore(getKey(), otherKeys, destKey);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundKeyOperations#getType()
	 */
	public DataType getType() {
		return DataType.ZSET;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.core.BoundZSetOperations#scan(org.springframework.data.redis.core.ScanOptions)
	 */
	@Override
	public Cursor<TypedTuple<V>> scan(ScanOptions options) {
		return ops.scan(getKey(), options);
	}
}
