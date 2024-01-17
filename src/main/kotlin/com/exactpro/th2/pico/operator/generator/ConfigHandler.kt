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

import com.exactpro.th2.pico.operator.config.fields.DefaultConfigNames
import com.exactpro.th2.pico.operator.config.fields.DefaultSchemaConfigs
import com.exactpro.th2.pico.operator.util.Mapper.JSON_MAPPER
import com.exactpro.th2.pico.operator.util.Mapper.YAML_MAPPER
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

abstract class ConfigHandler(
    protected val generatedConfigsLocation: String,
    private val schemaConfigs: DefaultSchemaConfigs,
) {
    abstract fun handle()

    private fun pathToDefaultConfig(configName: DefaultConfigNames): Path =
        Path("${schemaConfigs.location}/${schemaConfigs.configNames[configName]}")

    private fun pathToTargetConfig(fileName: String): Path {
        val file = File("$generatedConfigsLocation/$fileName")
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
        fun clearOldConfigs(generatedConfigsLocation: String) {
            File(generatedConfigsLocation).deleteRecursively()
        }

        fun copyDefaultConfigs(schemaConfigs: DefaultSchemaConfigs, generatedConfigsLocation: String) {
            val directories = File(generatedConfigsLocation).listFiles() ?: return
            directories.forEach { dir ->
                if (dir.isDirectory) {
                    schemaConfigs.configNames.values.forEach { file ->
                        if (!File("$dir/$file").exists()) {
                            Files.copy(Path("${schemaConfigs.location}/$file"), Path("$dir/$file"))
                        }
                    }
                }
            }
            copyLogging(schemaConfigs.location, generatedConfigsLocation)
        }

        private fun copyLogging(schemaConfigsLocation: String, generatedConfigsLocation: String) {
            val files = File("$schemaConfigsLocation/logging").listFiles() ?: return
            files.forEach {
                Files.copy(it.toPath(), Path("$generatedConfigsLocation/${it.name}"))
            }
        }
    }
}
