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

import com.exactpro.th2.model.latest.box.Spec
import com.exactpro.th2.pico.operator.configDir
import com.exactpro.th2.pico.operator.generator.ConfigHandler
import com.exactpro.th2.pico.operator.repo.BoxResource
import com.exactpro.th2.pico.operator.repo.RepositoryContext
import com.exactpro.th2.pico.operator.util.Mapper.YAML_MAPPER
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.lookup.StringLookup
import org.apache.commons.text.lookup.StringLookupFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.zip.GZIPOutputStream

class DictionaryConfigHandler(private val resource: BoxResource, private val isOldFormat: Boolean) : ConfigHandler() {
    private val logger = KotlinLogging.logger { }

    private val dictionariesDir = "${this.resource.metadata.name}/dictionaries"
    private val dictionaries: MutableSet<String> = HashSet()

    override fun handle() {
        val customConfig = resource.spec.customConfig ?: return
        collectDictionaries(resource.spec)
        for (dictionaryName in dictionaries) {
            val dictionary = RepositoryContext.dictionaries[dictionaryName]
            if (dictionary == null) {
                logger.error(
                    "Dictionary: {} requested by resource: {} is no present in repository",
                    dictionaryName,
                    resource.metadata.name
                )
                continue
            }
            val compressedData = compressData(dictionary.spec.data)
            saveDictionary(dictionaryName, compressedData)
        }
        if (isOldFormat) {
            customConfig.remove("dictionaries")
        }
    }

    private fun saveDictionary(fileName: String, data: String) {
        val file = File("$configDir/$dictionariesDir/$fileName")
        file.parentFile.mkdirs()
        Files.writeString(file.toPath(), data)
    }

    private fun collectDictionaries(spec: Spec) {
        val stringSubstitutor = StringSubstitutor(
            StringLookupFactory.INSTANCE.interpolatorStringLookup(
                mapOf(
                    DICTIONARY_LINK_PREFIX to CustomLookupForDictionaries(dictionaries)
                ),
                null,
                false
            )
        )
        val customConfigStr: String = YAML_MAPPER.writeValueAsString(spec.customConfig)
        spec.customConfig = YAML_MAPPER.readValue(stringSubstitutor.replace(customConfigStr))
    }

    private fun compressData(value: String): String {
        val baos = ByteArrayOutputStream()
        val gzipos = GZIPOutputStream(baos)
        baos.use {
            gzipos.use {
                gzipos.write(value.toByteArray())
                gzipos.finish()
                return String(Base64.getEncoder().encode(baos.toByteArray()))
            }
        }
    }

    class CustomLookupForDictionaries(private val collector: MutableSet<String>) : StringLookup {
        override fun lookup(key: String): String {
            collector.add(key)
            return key
        }
    }

    companion object {
        private const val DICTIONARY_LINK_PREFIX = "dictionary_link"
    }
}
