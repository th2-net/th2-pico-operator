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

package com.exactpro.th2.pico.operator.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.KotlinModule

object Mapper {
    val YAML_MAPPER: ObjectMapper = ObjectMapper(
        YAMLFactory()
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
    ).registerModule(KotlinModule.Builder().build())
        .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)

    val JSON_MAPPER: ObjectMapper = ObjectMapper(JsonFactory())
        .registerModule(KotlinModule.Builder().build())
        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)

    fun mergeConfigs(defaults: Map<String, Any>, newData: Any): Map<String, Any> {
        val updater = JSON_MAPPER.readerForUpdating(defaults)
        val newDataStr = JSON_MAPPER.writeValueAsString(newData)
        return updater.readValue(newDataStr)
    }
}
