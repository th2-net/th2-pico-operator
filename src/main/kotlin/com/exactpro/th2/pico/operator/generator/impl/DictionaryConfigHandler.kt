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
import com.exactpro.th2.pico.operator.repo.RepositoryContext
import mu.KotlinLogging
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPOutputStream
import kotlin.collections.HashSet

class DictionaryConfigHandler(resource: BoxResource) : ConfigHandler(resource) {
    private val logger = KotlinLogging.logger { }

    private val dictionariesDir = "${this.resource.metadata.name}/dictionaries"
    private val dictionaries: MutableSet<String> = HashSet()

    override fun handle() {
        val customConfig = resource.spec.customConfig ?: return
        collectDictionaries(customConfig)
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
            saveConfigFle("$dictionariesDir/$dictionaryName", compressedData)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun collectDictionaries(customConfig: MutableMap<String, Any>) {
        for ((key, value) in customConfig) {
            if (value is String) {
                val matchGroups = DICTIONARY_LINK_REGEXP.find(value)?.groupValues ?: continue
                val dictionaryName = matchGroups[1]
                customConfig[key] = value.replace(matchGroups[0], dictionaryName)
                dictionaries.add(dictionaryName)
            } else if (value is Map<*, *>) {
                collectDictionaries(value as MutableMap<String, Any>)
            }
        }
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

    companion object {
        private val DICTIONARY_LINK_REGEXP = "\\$\\{dictionary_link:([A-Za-z-\\d]*)}".toRegex()
    }
}
