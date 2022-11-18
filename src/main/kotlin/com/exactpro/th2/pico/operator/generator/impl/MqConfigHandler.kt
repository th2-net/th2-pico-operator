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

package com.exactpro.th2.pico.operator.generator.impl

import com.exactpro.th2.pico.operator.generator.ConfigHandler
import com.exactpro.th2.pico.operator.mq.factory.MqRouterConfigFactory
import com.exactpro.th2.pico.operator.mq.factory.MqRouterConfigFactoryBox
import com.exactpro.th2.pico.operator.mq.factory.MqRouterConfigFactoryEstore
import com.exactpro.th2.pico.operator.mq.factory.MqRouterConfigFactoryMstore
import com.exactpro.th2.pico.operator.repo.BoxResource
import com.exactpro.th2.pico.operator.repo.ResourceType

class MqConfigHandler(resource: BoxResource) : ConfigHandler(resource) {

    private val fileName = "${this.resource.metadata.name}/mq.json"

    override fun handle() {
        val mqRouterConfigFactory = mqRouterConfigFactories[ResourceType.forKind(resource.kind)]!!
        val config = mqRouterConfigFactory.createConfig(resource)
        saveConfigFle(fileName, config)
    }

    companion object {
        private val mqRouterConfigFactories: Map<ResourceType, MqRouterConfigFactory> = mapOf(
            ResourceType.Th2Estore to MqRouterConfigFactoryEstore(schemaName),
            ResourceType.Th2Mstore to MqRouterConfigFactoryMstore(schemaName),
            ResourceType.Th2Box to MqRouterConfigFactoryBox(schemaName),
            ResourceType.Th2CoreBox to MqRouterConfigFactoryBox(schemaName)
        )
    }
}
