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

package com.exactpro.th2.pico.operator.mq.queue

import com.exactpro.th2.pico.operator.mq.queue.AbstractName.Companion.BOX_NAME_REGEXP
import com.exactpro.th2.pico.operator.mq.queue.AbstractName.Companion.NAMESPACE_REGEXP
import com.exactpro.th2.pico.operator.mq.queue.AbstractName.Companion.PIN_NAME_REGEXP

data class RoutingKey(
    val schemaName: String,
    val boxName: String?,
    val pinName: String?
) : AbstractName {

    override fun toString(): String {
        return format(schemaName, boxName, pinName)
    }

    companion object {
        private const val EMPTY_ROUTING_KEY = ""
        private const val ROUTING_KEY_PREFIX = "key"

        val EMPTY = RoutingKey("", "", "")
        val ROUTING_KEY_REGEXP =
            "$ROUTING_KEY_PREFIX\\[$NAMESPACE_REGEXP$BOX_NAME_REGEXP:$PIN_NAME_REGEXP]".toRegex()

        fun format(schemaName: String, boxName: String?, pinName: String?): String {
            return if (schemaName == "" && boxName == "" && pinName == "") {
                EMPTY_ROUTING_KEY
            } else {
                "$ROUTING_KEY_PREFIX[$schemaName:$boxName:$pinName]"
            }
        }

        fun fromString(str: String): RoutingKey? {
            if (str.matches(ROUTING_KEY_REGEXP)) {
                try {
                    val enclosedStr = str.substring(ROUTING_KEY_PREFIX.length + 1, str.length - 1)
                    val tokens = enclosedStr.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    return RoutingKey(tokens[0], tokens[1], tokens[1])
                } catch (ignored: Exception) {
                }
            }
            return null
        }
    }
}
