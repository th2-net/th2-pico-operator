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

package com.exactpro.th2.pico.operator.mq.queue

import com.exactpro.th2.pico.operator.EVENT_STORAGE_BOX_ALIAS
import com.exactpro.th2.pico.operator.EVENT_STORAGE_PIN_ALIAS
import com.exactpro.th2.pico.operator.MESSAGE_STORAGE_BOX_ALIAS
import com.exactpro.th2.pico.operator.MESSAGE_STORAGE_PARSED_PIN_ALIAS
import com.exactpro.th2.pico.operator.MESSAGE_STORAGE_PIN_ALIAS
import com.exactpro.th2.pico.operator.mq.RabbitMQManager
import com.exactpro.th2.pico.operator.repo.BoxResource
import com.rabbitmq.http.client.domain.QueueInfo
import mu.KotlinLogging
import java.io.IOException

class QueuesProcessor(
    private val rabbitMQManager: RabbitMQManager,
) {
    private val logger = KotlinLogging.logger { }

    private val declaredQueues: MutableSet<String> = HashSet()

    private val estoreQueue = Queue(
        rabbitMQManager.schemaName,
        EVENT_STORAGE_BOX_ALIAS,
        EVENT_STORAGE_PIN_ALIAS
    ).toString()

    private val mstoreQueue = Queue(
        rabbitMQManager.schemaName,
        MESSAGE_STORAGE_BOX_ALIAS,
        MESSAGE_STORAGE_PIN_ALIAS
    ).toString()

    private val mstoreParsedQueue = Queue(
        rabbitMQManager.schemaName,
        MESSAGE_STORAGE_BOX_ALIAS,
        MESSAGE_STORAGE_PARSED_PIN_ALIAS
    ).toString()

    fun process(resource: BoxResource) {
        declaredQueues.addAll(DeclareQueueResolver(resource, rabbitMQManager).handle())
        BindQueueLinkResolver(resource, rabbitMQManager).handle()
    }

    fun removeUnusedQueues() {
        val allQueues: List<QueueInfo> = rabbitMQManager.getAllQueues()
        val channel = rabbitMQManager.channel
        for (queue in allQueues) {
            val queueName = queue.name
            if (queueName == estoreQueue || queueName == mstoreQueue || queueName == mstoreParsedQueue) {
                continue
            }
            if (!declaredQueues.contains(queueName)) {
                try {
                    channel.queueDelete(queueName)
                    logger.info("Deleted queue: [{}]", queueName)
                } catch (e: IOException) {
                    logger.error("Exception deleting queue: [{}]", queueName, e)
                    throw e
                }
            }
        }
    }

    fun createStoreQueues() {
        val persistence = rabbitMQManager.persistence
        val logMessage = "Queue \"{}\" was successfully declared"
        var declareResult = rabbitMQManager.channel.queueDeclare(
            estoreQueue,
            persistence,
            false,
            false,
            null
        )
        logger.info(logMessage, declareResult.queue)
        declareResult = rabbitMQManager.channel.queueDeclare(
            mstoreQueue,
            persistence,
            false,
            false,
            null
        )
        logger.info(logMessage, declareResult.queue)
        declareResult = rabbitMQManager.channel.queueDeclare(
            mstoreParsedQueue,
            persistence,
            false,
            false,
            null
        )
        logger.info(logMessage, declareResult.queue)
    }
}
