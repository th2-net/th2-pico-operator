/*
 * Copyright 2022-2022 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.th2.pico.operator.mq

import com.exactpro.th2.pico.operator.config.fields.RabbitMQManagementConfig
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import mu.KotlinLogging

object RabbitMqChannelFactory {
    private val logger = KotlinLogging.logger { }

    fun create(config: RabbitMQManagementConfig): Channel {
        val connectionFactory = ConnectionFactory()
        connectionFactory.host = config.host
        connectionFactory.port = config.applicationPort
        connectionFactory.virtualHost = config.vhostName
        connectionFactory.username = config.username
        connectionFactory.password = config.password
        try {
            val connection = connectionFactory.newConnection()
            return connection.createChannel()
        } catch (e: Exception) {
            logger.error("Exception while creating rabbitMq channel", e)
            throw e
        }
    }
}
