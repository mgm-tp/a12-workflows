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

package com.mgmtp.a12.workflows.engine.client.internal.dto.process

import com.mgmtp.a12.workflows.shared.dtos.MessageDto
import com.mgmtp.a12.workflows.shared.dtos.ProcessEngineVariable
import com.mgmtp.a12.workflows.shared.functions.testUtils.readResource
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.convertValue
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

private class MessageDtoTest {

    val jacksonObjectMapper = jacksonObjectMapper()

    companion object {
        const val MESSAGE_NAME = "someMessageName"
        const val BUSINESS_KEY = "someBusinessKey"
        const val PROCESS_INSTANCE_ID = "someProcessInstanceId"
        const val PROCESS_VARIABLE_NAME = "additionalVariable"
        const val PROCESS_VARIABLE_VALUE = "test"

        val messageDtoString =
            readResource<String>(this::class, "/dto/messageDto.json")
    }

    @Test
    fun `should successfully convert string to MessageDto`() {
        // when
        val messageDto = jacksonObjectMapper.readValue<MessageDto>(messageDtoString)

        // then
        messageDto.messageName shouldBe MESSAGE_NAME
        messageDto.businessKey shouldBe BUSINESS_KEY
        messageDto.processInstanceId shouldBe PROCESS_INSTANCE_ID

        val processVariablesAsMap = jacksonObjectMapper.convertValue<Map<String, ProcessEngineVariable<*>>>(messageDto.processVariables)
        processVariablesAsMap.size shouldBe 1
        processVariablesAsMap[PROCESS_VARIABLE_NAME]?.value shouldBe PROCESS_VARIABLE_VALUE
    }
}
