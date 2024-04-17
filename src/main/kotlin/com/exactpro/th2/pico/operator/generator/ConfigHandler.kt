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
import mu.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.notExists
import kotlin.io.path.outputStream
import kotlin.io.path.writeText

abstract class ConfigHandler(
    protected val generatedConfigsLocation: Path,
    private val schemaConfigs: DefaultSchemaConfigs,
) {
    abstract fun handle()

    private fun pathToDefaultConfig(configName: DefaultConfigNames): Path {
        return requireNotNull(schemaConfigs.configNames[configName]) {
            "th2 config name for '$configName; isn't declared in config"
        }.run(schemaConfigs.location::resolve)
    }

    private fun pathToTargetConfig(fileName: String): Path {
        return generatedConfigsLocation.resolve(fileName).also {
            it.parent.createDirectories()
        }
    }

    protected fun copyDefaultConfig(configName: DefaultConfigNames, fileName: String) {
        val source = pathToDefaultConfig(configName)
        if (!source.exists()) {
            return
        }
        val target = pathToTargetConfig(fileName)
        source.copyTo(target, overwrite = true)
        LOGGER.info { "Updated '$target' from '$source' file" }
    }

    protected fun saveConfigFile(fileName: String, configContent: String) {
        pathToTargetConfig(fileName).writeText(configContent)
    }

    protected fun saveConfigFile(fileName: String, configContent: Any) {
        pathToTargetConfig(fileName).outputStream().use {
            JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValue(it, configContent)
        }
    }

    protected fun loadDefaultConfig(configName: DefaultConfigNames): Map<String, Any> {
        return pathToDefaultConfig(configName).inputStream().use {
            YAML_MAPPER.readValue(it)
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }

        @OptIn(ExperimentalPathApi::class)
        fun clearOldConfigs(generatedConfigsLocation: Path) {
            generatedConfigsLocation.deleteRecursively()
        }

        fun copyDefaultConfigs(schemaConfigs: DefaultSchemaConfigs, generatedConfigsLocation: Path) {
            generatedConfigsLocation.listDirectoryEntries().asSequence()
                .filter(Path::isDirectory)
                .forEach { dir ->
                    schemaConfigs.configNames.values.forEach { file ->
                        val target = dir.resolve(file)
                        if (target.notExists()) {
                            val source = schemaConfigs.location.resolve(file)
                            source.copyTo(target, overwrite = false)
                            LOGGER.debug { "Updated '$target' from '$source' file" }
                        }
                    }
                }
        }
    }
}
