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
import com.exactpro.th2.pico.operator.repo.BoxResource

class LoggingConfigHandler(private val resource: BoxResource) : ConfigHandler() {
    private val fileNames = listOf(
        "${this.resource.metadata.name}/log4j2.properties",
        "${this.resource.metadata.name}/log4j.properties",
        "${this.resource.metadata.name}/log4py.conf",
        "${this.resource.metadata.name}/log4cxx.properties",
    )

    override fun handle() {
        val config = resource.spec.loggingConfig ?: return
        fileNames.forEach {
            saveConfigFle(it, config)
        }
    }
}
