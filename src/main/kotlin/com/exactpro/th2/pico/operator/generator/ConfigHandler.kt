/*
 * Copyright 2022-2023 Exactpro (Exactpro Systems Limited)
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
import com.exactpro.th2.pico.operator.configDir
import com.exactpro.th2.pico.operator.util.Mapper.JSON_MAPPER
import com.exactpro.th2.pico.operator.util.Mapper.YAML_MAPPER
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

abstract class ConfigHandler {

    abstract fun handle()

    private fun pathToDefaultConfig(configName: DefaultConfigNames): Path =
        Path("$defaultConfigsLocation/${defaultConfigNames[configName]}")

    private fun pathToTargetConfig(fileName: String): Path {
        val file = File("$configDir/$fileName")
        file.parentFile.mkdirs()
        return file.toPath()
    }

    protected fun copyDefaultConfig(configName: DefaultConfigNames, fileName: String) {
        val source = pathToDefaultConfig(configName)
        if (!Files.exists(source)) {
            return
        }
        val target = pathToTargetConfig(fileName)
        Files.copy(source, target)
    }

    protected fun saveConfigFile(fileName: String, configContent: Any) {
        val file = pathToTargetConfig(fileName)
        val configContentStr = JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(configContent)
        Files.writeString(file, configContentStr)
    }

    protected fun loadDefaultConfig(configName: DefaultConfigNames): Map<String, Any> {
        val path = pathToDefaultConfig(configName)
        return YAML_MAPPER.readValue(Files.readString(path))
    }

    companion object {
        private val defaultConfigsLocation = ConfigLoader.config.defaultSchemaConfigs.location
        private val defaultConfigNames = ConfigLoader.config.defaultSchemaConfigs.configNames

        fun clearOldConfigs() {
            File(configDir).deleteRecursively()
        }

        fun copyDefaultConfigs() {
            val directories = File(configDir).listFiles() ?: return
            directories.forEach { dir ->
                if (dir.isDirectory) {
                    ConfigLoader.config.defaultSchemaConfigs.configNames.values.forEach { file ->
                        if (!File("$dir/$file").exists()) {
                            Files.copy(Path("$defaultConfigsLocation/$file"), Path("$dir/$file"))
                        }
                    }
                }
            }
            copyLogging()
        }

        private fun copyLogging() {
            val files = File("${ConfigLoader.config.defaultSchemaConfigs.location}/logging").listFiles() ?: return
            files.forEach {
                Files.copy(it.toPath(), Path("$configDir/${it.name}"))
            }
        }
    }
}
