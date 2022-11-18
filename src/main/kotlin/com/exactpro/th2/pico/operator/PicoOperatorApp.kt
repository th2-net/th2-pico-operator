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

package com.exactpro.th2.pico.operator

import com.exactpro.th2.pico.operator.generator.ConfigHandler
import com.exactpro.th2.pico.operator.generator.ConfigProcessor
import com.exactpro.th2.pico.operator.mq.RabbitMQManager
import com.exactpro.th2.pico.operator.mq.queue.QueuesProcessor
import com.exactpro.th2.pico.operator.repo.RepositoryContext
import mu.KotlinLogging
import kotlin.system.exitProcess

const val EVENT_STORAGE_BOX_ALIAS = "estore"
const val EVENT_STORAGE_PIN_ALIAS = "estore-pin"

const val MESSAGE_STORAGE_BOX_ALIAS = "mstore"
const val MESSAGE_STORAGE_PIN_ALIAS = "mstore-pin"

private val logger = KotlinLogging.logger { }

fun main(args: Array<String>) {
    when (val mode = if (args.isNotEmpty()) args[0] else "full") {
        "full" -> {
            ConfigHandler.clearOldConfigs()
            ConfigHandler.copyDefaultConfigs()
            RabbitMQManager.creteInitialSetup()

            val queuesProcessor = QueuesProcessor()
            queuesProcessor.createStoreQueues()
            RepositoryContext.boxResources.values.forEach {
                ConfigProcessor(it).process()
                queuesProcessor.process(it)
            }
            queuesProcessor.removeUnusedQueues()
            RabbitMQManager.closeChannel()
        }
        "queues" -> {
            RabbitMQManager.creteInitialSetup()
            val queuesProcessor = QueuesProcessor()
            queuesProcessor.createStoreQueues()
            RepositoryContext.boxResources.values.forEach {
                queuesProcessor.process(it)
            }
            queuesProcessor.removeUnusedQueues()
            RabbitMQManager.closeChannel()
        }
        "configs" -> {
            ConfigHandler.clearOldConfigs()
            ConfigHandler.copyDefaultConfigs()

            RepositoryContext.boxResources.values.forEach {
                ConfigProcessor(it).process()
            }
        }
        else -> {
            logger.error("Command line argument: {} is not supported", mode)
        }
    }
    exitProcess(0)
}
