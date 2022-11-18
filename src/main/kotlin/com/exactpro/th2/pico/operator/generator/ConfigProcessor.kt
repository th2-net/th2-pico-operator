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

package com.exactpro.th2.pico.operator.generator

import com.exactpro.th2.pico.operator.generator.impl.BoxConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.CradleManagerConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.CustomConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.DictionaryConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.GrpcConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.GrpcRouterConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.LoggingConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.MqConfigHandler
import com.exactpro.th2.pico.operator.generator.impl.MqRouterConfigHandler
import com.exactpro.th2.pico.operator.repo.BoxResource

class ConfigProcessor(resource: BoxResource) {
    private val configHandlers: List<ConfigHandler> = ArrayList<ConfigHandler>().apply {
        add(MqConfigHandler(resource))
        add(BoxConfigHandler(resource))
        add(GrpcConfigHandler(resource))
        add(DictionaryConfigHandler(resource))
        // DictionaryConfigHandler Has to come before CustomConfigHandler as it modifies values in custom config
        add(CustomConfigHandler(resource))
        add(LoggingConfigHandler(resource))
        add(MqRouterConfigHandler(resource))
        add(GrpcRouterConfigHandler(resource))
        add(CradleManagerConfigHandler(resource))
    }

    fun process() {
        configHandlers.forEach(ConfigHandler::handle)
    }
}
