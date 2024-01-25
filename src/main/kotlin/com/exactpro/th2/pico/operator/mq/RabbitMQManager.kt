/*
 * Copyright 2022-2024 Exactpro (Exactpro Systems Limited)
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
import com.exactpro.th2.pico.operator.config.fields.DefaultConfigNames
import com.exactpro.th2.pico.operator.config.fields.DefaultSchemaConfigs
import com.exactpro.th2.pico.operator.config.fields.RabbitMQManagementConfig
import com.exactpro.th2.pico.operator.mq.queue.Queue
import com.exactpro.th2.pico.operator.util.Mapper.JSON_MAPPER
import com.fasterxml.jackson.module.kotlin.readValue
import com.rabbitmq.http.client.Client
import com.rabbitmq.http.client.ClientParameters
import com.rabbitmq.http.client.domain.BindingInfo
import com.rabbitmq.http.client.domain.QueueInfo
import com.rabbitmq.http.client.domain.UserPermissions
import mu.KotlinLogging
import java.io.IOException
import kotlin.io.path.inputStream

class RabbitMQManager(
    private val managementConfig: RabbitMQManagementConfig,
    private val defaultSchemaConfigs: DefaultSchemaConfigs,
    val schemaName: String,
) {
    private val logger = KotlinLogging.logger { }

    private val client: Client = Client(
        ClientParameters()
            .url("http://${managementConfig.host}:${managementConfig.managementPort}/api")
            .username(managementConfig.username)
            .password(managementConfig.password)
    )

    val channel = RabbitMqChannelFactory.create(managementConfig)
    val persistence = managementConfig.persistence

    fun creteInitialSetup() {
        declareTopicExchange()
        setUpRabbitMqForSchema()
    }

    fun cleanupRabbit() {
        removeSchemaExchange()
        removeSchemaQueues()
        removeSchemaUser()
    }

    fun getAllQueues(): List<QueueInfo> = client.getQueues(managementConfig.vhostName)

    fun getQueueBindings(queue: String?): List<BindingInfo> = client.getQueueBindings(managementConfig.vhostName, queue)

    fun getQueue(queueName: String): QueueInfo? = client.getQueue(managementConfig.vhostName, queueName)

    fun generateQueueArguments(pinSettings: PinSettings?): Map<String, Any?> {
        if (pinSettings == null) {
            return emptyMap()
        }
        return if (pinSettings.storageOnDemand!!) {
            emptyMap()
        } else {
            val args: MutableMap<String, Any?> = HashMap()
            args["x-max-length"] = pinSettings.queueLength ?: DEFAULT_QUEUE_LENGTH
            args["x-overflow"] = pinSettings.overloadStrategy ?: DEFAULT_STRATEGY
            args
        }
    }

    fun closeChannel() {
        channel.close()
    }

    private fun declareTopicExchange() {
        val exchangeName = managementConfig.exchangeName
        try {
            channel.exchangeDeclare(exchangeName, "topic", persistence)
        } catch (e: Exception) {
            logger.error("Exception setting up exchange: \"{}\"", exchangeName, e)
        }
    }

    private fun setUpRabbitMqForSchema() {
        try {
            createUser()
            declareExchange()
        } catch (e: Exception) {
            logger.error("Exception setting up rabbitMq for schema: \"{}\"", schemaName, e)
        }
    }

    private fun createUser() {
        val password = getUserPassword()
        val vHostName = managementConfig.vhostName
        if (schemaName.isEmpty()) {
            return
        }
        try {
            if (client.getVhost(vHostName) == null) {
                logger.error("vHost: \"{}\" is not present", vHostName)
                return
            }
            client.createUser(schemaName, password.toCharArray(), emptyList())
            logger.info("Created user \"{}\" on vHost \"{}\"", schemaName, vHostName)

            // set permissions
            val (configure, read, write) = managementConfig.schemaPermissions
            val permissions = UserPermissions()
            permissions.configure = configure
            permissions.read = read
            permissions.write = write
            client.updatePermissions(vHostName, schemaName, permissions)
            logger.info("User \"{}\" permissions set in RabbitMQ", schemaName)
        } catch (e: Exception) {
            logger.error("Exception setting up user: \"{}\" for vHost: \"{}\"", schemaName, vHostName, e)
            throw e
        }
    }

    private fun declareExchange() {
        try {
            channel.exchangeDeclare(schemaName, "direct", persistence)
        } catch (e: Exception) {
            logger.error("Exception setting up exchange: \"{}\"", schemaName, e)
            throw e
        }
    }

    private fun removeSchemaUser() {
        val vHostName = managementConfig.vhostName
        if (client.getVhost(vHostName) == null) {
            logger.error("vHost: \"{}\" is not present", vHostName)
            return
        }
        client.deleteUser(schemaName)
        logger.info("Deleted user \"{}\" from vHost \"{}\"", schemaName, vHostName)
    }

    private fun removeSchemaExchange() {
        try {
            channel.exchangeDelete(schemaName)
        } catch (e: Exception) {
            logger.error("Exception deleting exchange: \"{}\"", schemaName, e)
        }
    }

    private fun removeSchemaQueues() {
        try {
            getAllQueues().forEach {
                val queueName = it.name
                val queue = Queue.fromString(queueName)
                if (queue?.schemaName == schemaName) {
                    try {
                        channel.queueDelete(queueName)
                        logger.info("Deleted queue: [{}]", queueName)
                    } catch (e: IOException) {
                        logger.error("Exception deleting queue: [{}]", queueName, e)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Exception cleaning up queues for: \"{}\"", schemaName, e)
        }
    }

    private fun getUserPassword(): String {
        return requireNotNull(defaultSchemaConfigs.configNames[DefaultConfigNames.rabbitMQ]) {
            "th2 config name for '${DefaultConfigNames.rabbitMQ}; isn't declared in config"
        }.run(defaultSchemaConfigs.location::resolve).inputStream().use {
            requireNotNull(JSON_MAPPER.readValue<Map<String, String>>(it)[PASSWORD_FIELD]) {
                "'$PASSWORD_FIELD' field isn't declared in the ${DefaultConfigNames.rabbitMQ} config"
            }
        }
    }

    companion object {
        private const val DEFAULT_QUEUE_LENGTH = 1000
        private const val DEFAULT_STRATEGY = "drop-head"
        private const val PASSWORD_FIELD = "password"
    }
}
