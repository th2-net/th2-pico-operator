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
import com.exactpro.th2.pico.operator.config.ConfigLoader
import com.exactpro.th2.pico.operator.config.fields.DefaultConfigNames
import com.exactpro.th2.pico.operator.config.fields.RabbitMQManagementConfig
import com.exactpro.th2.pico.operator.mq.queue.Queue
import com.exactpro.th2.pico.operator.schemaName
import com.exactpro.th2.pico.operator.util.Mapper.JSON_MAPPER
import com.fasterxml.jackson.module.kotlin.readValue
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

    private const val defaultQueueLength = 1000
    private const val defaultStrategy = "drop-head"

    private val managementConfig: RabbitMQManagementConfig = ConfigLoader.config.rabbitMQManagement

    private val client: Client = Client(
        ClientParameters()
            .url("http://${managementConfig.host}:${managementConfig.managementPort}/api")
            .username(managementConfig.username)
            .password(managementConfig.password)
    )

    val channel = RabbitMqChannelFactory.create(managementConfig)

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
            args["x-max-length"] = pinSettings.queueLength ?: defaultQueueLength
            args["x-overflow"] = pinSettings.overloadStrategy ?: defaultStrategy
            args
        }
    }

    fun closeChannel() {
        channel.close()
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
            client.createUser(schemaName, password.toCharArray(), ArrayList())
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
        val rabbitMQManagementConfig = managementConfig
        try {
            channel.exchangeDeclare(schemaName, "direct", rabbitMQManagementConfig.persistence)
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
        val defaultConfigs = ConfigLoader.config.defaultSchemaConfigs
        val rabbitMqAppConfig = Files.readString(
            Path("${defaultConfigs.location}/${defaultConfigs.configNames[DefaultConfigNames.rabbitMQ]}")
        )
        val cmData: Map<String, String> = JSON_MAPPER.readValue(rabbitMqAppConfig)
        return cmData["password"]!!
    }
}
