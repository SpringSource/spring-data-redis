/*
 * Copyright 2015-2020 the original author or authors.
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
package org.springframework.data.redis.listener;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

/**
 * @author Christoph Strobl
 */
@ExtendWith(MockitoExtension.class)
public class KeyExpirationEventMessageListenerTests {

	RedisMessageListenerContainer container;
	RedisConnectionFactory connectionFactory;
	KeyExpirationEventMessageListener listener;

	@Mock ApplicationEventPublisher publisherMock;

	@BeforeEach
	void beforeEach() {

		JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
		connectionFactory.afterPropertiesSet();
		this.connectionFactory = connectionFactory;

		container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.afterPropertiesSet();
		container.start();

		listener = new KeyExpirationEventMessageListener(container);
		listener.setApplicationEventPublisher(publisherMock);
		listener.init();
	}

	@AfterEach
	void tearDown() throws Exception {

		RedisConnection connection = connectionFactory.getConnection();
		try {
			connection.flushAll();
		} finally {
			connection.close();
		}

		listener.destroy();
		container.destroy();
		if (connectionFactory instanceof DisposableBean) {
			((DisposableBean) connectionFactory).destroy();
		}
	}

	@Test // DATAREDIS-425
	void listenerShouldPublishEventCorrectly() throws InterruptedException {

		byte[] key = ("to-expire:" + UUID.randomUUID().toString()).getBytes();

		setAndWaitForExpiry(key, connectionFactory.getConnection());

		ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);

		verify(publisherMock, times(1)).publishEvent(captor.capture());
		assertThat((byte[]) captor.getValue().getSource()).isEqualTo(key);
	}

	@Test // DATAREDIS-425
	void listenerShouldNotReactToDeleteEvents() throws InterruptedException {

		byte[] key = ("to-delete:" + UUID.randomUUID().toString()).getBytes();

		RedisConnection connection = connectionFactory.getConnection();
		try {

			connection.setEx(key, 10, "foo".getBytes());
			Thread.sleep(2000);
			connection.del(key);
			Thread.sleep(2000);
		} finally {
			connection.close();
		}

		Thread.sleep(2000);
		verifyZeroInteractions(publisherMock);
	}

	@Test // DATAREDIS-1075
	void databaseBoundListenerShouldNotReceiveEventsFromOtherDatabase() throws InterruptedException {

		ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

		listener = new KeyExpirationEventMessageListener(container, 0);
		listener.setApplicationEventPublisher(publisher);
		listener.init();

		byte[] key = ("to-expire:" + UUID.randomUUID().toString()).getBytes();

		RedisConnection connection = connectionFactory.getConnection();
		setAndWaitForExpiry(key, 1, connection);

		verify(publisherMock).publishEvent(any());
		verify(publisher, never()).publishEvent(any());
	}

	@Test // DATAREDIS-1075
	void databaseBoundListenerShouldReceiveEventsFromSelectedDatabase() throws InterruptedException {

		ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

		listener = new KeyExpirationEventMessageListener(container, 1);
		listener.setApplicationEventPublisher(publisher);
		listener.init();

		byte[] key = ("to-expire:" + UUID.randomUUID().toString()).getBytes();

		RedisConnection connection = connectionFactory.getConnection();
		setAndWaitForExpiry(key, 1, connection);

		ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);

		verify(publisherMock).publishEvent(any());
		verify(publisherMock).publishEvent(captor.capture());
		assertThat((byte[]) captor.getValue().getSource()).isEqualTo(key);
	}

	private void setAndWaitForExpiry(byte[] key, RedisConnection connection) throws InterruptedException {
		setAndWaitForExpiry(key, null, connection);
	}

	private void setAndWaitForExpiry(byte[] key, Integer database, RedisConnection connection)
			throws InterruptedException {

		if (database != null) {
			connection.select(database);
		}

		try {
			connection.setEx(key, 2, "foo".getBytes());

			int iteration = 0;
			while (connection.get(key) != null || iteration >= 3) {

				Thread.sleep(2000);
				iteration++;
			}
		} finally {
			connection.close();
		}

		Thread.sleep(2000);
	}
}
