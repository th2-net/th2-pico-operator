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
import com.exactpro.th2.pico.operator.MESSAGE_STORAGE_PARSED_PIN_ALIAS
import com.exactpro.th2.pico.operator.MESSAGE_STORAGE_PIN_ALIAS
import com.exactpro.th2.pico.operator.config.ConfigLoader
import com.exactpro.th2.pico.operator.mq.MessageRouterConfiguration
import com.exactpro.th2.pico.operator.mq.PinAttribute
import com.exactpro.th2.pico.operator.mq.QueueConfiguration
import com.exactpro.th2.pico.operator.mq.queue.LinkDescription
import com.exactpro.th2.pico.operator.mq.queue.Queue
import com.exactpro.th2.pico.operator.mq.queue.RoutingKey
import com.exactpro.th2.pico.operator.repo.BoxResource

class MqRouterConfigFactoryMstore(schemaName: String) : MqRouterConfigFactory(schemaName) {

    @Override
    override fun createConfig(resource: BoxResource): MessageRouterConfiguration {
        val queues: MutableMap<String, QueueConfiguration> = generateDeclaredQueues(resource)
        val boxName = resource.metadata.name

        // add event storage pin config for each resource
        queues[EVENT_STORAGE_PIN_ALIAS] = generatePublishToEstorePin(schemaName, boxName)
        queues[MESSAGE_STORAGE_PIN_ALIAS] = QueueConfiguration(
            LinkDescription(
                Queue(schemaName, boxName, MESSAGE_STORAGE_PIN_ALIAS),
                RoutingKey.EMPTY,
                schemaName
            ),
            listOf(PinAttribute.subscribe.name, PinAttribute.raw.name),
            emptyList()
        )
        queues[MESSAGE_STORAGE_PARSED_PIN_ALIAS] = QueueConfiguration(
            LinkDescription(
                Queue(schemaName, boxName, MESSAGE_STORAGE_PARSED_PIN_ALIAS),
                RoutingKey.EMPTY,
                schemaName
            ),
            listOf(PinAttribute.subscribe.name, PinAttribute.parsed.name),
            emptyList()
        )

        val globalExchange = ConfigLoader.config.rabbitMQManagement.exchangeName
        return MessageRouterConfiguration(queues, GlobalNotification(globalExchange))
    }
}
