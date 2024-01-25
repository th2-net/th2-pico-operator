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

package com.exactpro.th2.pico.operator.mq.queue

import com.exactpro.th2.model.latest.box.pins.MqPublisher
import com.exactpro.th2.model.latest.link.LinkEndpoint
import com.exactpro.th2.pico.operator.EVENT_STORAGE_BOX_ALIAS
import com.exactpro.th2.pico.operator.EVENT_STORAGE_PIN_ALIAS
import com.exactpro.th2.pico.operator.MESSAGE_STORAGE_BOX_ALIAS
import com.exactpro.th2.pico.operator.MESSAGE_STORAGE_PIN_ALIAS
import com.exactpro.th2.pico.operator.mq.PinAttribute
import com.exactpro.th2.pico.operator.mq.RabbitMQManager
import com.exactpro.th2.pico.operator.mq.queue.RoutingKey.Companion.ROUTING_KEY_REGEXP
import com.exactpro.th2.pico.operator.repo.BoxResource
import mu.KotlinLogging

class BindQueueLinkResolver(
    private val resource: BoxResource,
    private val rabbitMQManager: RabbitMQManager,
) {

    private val schemaName = rabbitMQManager.schemaName
    private val resourceName = resource.metadata.name
    private val channel = rabbitMQManager.channel

    fun handle() {
        if (resourceName == EVENT_STORAGE_BOX_ALIAS || resourceName == MESSAGE_STORAGE_BOX_ALIAS) {
            return
        }
        resolveDeclaredLinks()
        resolveHiddenLinks()
    }

    private fun resolveDeclaredLinks() {
        val subscribers = resource.spec.pins?.mq?.subscribers ?: return
        for (subscriberPin in subscribers) {
            val queue = Queue(schemaName, resourceName, subscriberPin.name)
            val currentPinLinks: List<LinkEndpoint> = subscriberPin.linkTo ?: emptyList()
            for ((box, pin) in currentPinLinks) {
                val linkDescription = LinkDescription(
                    queue,
                    RoutingKey(schemaName, box, pin),
                    schemaName
                )
                bindQueue(linkDescription)
            }
            removeExtinctBindings(queue, currentPinLinks)
        }
    }

    private fun resolveHiddenLinks() {
        // create event storage link for each resource
        val estoreLinkDescription = LinkDescription(
            Queue(schemaName, EVENT_STORAGE_BOX_ALIAS, EVENT_STORAGE_PIN_ALIAS),
            RoutingKey(schemaName, resourceName, EVENT_STORAGE_PIN_ALIAS),
            schemaName
        )
        bindQueue(estoreLinkDescription)

        // create message store link for only resources that need it
        val currentLinks: MutableList<LinkEndpoint> = ArrayList()
        val mstoreQueue = Queue(schemaName, MESSAGE_STORAGE_BOX_ALIAS, MESSAGE_STORAGE_PIN_ALIAS)
        val publishers: List<MqPublisher> = resource.spec.pins?.mq?.publishers ?: emptyList()
        for ((pinName, attributes) in publishers) {
            if (attributes?.contains(PinAttribute.store.name) == true &&
                attributes.contains(PinAttribute.raw.name) &&
                !attributes.contains(PinAttribute.parsed.name)
            ) {
                val mstoreLinkDescription = LinkDescription(
                    mstoreQueue,
                    RoutingKey(schemaName, resourceName, pinName),
                    schemaName
                )
                bindQueue(mstoreLinkDescription)
                currentLinks.add(LinkEndpoint(resourceName, pinName))
            }
        }
        removeExtinctBindings(mstoreQueue, currentLinks)
    }

    private fun bindQueue(queue: LinkDescription) {
        try {
            val queueName = queue.queue.toString()
            val routingKeyName = queue.routingKey.toString()
            val currentQueue = rabbitMQManager.getQueue(queueName)
            if (currentQueue == null) {
                LOGGER.info("Queue '{}' does not yet exist. skipping binding", queueName)
                return
            }
            channel.queueBind(queueName, queue.exchange, routingKeyName)
            LOGGER.info(
                "Queue '{}' successfully bound to '{}'",
                queueName,
                routingKeyName
            )
        } catch (e: Exception) {
            val message = "Exception while working with rabbitMq"
            LOGGER.error(message, e)
            throw e
        }
    }

    private fun removeExtinctBindings(
        queue: Queue,
        currentLinks: List<LinkEndpoint>,
    ) {
        val queueName = queue.toString()
        val bindingOnRabbit = rabbitMQManager.getQueueBindings(queueName)
            .map { it.routingKey }
            .filter { it.matches(ROUTING_KEY_REGEXP) && RoutingKey.fromString(it)?.boxName == resourceName }
        val currentBindings = currentLinks.mapTo(HashSet()) {
            RoutingKey(schemaName, it.box, it.pin).toString()
        }
        try {
            bindingOnRabbit.forEach {
                if (!currentBindings.contains(it)) {
                    val currentQueue = rabbitMQManager.getQueue(queueName)
                    if (currentQueue == null) {
                        LOGGER.info("Queue '{}' already removed. skipping unbinding", queueName)
                        return
                    }
                    channel.queueUnbind(queueName, schemaName, it)
                    LOGGER.info("Unbind queue '{}' -> '{}'.", it, queueName)
                }
            }
        } catch (e: Exception) {
            val message = "Exception while removing bindings"
            LOGGER.error(message, e)
            throw e
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger { }
    }
}
