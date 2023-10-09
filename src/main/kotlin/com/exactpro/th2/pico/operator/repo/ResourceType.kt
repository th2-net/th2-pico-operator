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

enum class ResourceType(val kind: String, val path: String, val isComponent: Boolean) {
    Th2Dictionary("Th2Dictionary", "dictionaries", false),
    InfraMgrConfig("SettingsFile", "", false),

    Th2CoreBox("Th2CoreBox", "core", true),
    Th2Mstore("Th2Mstore", "core", true),
    Th2Estore("Th2Estore", "core", true),
    Th2Box("Th2Box", "boxes", true),
    Th2Job("Th2Job", "jobs", true);

    companion object {
        private val kinds: MutableMap<String, ResourceType> = HashMap()

        fun forKind(kind: String): ResourceType? {
            return kinds[kind]
        }

        fun knownKinds(): Set<String> {
            return kinds.keys
        }

        init {
            for (t in values()) {
                kinds[t.kind] = t
            }
        }
    }
}
