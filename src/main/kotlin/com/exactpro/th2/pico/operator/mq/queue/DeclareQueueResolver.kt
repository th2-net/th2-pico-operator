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

package com.exactpro.th2.pico.operator.mq.queue

import com.exactpro.th2.pico.operator.config.ConfigLoader
import com.exactpro.th2.pico.operator.mq.RabbitMQManager
import com.exactpro.th2.pico.operator.repo.BoxResource
import com.exactpro.th2.pico.operator.schemaName
import com.rabbitmq.client.Channel
import mu.KotlinLogging

class DeclareQueueResolver(val resource: BoxResource) {
    private val logger = KotlinLogging.logger { }
    private val resourceName = resource.metadata.name

    fun handle(): Set<String> {
        try {
            return declareQueues()
        } catch (e: Exception) {
            val message = "Exception while working with rabbitMq"
            logger.error(message, e)
            throw e
        }
    }

    private fun declareQueues(): Set<String> {
        val declaredQueues: MutableSet<String> = HashSet()
        val channel: Channel = RabbitMQManager.channel
        val persistence: Boolean = ConfigLoader.config.rabbitMQManagement.persistence
        // get queues that are associated with current box and are not linked through Th2Link resources
        for (pin in resource.spec.pins?.mq?.subscribers ?: emptyList()) {
            val queue: String = Queue(schemaName, resourceName, pin.name).toString()
            val newQueueArguments = RabbitMQManager.generateQueueArguments(pin.settings)
            val currentQueue = RabbitMQManager.getQueue(queue)
            if (currentQueue != null && !currentQueue.arguments.equals(newQueueArguments)) {
                logger.warn("Arguments for queue '{}' were modified. Recreating with new arguments", queue)
                channel.queueDelete(queue)
            }
            val declareResult = channel.queueDeclare(
                queue,
                persistence,
                false,
                false,
                newQueueArguments
            )
            declaredQueues.add(queue)
            logger.info(
                "Queue '{}' of resource {} was successfully declared",
                declareResult.queue,
                resourceName
            )
        }
        return declaredQueues
    }
}
