/*
 * Copyright 2023 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.th2.pico.operator.config.fields.DefaultConfigNames
import com.exactpro.th2.pico.operator.generator.ConfigHandler
import com.exactpro.th2.pico.operator.repo.BoxResource

class LogConfigHandler(private val resource: BoxResource) : ConfigHandler() {
    private val log4j2FileName = "${this.resource.metadata.name}/log4j2.properties"
    private val log4pyFileName = "${this.resource.metadata.name}/log4py.conf"
    private val zeroLogFileName = "${this.resource.metadata.name}/zerolog.properties"

    override fun handle() {
        copyDefaultConfig(DefaultConfigNames.log4j2Config, log4j2FileName)
        copyDefaultConfig(DefaultConfigNames.log4pyConfig, log4pyFileName)
        copyDefaultConfig(DefaultConfigNames.zeroLogConfig, zeroLogFileName)
    }
}
