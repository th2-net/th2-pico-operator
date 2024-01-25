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

package com.exactpro.th2.pico.operator.repo

import com.exactpro.th2.pico.operator.util.Mapper.YAML_MAPPER
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import kotlin.collections.HashMap
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

class RepositoryLoader(
    repoLocation: Path,
    schemaName: String,
) {

    private val logger = KotlinLogging.logger { }

    private val schemaDir = repoLocation.resolve(schemaName)

    private inline fun <reified T> loadCustomResourceFile(path: Path): T {
        return YAML_MAPPER.readValue(Files.readString(path))
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
        val dir = schemaDir.resolve(kind.path)
        if (dir.isDirectory()) {
            dir.listDirectoryEntries().forEach { f ->
                if (f.isRegularFile() && (SUPPORTED_EXTENSIONS.contains(f.extension))) {
                    try {
                        val resource = loadCustomResourceFile<T>(f)
                        val meta = resource.metadata
                        if (f.nameWithoutExtension != meta.name) {
                            logger.warn {
                                "skipping '${f.absolutePathString()}' | resource name does not match filename."
                            }
                            return@forEach
                        }
                        if (!ResourceType.knownKinds().contains(resource.kind)) {
                            logger.error {
                                "skipping '${f.absolutePathString()}' | Unknown kind '${resource.kind}'. " +
                                    "Known values are: '${ResourceType.knownKinds()}'"
                            }
                            return@forEach
                        }
                        if (!ResourceType.forKind(resource.kind)?.path.equals(kind.path)) {
                            logger.error {
                                "skipping '${f.absolutePathString()}' | resource is located in wrong directory. " +
                                    "kind: '${resource.kind}', dir: '${kind.path}'"
                            }
                            return@forEach
                        }

                        // some directories might contain multiple resource kinds
                        // skip other kinds as they will be checked on their iteration
                        if (resource.kind != kind.kind) {
                            return@forEach
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
                            return@forEach
                        }
                        resources[name] = resource
                        firstOccurrences.add(name)
                    } catch (e: Exception) {
                        logger.error(e) {
                            "skipping '${f.absolutePathString()}' | exception loading resource"
                        }
                    }
                }
            }
        }
        return resources
    }

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("yml", "yaml", "json")
    }
}
