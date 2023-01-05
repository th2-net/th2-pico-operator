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

package com.exactpro.th2.pico.operator

import com.exactpro.th2.pico.operator.config.ConfigLoader
import com.exactpro.th2.pico.operator.config.OperatorRunConfig
import kotlin.system.exitProcess

const val EVENT_STORAGE_BOX_ALIAS = "estore"
const val EVENT_STORAGE_PIN_ALIAS = "estore-pin"

const val MESSAGE_STORAGE_BOX_ALIAS = "mstore"
const val MESSAGE_STORAGE_PIN_ALIAS = "mstore-pin-raw"
const val MESSAGE_STORAGE_PARSED_PIN_ALIAS = "mstore-pin-parsed"

val schemaName = ConfigLoader.config.schemaName
val configDir = ConfigLoader.config.generatedConfigsLocation

fun main(args: Array<String>) {
    val mode = if (args.isNotEmpty()) args[0] else "full"
    val old = args.size > 1 && args[1] == "old"
    PicoOperator.run(OperatorRunConfig(mode, old))
    exitProcess(0)
}
