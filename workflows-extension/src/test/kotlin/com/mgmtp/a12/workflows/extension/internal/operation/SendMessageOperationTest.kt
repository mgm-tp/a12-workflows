/**
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
 * THIS SOFTWARE IS PROVIDED “AS IS” AND WITHOUT WARRANTY OF ANY KIND,
 * WHETHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT, EXCEPT WHERE SUCH DISCLAIMERS ARE HELD TO BE
 * LEGALLY INVALID. SEE THE RESPECTIVE LICENSE TEXT FOR DETAILS.
 */
package com.mgmtp.a12.workflows.extension.internal.operation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mgmtp.a12.dataservices.common.anonymizing.Anonymizer
import com.mgmtp.a12.dataservices.rpc.RpcException
import com.mgmtp.a12.workflows.engine.client.internal.ProcessEngineOperationBroker
import com.mgmtp.a12.workflows.extension.event.ProcessAfterMessageSendEvent
import com.mgmtp.a12.workflows.extension.event.ProcessBeforeMessageSendEvent
import com.mgmtp.a12.workflows.extension.internal.exception.ProcessEngineExceptionResponse
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcExceptionHandler
import com.mgmtp.a12.workflows.extension.internal.operation.SendMessageOperationTest.TestConstants.BUSINESS_KEY
import com.mgmtp.a12.workflows.extension.internal.operation.SendMessageOperationTest.TestConstants.MESSAGE_NAME
import com.mgmtp.a12.workflows.extension.internal.operation.SendMessageOperationTest.TestConstants.PROCESS_INSTANCE_ID
import com.mgmtp.a12.workflows.extension.internal.operation.SendMessageOperationTest.TestConstants.accessDeniedResponse
import com.mgmtp.a12.workflows.extension.internal.operation.SendMessageOperationTest.TestConstants.emptyPayload
import com.mgmtp.a12.workflows.extension.internal.operation.SendMessageOperationTest.TestConstants.invalidPayload
import com.mgmtp.a12.workflows.extension.internal.operation.SendMessageOperationTest.TestConstants.payload
import com.mgmtp.a12.workflows.shared.dtos.MessageDto
import com.mgmtp.a12.workflows.shared.dtos.ProcessEngineVariable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.web.client.RestClientResponseException
import jakarta.jms.IllegalStateException as JakartaJmsIllegalStateException
import org.springframework.jms.IllegalStateException as JmsIllegalStateException

private class SendMessageOperationTest {

    val mockAnonymizer: Anonymizer = mockk()
    val mockProcessEngineOperationBroker: ProcessEngineOperationBroker = mockk(relaxed = true)
    val spyEventPublisher: ApplicationEventPublisher = spyk()

    val sendMessageOperation = SendMessageOperation(
        mockAnonymizer,
        mockProcessEngineOperationBroker,
        spyEventPublisher,
        WorkflowsRpcExceptionHandler(jacksonObjectMapper()),
    )

    object TestConstants {
        const val PROCESS_INSTANCE_ID = "123ABC"
        const val MESSAGE_NAME = "someMessage"
        const val BUSINESS_KEY = "someBusinessKey"

        val payload: JsonNode = jacksonObjectMapper().readTree("""{ "someKey": "someValue" }""")
        val emptyPayload: JsonNode = jacksonObjectMapper().readTree("{}")
        val invalidPayload = TextNode("just text, no key-value pair")

        val exceptionResponseAsString: String = jacksonObjectMapper()
            .writeValueAsString(ProcessEngineExceptionResponse("AccessDeniedException", "", -1))
        val accessDeniedResponse = RestClientResponseException(
            "",
            500,
            "",
            null,
            exceptionResponseAsString.toByteArray(),
            null,
        )
    }

    @BeforeEach
    fun setup() {
        every { mockAnonymizer.apply(any()) } returns "***"
        justRun { mockProcessEngineOperationBroker.sendMessage(any()) }
    }

    @Nested
    @DisplayName("Given Successful Response From Process Engine")
    inner class GivenSuccessfulResponseFromProcessEngine {

        @Test
        fun `should call SEND_MESSAGE operation and send message successfully with all parameters given`() {
            // given
            val messageDtoSlot = slot<MessageDto>()

            // when
            sendMessageOperation.rpc(MESSAGE_NAME, BUSINESS_KEY, PROCESS_INSTANCE_ID, payload)

            // then
            verify(exactly = 1) { mockProcessEngineOperationBroker.sendMessage(capture(messageDtoSlot)) }

            val expectedTransformedPayload = mapOf("someKey" to ProcessEngineVariable("someValue"))

            messageDtoSlot.captured.processInstanceId shouldBe PROCESS_INSTANCE_ID
            messageDtoSlot.captured.messageName shouldBe MESSAGE_NAME
            messageDtoSlot.captured.businessKey shouldBe BUSINESS_KEY
            messageDtoSlot.captured.processVariables shouldBe expectedTransformedPayload
        }

        @Test
        fun `should call SEND_MESSAGE operation with null processInstanceId`() {
            // given
            val messageDtoSlot = slot<MessageDto>()

            // when
            sendMessageOperation.rpc(MESSAGE_NAME, BUSINESS_KEY, null, payload)

            // then
            verify(exactly = 1) { mockProcessEngineOperationBroker.sendMessage(capture(messageDtoSlot)) }

            messageDtoSlot.captured.processInstanceId shouldBe null
            messageDtoSlot.captured.businessKey shouldBe BUSINESS_KEY
        }

        @Test
        fun `should call SEND_MESSAGE operation with empty payload`() {
            // given
            val messageDtoSlot = slot<MessageDto>()

            // when
            sendMessageOperation.rpc(MESSAGE_NAME, BUSINESS_KEY, PROCESS_INSTANCE_ID, emptyPayload)

            // then
            verify(exactly = 1) { mockProcessEngineOperationBroker.sendMessage(capture(messageDtoSlot)) }

            messageDtoSlot.captured.processVariables shouldBe emptyMap()
        }

        @Test
        fun `should call SEND_MESSAGE operation with null payload`() {
            // when
            sendMessageOperation.rpc(MESSAGE_NAME, BUSINESS_KEY, PROCESS_INSTANCE_ID, null)

            // then
            verify(exactly = 1) { mockProcessEngineOperationBroker.sendMessage(any()) }
        }

        @Test
        fun `should publish ProcessBeforeMessageSendEvent`() {
            // when
            sendMessageOperation.rpc(MESSAGE_NAME, BUSINESS_KEY, PROCESS_INSTANCE_ID, payload)

            // then
            val expectedEvent = ProcessBeforeMessageSendEvent(MESSAGE_NAME, BUSINESS_KEY, PROCESS_INSTANCE_ID, payload)
            verify(exactly = 1) { spyEventPublisher.publishEvent(expectedEvent) }
        }

        @Test
        fun `should publish ProcessAfterMessageSendEvent`() {
            // when
            sendMessageOperation.rpc(MESSAGE_NAME, BUSINESS_KEY, PROCESS_INSTANCE_ID, payload)

            // then
            val expectedEvent = ProcessAfterMessageSendEvent(MESSAGE_NAME, BUSINESS_KEY, PROCESS_INSTANCE_ID, payload)
            verify(exactly = 1) { spyEventPublisher.publishEvent(expectedEvent) }
        }

        @Test
        fun `should NOT publish any events if given payload has invalid format`() {
            // when
            shouldThrow<IllegalArgumentException> {
                sendMessageOperation.rpc(MESSAGE_NAME, BUSINESS_KEY, PROCESS_INSTANCE_ID, invalidPayload)
            }

            // then
            verify { spyEventPublisher wasNot Called }
        }
    }

    @Nested
    @DisplayName("Given AccessDeniedException from Process Engine...")
    inner class GivenAccessDeniedFromProcessEngine {

        @BeforeEach
        fun given() {
            // given
            every { mockProcessEngineOperationBroker.sendMessage(any()) } throws accessDeniedResponse
        }

        @Test
        fun `should throw RpcException if request to Process Engine fails`() {
            // when/then
            shouldThrow<RpcException> {
                sendMessageOperation.rpc(MESSAGE_NAME, BUSINESS_KEY, PROCESS_INSTANCE_ID, emptyPayload)
            }
        }

        @Test
        fun `should publish ONLY before event, NOT after event`() {
            // when
            shouldThrow<RpcException> {
                sendMessageOperation.rpc(MESSAGE_NAME, BUSINESS_KEY, PROCESS_INSTANCE_ID, payload)
            }

            // then
            val expectedEvent = ProcessBeforeMessageSendEvent(MESSAGE_NAME, BUSINESS_KEY, PROCESS_INSTANCE_ID, payload)
            val nonExpectedEvent = ProcessAfterMessageSendEvent(MESSAGE_NAME, BUSINESS_KEY, PROCESS_INSTANCE_ID, payload)

            verify(exactly = 1) { spyEventPublisher.publishEvent(expectedEvent) }
            verify(exactly = 0) { spyEventPublisher.publishEvent(nonExpectedEvent) }
        }
    }

    @Nested
    @DisplayName("Given JmsException...")
    inner class GivenJmsException {

        val jmsException =
            JmsIllegalStateException(JakartaJmsIllegalStateException("some error message"))

        @Test
        fun `should throw RpcException if request via jms to Process Engine fails`() {
            // given
            every { mockProcessEngineOperationBroker.sendMessage(any()) } throws jmsException

            // when/then
            shouldThrow<RpcException> {
                sendMessageOperation.rpc(MESSAGE_NAME, BUSINESS_KEY, PROCESS_INSTANCE_ID, payload)
            }
        }
    }
}
