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

package com.exactpro.th2.pico.operator.repo

import com.exactpro.th2.pico.operator.config.ConfigLoader
import com.exactpro.th2.pico.operator.schemaName
import com.exactpro.th2.pico.operator.util.Mapper.YAML_MAPPER
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import kotlin.collections.HashMap

object RepositoryLoader {
    private const val YML_EXTENSION = ".yml"
    private const val YAML_EXTENSION = ".yaml"

    private val logger = KotlinLogging.logger { }

    private val schemaDir = "${ConfigLoader.config.repoLocation}/$schemaName"

    private inline fun <reified T> loadCustomResourceFile(file: File): T {
        return YAML_MAPPER.readValue(Files.readString(file.toPath()))
    }

    fun loadBoxResources(): Map<String, BoxResource> {
        val firstOccurrences: MutableSet<String> = HashSet()
        val resources: MutableMap<String, BoxResource> = HashMap()
        for (t in ResourceType.values()) {
            if (!t.isComponent) {
                continue
            }
            resources.putAll(loadKind(t, firstOccurrences))
        }
        return resources
    }

    fun loadInfraMgrConfigResource(): InfraMgrConfigResource {
        val map: Map<String, InfraMgrConfigResource> = loadKind(ResourceType.InfraMgrConfig, mutableSetOf())
        return when (map.size) {
            0 -> InfraMgrConfigResource()
            1 -> map.values.single()
            else -> error(
                "Infra schema should contain only one 'InfraMgrConfig' " +
                        "with '${ResourceType.InfraMgrConfig.kind}' kind, found: $map"
            )
        }
    }

    fun loadDictionaries(): Map<String, DictionaryResource> {
        return loadKind(ResourceType.Th2Dictionary, HashSet())
    }

    @Suppress("CyclomaticComplexMethod")
    private inline fun <reified T : CustomResource>loadKind(
        kind: ResourceType,
        firstOccurrences: MutableSet<String>
    ): Map<String, T> {
        val resources: MutableMap<String, T> = HashMap()
        val dir = File(schemaDir + "/" + kind.path)
        if (dir.exists()) {
            val files = dir.listFiles() ?: return resources
            for (f in files) {
                if (f.isFile && (f.absolutePath.endsWith(YML_EXTENSION) || f.absolutePath.endsWith(YAML_EXTENSION))) {
                    try {
                        val resource = loadCustomResourceFile<T>(f)
                        val meta = resource.metadata
                        if (f.nameWithoutExtension != meta.name) {
                            logger.warn(
                                "skipping \"{}\" | resource name does not match filename",
                                f.absolutePath
                            )
                            continue
                        }
                        if (!ResourceType.knownKinds().contains(resource.kind)) {
                            logger.error(
                                "skipping \"{}\" | Unknown kind \"{}\". Known values are: \"{}\"",
                                f.absolutePath,
                                resource.kind,
                                ResourceType.knownKinds()
                            )
                            continue
                        }
                        if (!ResourceType.forKind(resource.kind)?.path.equals(kind.path)) {
                            logger.error(
                                "skipping \"{}\" | resource is located in wrong directory. kind" +
                                    ": {}, dir:" + " {}",
                                f.absolutePath,
                                resource.kind,
                                kind.path
                            )
                            continue
                        }

                        // some directories might contain multiple resource kinds
                        // skip other kinds as they will be checked on their iteration
                        if (resource.kind != kind.kind) {
                            continue
                        }

                        val name: String = meta.name
                        if (firstOccurrences.contains(name)) {
                            // we already encountered resource with same name
                            // ignore both of them
                            logger.error(
                                "Detected two resources with the same name: \"{}\" skipping both of them.",
                                name,
                            )
                            resources.remove(name)
                            continue
                        }
                        resources[name] = resource
                        firstOccurrences.add(name)
                    } catch (e: Exception) {
                        logger.error("skipping \"{}\" | exception loading resource", f.absolutePath, e)
                    }
                }
            }
        }
        return resources
    }
}
