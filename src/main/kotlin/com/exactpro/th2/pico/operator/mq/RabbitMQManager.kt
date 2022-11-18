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

import com.exactpro.th2.model.latest.box.pins.PinSettings
import com.exactpro.th2.pico.operator.EVENT_STORAGE_BOX_ALIAS
import com.exactpro.th2.pico.operator.EVENT_STORAGE_PIN_ALIAS
import com.exactpro.th2.pico.operator.MESSAGE_STORAGE_BOX_ALIAS
import com.exactpro.th2.pico.operator.MESSAGE_STORAGE_PIN_ALIAS
import com.exactpro.th2.pico.operator.config.ConfigLoader
import com.exactpro.th2.pico.operator.config.fields.DefaultConfigNames
import com.exactpro.th2.pico.operator.config.fields.RabbitMQManagementConfig
import com.exactpro.th2.pico.operator.mq.queue.Queue
import com.exactpro.th2.pico.operator.util.Mapper.JSON_MAPPER
import com.fasterxml.jackson.module.kotlin.readValue
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.http.client.Client
import com.rabbitmq.http.client.ClientParameters
import com.rabbitmq.http.client.domain.BindingInfo
import com.rabbitmq.http.client.domain.QueueInfo
import com.rabbitmq.http.client.domain.UserPermissions
import mu.KotlinLogging
import java.io.IOException
import java.nio.file.Files
import kotlin.io.path.Path

object RabbitMQManager {
    private val logger = KotlinLogging.logger { }

    private val schema = ConfigLoader.config.schemaName
    private val managementConfig: RabbitMQManagementConfig = ConfigLoader.config.rabbitMQManagement
    private var channelContext: ChannelContext? = null

    private val client: Client = Client(
        ClientParameters()
            .url("http://${managementConfig.host}:${managementConfig.managementPort}/api")
            .username(managementConfig.username)
            .password(managementConfig.password)
    )

    val channel: Channel
        get() {
            var channel = getChannelContext().channel
            if (!channel.isOpen) {
                logger.warn("RabbitMQ connection is broken, trying to reconnect...")
                getChannelContext().close()
                channel = getChannelContext().channel
                logger.info("RabbitMQ connection has been restored")
            }
            return channel
        }

    fun creteInitialSetup() {
        declareTopicExchange()
        setUpRabbitMqForSchema()
    }

    private fun declareTopicExchange() {
        val exchangeName = managementConfig.exchangeName
        try {
            channel.exchangeDeclare(exchangeName, "topic", managementConfig.persistence)
        } catch (e: Exception) {
            logger.error("Exception setting up exchange: \"{}\"", exchangeName, e)
        }
    }

    private fun setUpRabbitMqForSchema() {
        try {
            createUser()
            declareExchange()
            createStoreQueues()
        } catch (e: Exception) {
            logger.error("Exception setting up rabbitMq for schema: \"{}\"", schema, e)
        }
    }

    private fun createUser() {
        val password = getUserPassword()
        val vHostName = managementConfig.vhostName
        if (schema.isEmpty()) {
            return
        }
        try {
            if (client.getVhost(vHostName) == null) {
                logger.error("vHost: \"{}\" is not present", vHostName)
                return
            }
            client.createUser(schema, password.toCharArray(), ArrayList())
            logger.info("Created user \"{}\" on vHost \"{}\"", schema, vHostName)

            // set permissions
            val (configure, read, write) = managementConfig.schemaPermissions
            val permissions = UserPermissions()
            permissions.configure = configure
            permissions.read = read
            permissions.write = write
            client.updatePermissions(vHostName, schema, permissions)
            logger.info("User \"{}\" permissions set in RabbitMQ", schema)
        } catch (e: Exception) {
            logger.error("Exception setting up user: \"{}\" for vHost: \"{}\"", schema, vHostName, e)
            throw e
        }
    }

    private fun declareExchange() {
        val rabbitMQManagementConfig = managementConfig
        try {
            channel.exchangeDeclare(schema, "direct", rabbitMQManagementConfig.persistence)
        } catch (e: Exception) {
            logger.error("Exception setting up exchange: \"{}\"", schema, e)
            throw e
        }
    }

    private fun createStoreQueues() {
        var declareResult = channel.queueDeclare(
            Queue(
                schema,
                EVENT_STORAGE_BOX_ALIAS,
                EVENT_STORAGE_PIN_ALIAS
            ).toString(),
            managementConfig.persistence,
            false,
            false,
            null
        )
        logger.info("Queue \"{}\" was successfully declared", declareResult.queue)
        declareResult = channel.queueDeclare(
            Queue(
                schema,
                MESSAGE_STORAGE_BOX_ALIAS,
                MESSAGE_STORAGE_PIN_ALIAS
            ).toString(),
            managementConfig.persistence,
            false,
            false,
            null
        )
        logger.info("Queue \"{}\" was successfully declared", declareResult.queue)
    }

    fun cleanupRabbit() {
        removeSchemaExchange()
        removeSchemaQueues()
        removeSchemaUser()
    }

    private fun removeSchemaUser() {
        val vHostName = managementConfig.vhostName
        if (client.getVhost(vHostName) == null) {
            logger.error("vHost: \"{}\" is not present", vHostName)
            return
        }
        client.deleteUser(schema)
        logger.info("Deleted user \"{}\" from vHost \"{}\"", schema, vHostName)
    }

    private fun removeSchemaExchange() {
        try {
            channel.exchangeDelete(schema)
        } catch (e: Exception) {
            logger.error("Exception deleting exchange: \"{}\"", schema, e)
        }
    }

    private fun removeSchemaQueues() {
        try {
            getAllQueues().forEach {
                val queueName = it.name
                val queue = Queue.fromString(queueName)
                if (queue?.schemaName == schema) {
                    try {
                        channel.queueDelete(queueName)
                        logger.info("Deleted queue: [{}]", queueName)
                    } catch (e: IOException) {
                        logger.error("Exception deleting queue: [{}]", queueName, e)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Exception cleaning up queues for: \"{}\"", schema, e)
        }
    }

    fun generateQueueArguments(pinSettings: PinSettings?): Map<String, Any?> {
        if (pinSettings == null) {
            return emptyMap()
        }
        return if (pinSettings.storageOnDemand!!) {
            emptyMap()
        } else {
            val args: MutableMap<String, Any?> = HashMap()
            args["x-max-length"] = pinSettings.queueLength
            args["x-overflow"] = pinSettings.overloadStrategy
            args
        }
    }

    fun getAllQueues(): List<QueueInfo> = client.getQueues(managementConfig.vhostName)

    fun getQueueBindings(queue: String?): List<BindingInfo> = client.getQueueBindings(managementConfig.vhostName, queue)

    fun getQueue(queueName: String): QueueInfo? = client.getQueue(managementConfig.vhostName, queueName)

    private fun getChannelContext(): ChannelContext {
        // we do not need to synchronize as we are assigning immutable object from singleton
        if (channelContext == null) {
            channelContext = ChannelContext()
        }
        return channelContext!!
    }

    private fun getUserPassword(): String {
        val defaultConfigs = ConfigLoader.config.defaultSchemaConfigs
        val rabbitMqAppConfig = Files.readString(
            Path("${defaultConfigs.location}/${defaultConfigs.configNames[DefaultConfigNames.rabbitMQ]}")
        )
        val cmData: Map<String, String> = JSON_MAPPER.readValue(rabbitMqAppConfig)
        return cmData["password"]!!
    }

    internal class ChannelContext {
        var channel: Channel
        private var connection: Connection

        init {
            val rabbitMQManagementConfig = managementConfig
            val connectionFactory = ConnectionFactory()
            connectionFactory.host = rabbitMQManagementConfig.host
            connectionFactory.port = rabbitMQManagementConfig.applicationPort
            connectionFactory.virtualHost = rabbitMQManagementConfig.vhostName
            connectionFactory.username = rabbitMQManagementConfig.username
            connectionFactory.password = rabbitMQManagementConfig.password
            try {
                this.connection = connectionFactory.newConnection()
                this.channel = connection.createChannel()
            } catch (e: Exception) {
                close()
                val message = "Exception while creating rabbitMq channel"
                logger.error(message, e)
                throw e
            }
        }

        fun close() {
            try {
                if (channel.isOpen) {
                    channel.close()
                }
            } catch (e: Exception) {
                logger.error("Exception closing RabbitMQ channel", e)
            }
            try {
                if (connection.isOpen) {
                    connection.close()
                }
            } catch (e: Exception) {
                logger.error("Exception closing RabbitMQ connection for", e)
            }
            channelContext = null
        }
    }
}
