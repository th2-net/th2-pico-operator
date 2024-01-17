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

package com.exactpro.th2.pico.operator.grpc.factory

import com.exactpro.th2.model.latest.box.pins.GrpcClient
import com.exactpro.th2.pico.operator.config.fields.GrpcServerPortsConfig
import com.exactpro.th2.pico.operator.grpc.GrpcEndpointConfiguration
import com.exactpro.th2.pico.operator.grpc.GrpcRouterConfiguration
import com.exactpro.th2.pico.operator.grpc.GrpcServerConfiguration
import com.exactpro.th2.pico.operator.grpc.GrpcServiceConfiguration
import com.exactpro.th2.pico.operator.grpc.RoutingStrategy
import com.exactpro.th2.pico.operator.repo.BoxResource

class GrpcRouterConfigFactory(
    private val grpcServerPortsConfig: GrpcServerPortsConfig,
) {
    fun createConfig(resource: BoxResource): GrpcRouterConfiguration {
        var server: GrpcServerConfiguration? = null
        val services: MutableMap<String, GrpcServiceConfiguration> = HashMap()
        if (resource.spec.pins?.grpc?.server?.isNotEmpty() == true) {
            server = createServer(getServerPort(resource.metadata.name))
        }
        val clientPins = resource.spec.pins?.grpc?.client ?: return GrpcRouterConfiguration(services, server)

        for (pin in clientPins) {
            for (link in pin.linkTo ?: continue) {
                createService(pin, link.box!!, services)
            }
        }
        return GrpcRouterConfiguration(services, server)
    }

    private fun createService(
        currentPin: GrpcClient,
        linkedBoxName: String,
        services: MutableMap<String, GrpcServiceConfiguration>
    ) {
        val serviceClass = currentPin.serviceClass
        val serviceName = currentPin.name
        var config = services[serviceName]
        val serverPort = getServerPort(linkedBoxName)
        val endpoints = mutableMapOf(
            linkedBoxName + ENDPOINT_ALIAS_SUFFIX to
                GrpcEndpointConfiguration(serverPort, currentPin.attributes)
        )

        if (config == null) {
            val routingStrategy = RoutingStrategy(
                currentPin.strategy,
                HashSet(endpoints.keys)
            )
            config = GrpcServiceConfiguration(routingStrategy, serviceClass, endpoints, currentPin.filters)
            services[serviceName] = config
        } else {
            config.endpoints.putAll(endpoints)
            // TODO grpc filters
//            config.filters!!.addAll(currentPin.filters!!)
            config.strategy.endpoints.addAll(HashSet(endpoints.keys))
        }
    }

    private fun getServerPort(serverName: String): Int {
        val serverPort = serverPortMapping[serverName] ?: grpcServerPortsConfig.acquirePort()
        serverPortMapping[serverName] = serverPort
        return serverPort
    }

    private fun createServer(port: Int): GrpcServerConfiguration {
        return GrpcServerConfiguration(
            DEFAULT_SERVER_WORKERS_COUNT,
            port,
            null,
            null
        )
    }

    companion object {
        val serverPortMapping: MutableMap<String, Int> = HashMap()
        private const val DEFAULT_SERVER_WORKERS_COUNT = 5
        private const val ENDPOINT_ALIAS_SUFFIX = "-endpoint"
    }
}
