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

package com.exactpro.th2.pico.operator.generator

import com.exactpro.th2.pico.operator.config.ApplicationConfig
import com.exactpro.th2.pico.operator.generator.impl.BoxAdditionalConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.BoxConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.CradleManagerConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.CustomConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.DictionaryConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.GrpcConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.GrpcRouterConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.LogConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.MqConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.MqRouterConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.PrometheusConfigHandler
import com.exactpro.th2.pico.operator.grpc.factory.GrpcRouterConfigFactory
import com.exactpro.th2.pico.operator.repo.BoxResource
import com.exactpro.th2.pico.operator.repo.InfraMgrConfigResource
import com.exactpro.th2.pico.operator.repo.RepositoryLoader

class ConfigProcessor(
    infraMgrConfig: InfraMgrConfigResource,
    resource: BoxResource,
    isOldFormat: Boolean,
    appConfig: ApplicationConfig,
    repositoryLoader: RepositoryLoader,
) {
    private val configHandlers: List<ConfigHandler> = ArrayList<ConfigHandler>().apply {
        val grpcRouterConfigFactory = GrpcRouterConfigFactory(appConfig.grpc.serverPorts)

        add(
            MqConfigHandler(
                resource,
                appConfig.rabbitMQManagement.exchangeName,
                appConfig.schemaName,
                appConfig.generatedConfigsLocation,
                appConfig.defaultSchemaConfigs
            )
        )
        add(
            BoxConfigHandler(
                infraMgrConfig,
                resource,
                appConfig.generatedConfigsLocation,
                appConfig.defaultSchemaConfigs
            )
        )
        add(
            BoxAdditionalConfigHandler(
                infraMgrConfig,
                resource,
                appConfig.generatedConfigsLocation,
                appConfig.defaultSchemaConfigs
            )
        )
        add(
            GrpcConfigHandler(
                resource,
                grpcRouterConfigFactory,
                appConfig.generatedConfigsLocation,
                appConfig.defaultSchemaConfigs
            )
        )
        add(
            DictionaryConfigHandler(
                resource,
                isOldFormat,
                repositoryLoader.loadDictionaries(),
                appConfig.generatedConfigsLocation,
                appConfig.defaultSchemaConfigs
            )
        )
        // DictionaryConfigHandler Has to come before CustomConfigHandler as it modifies values in custom config
        add(CustomConfigHandler(resource, appConfig.generatedConfigsLocation, appConfig.defaultSchemaConfigs))
        add(MqRouterConfigHandler(resource, appConfig.generatedConfigsLocation, appConfig.defaultSchemaConfigs))
        add(LogConfigHandler(resource, appConfig.generatedConfigsLocation, appConfig.defaultSchemaConfigs))
        add(GrpcRouterConfigHandler(resource, appConfig.generatedConfigsLocation, appConfig.defaultSchemaConfigs))
        add(CradleManagerConfigHandler(resource, appConfig.generatedConfigsLocation, appConfig.defaultSchemaConfigs))
        add(
            PrometheusConfigHandler(
                resource,
                appConfig.prometheus,
                appConfig.generatedConfigsLocation,
                appConfig.defaultSchemaConfigs
            )
        )
    }

    fun process() {
        configHandlers.forEach(ConfigHandler::handle)
    }
}
