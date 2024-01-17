/*
 * Copyright 2024 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.th2.pico.operator.config.fields.DefaultConfigNames
import com.exactpro.th2.pico.operator.config.fields.DefaultSchemaConfigs
import com.exactpro.th2.pico.operator.config.fields.GrpcConfig
import com.exactpro.th2.pico.operator.config.fields.GrpcServerPortsConfig
import com.exactpro.th2.pico.operator.config.fields.PrometheusPortsConfig
import com.exactpro.th2.pico.operator.config.fields.RabbitMQManagementConfig
import com.exactpro.th2.pico.operator.config.fields.RabbitMQNamespacePermissions
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals

class ConfigLoaderTest {

    @Test
    fun `load configuration test`() {
        val source = """
            repoLocation: th2-infra-schema
            generatedConfigsLocation: workspace/configs
            schemaName: schema
            rabbitMQManagement:
              host: localhost
              managementPort: 15672
              applicationPort: 5672
              vhostName: th2
              exchangeName: global-notification
              username: "${"$"}{RABBITMQ_USER}"
              password: "${"$"}{RABBITMQ_PASS}"
              persistence: true
              schemaPermissions:
                configure: ""
                read: ".*"
                write: ".*"
            grpc:
              serverPorts:
                start: 8091
                end: 8189
            prometheus:
               enabled: false
               start: 9000
               end: 9090
            defaultSchemaConfigs:
              location: cfg/defaultConfigs
              configNames:
                cradle: cradle.json
                cradleManager: cradle_manager.json
                grpcRouter: grpc_router.json
                mqRouter: mq_router.json
                rabbitMQ: rabbitMQ.json
                log4j2Config: log4j2.properties
                log4pyConfig: log4py.conf
                zeroLogConfig: zerolog.properties 
        """.trimIndent()

        val expected = ApplicationConfig(
            Path.of("th2-infra-schema"),
            Path.of("workspace/configs"),
            "schema",
            RabbitMQManagementConfig(
                "localhost",
                15672,
                5672,
                "th2",
                "global-notification",
                "\${RABBITMQ_USER}",
                "\${RABBITMQ_PASS}",
                true,
                RabbitMQNamespacePermissions(
                    "",
                    ".*",
                    ".*",
                )

            ),
            GrpcConfig(
                GrpcServerPortsConfig(
                    8091,
                    8189,
                ),
            ),
            PrometheusPortsConfig(
                false,
                9000,
                9090,
            ),
            DefaultSchemaConfigs(
                Path.of("cfg/defaultConfigs"),
                mapOf(
                    DefaultConfigNames.cradle to "cradle.json",
                    DefaultConfigNames.cradleManager to "cradle_manager.json",
                    DefaultConfigNames.grpcRouter to "grpc_router.json",
                    DefaultConfigNames.mqRouter to "mq_router.json",
                    DefaultConfigNames.rabbitMQ to "rabbitMQ.json",
                    DefaultConfigNames.log4j2Config to "log4j2.properties",
                    DefaultConfigNames.log4pyConfig to "log4py.conf",
                    DefaultConfigNames.zeroLogConfig to "zerolog.properties",
                )
            ),
        )

        assertEquals(expected, ConfigLoader.loadConfiguration(source.byteInputStream()))
    }
}
