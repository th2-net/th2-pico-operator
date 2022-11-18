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

import com.exactpro.th2.pico.operator.config.ConfigLoader
import com.exactpro.th2.pico.operator.config.fields.DefaultConfigNames
import com.exactpro.th2.pico.operator.repo.BoxResource
import com.exactpro.th2.pico.operator.util.Mapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

abstract class ConfigHandler(val resource: BoxResource) {

    abstract fun handle()

    protected fun saveConfigFle(fileName: String, configContent: Any) {
        val file = File("$configDir/$fileName")
        file.parentFile.mkdirs()
        val configContentStr = Mapper.JSON_MAPPER.writeValueAsString(configContent)
        Files.writeString(file.toPath(), configContentStr)
    }

    fun loadDefaultConfig(configName: DefaultConfigNames): Map<String, Any> {
        val path = Path("$defaultConfigsLocation/${defaultConfigNames[configName]}")
        return Mapper.YAML_MAPPER.readValue(Files.readString(path))
    }

    companion object {
        val schemaName = ConfigLoader.config.schemaName
        private val configDir = "${ConfigLoader.config.repoLocation}/$schemaName/generatedConfigs"
        private val defaultConfigsLocation = ConfigLoader.config.defaultSchemaConfigs.location
        private val defaultConfigNames = ConfigLoader.config.defaultSchemaConfigs.configNames

        fun clearOldConfigs() {
            File(configDir).deleteRecursively()
        }

        fun copyDefaultConfigs() {
            val destinationPath = "$configDir/default/"
            File(destinationPath).mkdirs()
            ConfigLoader.config.defaultSchemaConfigs.configNames.values.forEach {
                Files.copy(Path("$defaultConfigsLocation/$it"), Path("$destinationPath$it"))
            }
        }
    }
}
