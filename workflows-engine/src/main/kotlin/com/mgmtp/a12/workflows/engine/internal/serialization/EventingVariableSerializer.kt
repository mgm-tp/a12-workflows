/*
 * SPDX-License-Identifier: EUPL-1.2 OR LicenseRef-commercial
 *
 * Copyright (c) 2012-2026 mgm technology partners GmbH
 *
 * Dual License
 * ------------
 * This source file is part of the mgm A12 Platform and available under
 * a choice of two different licenses:
 *
 * 1. Open-Source License – EUPL v1.2
 *    You may redistribute and/or modify this file under the terms of the
 *    European Union Public License, version 1.2 - see https://eupl.eu/.
 *
 * 2. Commercial License
 *    Alternatively, you may obtain a commercial license from
 *    mgm technology partners GmbH, that permits use of this software
 *    under different terms (including support and maintenance services).
 *
 *    Please contact a12-license@mgm-tp.com for more information.
 *
 * You must select and comply with exactly one of the above license options.
 *
 * Warranty Disclaimer (applies to either option)
 * ----------------------------------------------
 * THIS SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * WHETHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT, EXCEPT WHERE SUCH DISCLAIMERS ARE HELD TO BE
 * LEGALLY INVALID. SEE THE RESPECTIVE LICENSE TEXT FOR DETAILS.
 */

package com.mgmtp.a12.workflows.engine.internal.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import org.cibseven.bpm.engine.impl.variable.serializer.JavaObjectSerializer
import org.springframework.context.ApplicationEventPublisher
import java.io.IOException

data class ProcessVariableBeforeSerializationEvent(var variable: String)
data class ProcessVariableAfterDeserializationEvent(var variable: String)

class EventingVariableSerializer(
    private val eventPublisher: ApplicationEventPublisher,
    private val objectMapper: ObjectMapper,
) : JavaObjectSerializer() {

    override fun serializeToByteArray(deserializedObject: Any?): ByteArray {
        val event = ProcessVariableBeforeSerializationEvent(objectMapper.writeValueAsString(deserializedObject))
        eventPublisher.publishEvent(event)

        return super.serializeToByteArray(event.variable)
    }

    override fun deserializeFromByteArray(bytes: ByteArray?, objectTypeName: String?): Any {
        val variable = super.deserializeFromByteArray(bytes, objectTypeName)
        val event = ProcessVariableAfterDeserializationEvent(variable as String)
        eventPublisher.publishEvent(event)

        try {
            return objectMapper.readValue(event.variable, Class.forName(objectTypeName))
        } catch (e: IOException) {
            throw IOException("Error during variable deserialization: ${e.message}")
        }
    }
}
