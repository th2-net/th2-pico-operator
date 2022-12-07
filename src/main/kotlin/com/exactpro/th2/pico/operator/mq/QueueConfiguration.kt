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

package com.exactpro.th2.pico.operator.mq

import com.exactpro.th2.pico.operator.mq.queue.LinkDescription
import com.fasterxml.jackson.annotation.JsonProperty

data class QueueConfiguration(
    private val queue: LinkDescription,
    var attributes: List<String>?,
    val filters: List<Any>?
) {

    @get:JsonProperty("queue")
    val queueName: String
        get() = queue.queue.toString()

    @get:JsonProperty("name")
    val routerKeyName: String
        get() = queue.routingKey.toString()

    @get:JsonProperty("exchange")
    val exchange: String
        get() = queue.exchange
}
