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

import com.exactpro.th2.pico.operator.config.fields.DefaultConfigNames
import com.exactpro.th2.pico.operator.generator.ConfigHandler
import com.exactpro.th2.pico.operator.repo.BoxResource

class BoxConfigHandler(resource: BoxResource) : ConfigHandler(resource) {
    private val fileName = "${this.resource.metadata.name}/box.json"
    private val defaultBookKey = "defaultBook"

    override fun handle() {
        val default = loadDefaultConfig(DefaultConfigNames.bookConfig)
        val bookName = resource.spec.bookName ?: default[defaultBookKey] as String
        saveConfigFle(
            fileName,
            mapOf(
                "boxName" to resource.metadata.name,
                "bookName" to bookName,
                "image" to "${resource.spec.imageName}:${resource.spec.imageVersion}"
            )
        )
    }
}
