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

package com.exactpro.th2.pico.operator

import com.exactpro.th2.pico.operator.config.ApplicationConfig
import com.exactpro.th2.pico.operator.config.OperatorRunConfig
import com.exactpro.th2.pico.operator.generator.ConfigHandler
import com.exactpro.th2.pico.operator.generator.ConfigProcessor
import com.exactpro.th2.pico.operator.generator.ImageExtractor
import com.exactpro.th2.pico.operator.mq.RabbitMQManager
import com.exactpro.th2.pico.operator.mq.queue.QueuesProcessor
import com.exactpro.th2.pico.operator.repo.BoxResource
import com.exactpro.th2.pico.operator.repo.InfraMgrConfigResource
import com.exactpro.th2.pico.operator.repo.RepositoryLoader
import mu.KotlinLogging

class PicoOperator(
    private val appConfig: ApplicationConfig
) {
    fun run(config: OperatorRunConfig) {
        val imageExtractor = ImageExtractor(appConfig.generatedConfigsLocation)
        val rabbitMQManager =
            RabbitMQManager(appConfig.rabbitMQManagement, appConfig.defaultSchemaConfigs, appConfig.schemaName)

        val repositoryLoader = RepositoryLoader(appConfig.repoLocation, appConfig.schemaName)
        val infraMgrConfig: InfraMgrConfigResource = repositoryLoader.loadInfraMgrConfigResource()
        val boxResources: Map<String, BoxResource> = repositoryLoader.loadBoxResources()

        when (val mode = config.mode) {
            "full" -> {
                ConfigHandler.clearOldConfigs(appConfig.generatedConfigsLocation)
                ConfigHandler.copyDefaultConfigs(appConfig.defaultSchemaConfigs, appConfig.generatedConfigsLocation)
                rabbitMQManager.creteInitialSetup()

                val queuesProcessor = QueuesProcessor(rabbitMQManager).also {
                    it.createStoreQueues()
                }
                boxResources.values.forEach {
                    ConfigProcessor(infraMgrConfig, it, config.isOldFormat, appConfig, repositoryLoader).process()
                    imageExtractor.process(it)
                    queuesProcessor.process(it)
                }
                imageExtractor.saveImagesToFile()
                queuesProcessor.removeUnusedQueues()
                rabbitMQManager.closeChannel()
            }
            "queues" -> {
                rabbitMQManager.creteInitialSetup()
                val queuesProcessor = QueuesProcessor(rabbitMQManager).also {
                    it.createStoreQueues()
                }
                boxResources.values.forEach {
                    queuesProcessor.process(it)
                }
                queuesProcessor.removeUnusedQueues()
                rabbitMQManager.closeChannel()
            }
            "configs" -> {
                ConfigHandler.clearOldConfigs(appConfig.generatedConfigsLocation)
                ConfigHandler.copyDefaultConfigs(appConfig.defaultSchemaConfigs, appConfig.generatedConfigsLocation)
                boxResources.values.forEach {
                    ConfigProcessor(infraMgrConfig, it, config.isOldFormat, appConfig, repositoryLoader).process()
                    imageExtractor.process(it)
                }
                imageExtractor.saveImagesToFile()
            }
            else -> {
                logger.error("Mode: {} is not supported", mode)
            }
        }
    }

    private val logger = KotlinLogging.logger {}
}
