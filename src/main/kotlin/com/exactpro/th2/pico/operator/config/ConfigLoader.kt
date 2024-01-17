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

package com.exactpro.th2.pico.operator.config

import com.exactpro.th2.pico.operator.util.Mapper.YAML_MAPPER
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.lookup.StringLookupFactory
import java.nio.file.Path
import kotlin.io.path.inputStream

object ConfigLoader {

    private val logger = KotlinLogging.logger { }

    fun loadConfiguration(configPath: Path): ApplicationConfig {
        try {
            configPath.inputStream().use { inputStream ->
                val stringSubstitute = StringSubstitutor(StringLookupFactory.INSTANCE.environmentVariableStringLookup())
                val content = stringSubstitute.replace(String(inputStream.readAllBytes()))
                return YAML_MAPPER.readValue(content)
            }
        } catch (e: UnrecognizedPropertyException) {
            logger.error(
                "Bad configuration: unknown property(\"{}\") specified in configuration file",
                e.propertyName
            )
            throw e
        } catch (e: JsonParseException) {
            logger.error("Bad configuration: exception while parsing configuration file")
            throw e
        }
    }
}
