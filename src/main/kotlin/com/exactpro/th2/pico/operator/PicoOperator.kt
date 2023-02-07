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

import com.exactpro.th2.pico.operator.config.OperatorRunConfig
import com.exactpro.th2.pico.operator.generator.ConfigHandler
import com.exactpro.th2.pico.operator.generator.ConfigProcessor
import com.exactpro.th2.pico.operator.generator.ImageExtractor
import com.exactpro.th2.pico.operator.mq.RabbitMQManager
import com.exactpro.th2.pico.operator.mq.queue.QueuesProcessor
import com.exactpro.th2.pico.operator.repo.RepositoryContext
import mu.KotlinLogging

object PicoOperator {
    fun run(config: OperatorRunConfig) {
        when (val mode = config.mode) {
            "full" -> {
                ConfigHandler.clearOldConfigs()
                RabbitMQManager.creteInitialSetup()

                val queuesProcessor = QueuesProcessor().also {
                    it.createStoreQueues()
                }
                RepositoryContext.boxResources.values.forEach {
                    ConfigProcessor(it).process()
                    ImageExtractor.process(it)
                    queuesProcessor.process(it)
                }
                ConfigHandler.copyDefaultConfigs()
                ImageExtractor.saveImagesToFile()
                queuesProcessor.removeUnusedQueues()
                RabbitMQManager.closeChannel()
            }
            "queues" -> {
                RabbitMQManager.creteInitialSetup()
                val queuesProcessor = QueuesProcessor().also {
                    it.createStoreQueues()
                }
                RepositoryContext.boxResources.values.forEach {
                    queuesProcessor.process(it)
                }
                queuesProcessor.removeUnusedQueues()
                RabbitMQManager.closeChannel()
            }
            "configs" -> {
                ConfigHandler.clearOldConfigs()
                RepositoryContext.boxResources.values.forEach {
                    ConfigProcessor(it).process()
                    ImageExtractor.process(it)
                }
                ConfigHandler.copyDefaultConfigs()
                ImageExtractor.saveImagesToFile()
            }
            else -> {
                logger.error("Mode: {} is not supported", mode)
            }
        }
    }

    val logger = KotlinLogging.logger {}
}
