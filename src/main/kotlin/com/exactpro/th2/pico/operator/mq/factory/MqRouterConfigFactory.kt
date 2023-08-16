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

package com.exactpro.th2.pico.operator.mq.factory

import com.exactpro.th2.pico.operator.EVENT_STORAGE_PIN_ALIAS
import com.exactpro.th2.pico.operator.mq.MessageRouterConfiguration
import com.exactpro.th2.pico.operator.mq.PinAttribute
import com.exactpro.th2.pico.operator.mq.QueueConfiguration
import com.exactpro.th2.pico.operator.mq.queue.LinkDescription
import com.exactpro.th2.pico.operator.mq.queue.Queue
import com.exactpro.th2.pico.operator.mq.queue.RoutingKey
import com.exactpro.th2.pico.operator.repo.BoxResource

abstract class MqRouterConfigFactory(val schemaName: String) {
    abstract fun createConfig(resource: BoxResource): MessageRouterConfiguration

    fun generatePublishToEstorePin(schemaName: String, boxName: String) = QueueConfiguration(
        LinkDescription(
            Queue.EMPTY,
            RoutingKey(schemaName, boxName, EVENT_STORAGE_PIN_ALIAS),
            schemaName
        ),
        listOf(PinAttribute.publish.name, PinAttribute.event.name),
        emptyList()
    )

    fun generateDeclaredQueues(
        resource: BoxResource
    ): MutableMap<String, QueueConfiguration> {
        val queues: MutableMap<String, QueueConfiguration> = HashMap()
        val boxName = resource.metadata.name

        // add configurations for the rest of the pins
        for ((pinName, attributes, filters) in resource.spec.pins?.mq?.publishers ?: emptyList()) {
            queues[pinName] = QueueConfiguration(
                LinkDescription(
                    Queue.EMPTY,
                    RoutingKey(schemaName, boxName, pinName),
                    schemaName
                ),
                attributes?.let {
                    ArrayList(it).apply {
                        add(PinAttribute.publish.name)
                    }
                },
                filters
            )
        }

        for ((pinName, attributes, filters) in resource.spec.pins?.mq?.subscribers ?: emptyList()) {
            queues[pinName] = QueueConfiguration(
                LinkDescription(
                    Queue(schemaName, boxName, pinName),
                    RoutingKey.EMPTY,
                    schemaName
                ),
                attributes?.let {
                    ArrayList(it).apply {
                        add(PinAttribute.subscribe.name)
                    }
                },
                filters
            )
        }
        return queues
    }
}
