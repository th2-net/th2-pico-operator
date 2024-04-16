/*
 * Copyright 2023-2024 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.th2.pico.operator.config.fields.DefaultSchemaConfigs
import com.exactpro.th2.pico.operator.config.fields.PrometheusPortsConfig
import com.exactpro.th2.pico.operator.generator.ConfigHandler
import com.exactpro.th2.pico.operator.repo.BoxResource
import java.nio.file.Path

class PrometheusConfigHandler(
    private val resource: BoxResource,
    private val prometheusPortsConfig: PrometheusPortsConfig,
    generatedConfigsLocation: Path,
    schemaConfigs: DefaultSchemaConfigs,
) : ConfigHandler(
    generatedConfigsLocation,
    schemaConfigs,
) {
    private val fileName = "${this.resource.metadata.name}/prometheus.json"

    override fun handle() {
        saveConfigFile(
            fileName,
            PrometheusConfig(
                prometheusPortsConfig.enabled && (resource.spec.prometheus?.enabled ?: true),
                prometheusPortsConfig.acquirePort()
            )
        )
    }
}

data class PrometheusConfig(
    val enabled: Boolean,
    val port: Int
)
