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

package com.mgmtp.a12.workflows.extension.internal.operation

import com.mgmtp.a12.dataservices.common.anonymizing.Anonymizer
import com.mgmtp.a12.dataservices.rpc.RpcException
import com.mgmtp.a12.workflows.engine.client.internal.ProcessEngineOperationBroker
import com.mgmtp.a12.workflows.extension.event.ProcessAfterStartEvent
import com.mgmtp.a12.workflows.extension.event.ProcessBeforeStartEvent
import com.mgmtp.a12.workflows.extension.internal.exception.ProcessEngineExceptionResponse
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcExceptionHandler
import com.mgmtp.a12.workflows.extension.internal.operation.StartProcessOperationTest.TestConstants.BUSINESS_KEY
import com.mgmtp.a12.workflows.extension.internal.operation.StartProcessOperationTest.TestConstants.CURRENT_USER
import com.mgmtp.a12.workflows.extension.internal.operation.StartProcessOperationTest.TestConstants.PROCESS_DEFINITION_KEY
import com.mgmtp.a12.workflows.extension.internal.operation.StartProcessOperationTest.TestConstants.VARIABLES
import com.mgmtp.a12.workflows.extension.internal.operation.StartProcessOperationTest.TestConstants.accessDeniedResponse
import com.mgmtp.a12.workflows.extension.internal.operation.StartProcessOperationTest.TestConstants.inputDocumentReferences
import com.mgmtp.a12.workflows.shared.dtos.ProcessEngineVariable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.StringNode
import tools.jackson.module.kotlin.jacksonObjectMapper

private class StartProcessOperationTest {

    val mockAnonymizer: Anonymizer = mockk()
    val mockProcessEngineOperationBroker: ProcessEngineOperationBroker = mockk(relaxed = true)
    val spyMockEventPublisher: ApplicationEventPublisher = spyk()
    val mockSecurityContext: SecurityContext = mockk(relaxed = true)

    val startProcessOperation = StartProcessOperation(
        mockAnonymizer,
        mockProcessEngineOperationBroker,
        spyMockEventPublisher,
        WorkflowsRpcExceptionHandler(jacksonObjectMapper()),
    )

    object TestConstants {
        const val PROCESS_DEFINITION_KEY = "TestProcessForIntegrationTests"
        const val BUSINESS_KEY = "someBusinessKey"
        const val CURRENT_USER = "currentUser"
        val VARIABLES: JsonNode = jacksonObjectMapper().valueToTree(mapOf("someVariable" to "someValue"))
        val inputDocumentReferences =
            this::class.java.getResource("/requests/onStart/inputDocumentReferences.json")?.readText()

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
        every { mockProcessEngineOperationBroker.startProcessInstance(any(), any(), any()) } just Runs
        every { mockSecurityContext.authentication!!.principal } returns CURRENT_USER
        SecurityContextHolder.setContext(mockSecurityContext)
    }

    @Nested
    @DisplayName("Given Successful Response From Process Engine")
    inner class GivenSuccessfulResponseFromProcessEngine {

        @Test
        fun `should call START_PROCESS operation and start process successfully with all parameters given`() {
            // given
            val variables = jacksonObjectMapper().readTree(inputDocumentReferences)
            val processEngineVariablesSlot = slot<Map<String, ProcessEngineVariable<*>>>()
            val processDefinitionKeySlot = slot<String>()
            val businessKeySlot = slot<String>()

            // when
            startProcessOperation.rpc(PROCESS_DEFINITION_KEY, BUSINESS_KEY, variables)

            // then
            verify(exactly = 1) {
                mockProcessEngineOperationBroker.startProcessInstance(
                    capture(processDefinitionKeySlot),
                    capture(businessKeySlot),
                    capture(processEngineVariablesSlot),
                )
            }

            processDefinitionKeySlot.captured shouldBe PROCESS_DEFINITION_KEY
            businessKeySlot.captured shouldBe BUSINESS_KEY
            processEngineVariablesSlot.captured["customerDocRef"]?.value shouldBe variables["customerDocRef"].asString()
            processEngineVariablesSlot.captured["moreDocRefs"]?.value shouldBe listOf("Order/3", "Order/4")
        }

        @Test
        fun `should start process successfully without 'variables' params and read current user to pass it as ProcessEngineVariable`() {
            // given
            val processEngineVariablesSlot = slot<Map<String, ProcessEngineVariable<*>>>()

            // when
            startProcessOperation.rpc(PROCESS_DEFINITION_KEY, BUSINESS_KEY, null)

            // then
            verify(exactly = 1) { mockProcessEngineOperationBroker.startProcessInstance(any(), any(), capture(processEngineVariablesSlot)) }
            processEngineVariablesSlot.captured["initiator"]?.value shouldBe CURRENT_USER
        }

        @Test
        fun `should read current user and pass it as ProcessEngineVariable`() {
            // given
            val variables = jacksonObjectMapper().readTree(inputDocumentReferences)
            val processEngineVariablesSlot = slot<Map<String, ProcessEngineVariable<*>>>()

            // when
            startProcessOperation.rpc(PROCESS_DEFINITION_KEY, BUSINESS_KEY, variables)

            // then
            verify(exactly = 1) {
                mockProcessEngineOperationBroker.startProcessInstance(PROCESS_DEFINITION_KEY, BUSINESS_KEY, capture(processEngineVariablesSlot))
            }
            processEngineVariablesSlot.captured["initiator"]?.value shouldBe CURRENT_USER
        }

        @Test
        fun `should publish ProcessBeforeStartEvent`() {
            // when
            startProcessOperation.rpc(PROCESS_DEFINITION_KEY, BUSINESS_KEY, VARIABLES)

            // then
            val expectedEvent = ProcessBeforeStartEvent(PROCESS_DEFINITION_KEY, BUSINESS_KEY, VARIABLES)
            verify(exactly = 1) { spyMockEventPublisher.publishEvent(expectedEvent) }
        }

        @Test
        fun `should publish ProcessAfterStartEvent`() {
            // when
            startProcessOperation.rpc(PROCESS_DEFINITION_KEY, BUSINESS_KEY, VARIABLES)

            // then
            val expectedEvent = ProcessAfterStartEvent(PROCESS_DEFINITION_KEY, BUSINESS_KEY, VARIABLES)
            verify(exactly = 1) { spyMockEventPublisher.publishEvent(expectedEvent) }
        }
    }

    @Nested
    @DisplayName("Given AccessDeniedException from Process Engine...")
    inner class GivenAccessDeniedFromProcessEngine {

        @BeforeEach
        fun given() {
            // given
            every { mockProcessEngineOperationBroker.startProcessInstance(any(), any(), any()) } throws accessDeniedResponse
        }

        @Test
        fun `should throw RpcException if request to Process Engine fails`() {
            // when/then
            shouldThrow<RpcException> { startProcessOperation.rpc(PROCESS_DEFINITION_KEY, "", null) }
        }

        @Test
        fun `should publish ONLY before event, NOT after event`() {
            // when
            shouldThrow<RpcException> { startProcessOperation.rpc(PROCESS_DEFINITION_KEY, BUSINESS_KEY, null) }

            // then
            val expectedEvent = ProcessBeforeStartEvent(PROCESS_DEFINITION_KEY, BUSINESS_KEY, null)
            val nonExpectedEvent = ProcessAfterStartEvent(PROCESS_DEFINITION_KEY, BUSINESS_KEY, null)

            verify(exactly = 1) { spyMockEventPublisher.publishEvent(expectedEvent) }
            verify(exactly = 0) { spyMockEventPublisher.publishEvent(nonExpectedEvent) }
        }

        @Test
        fun `should NOT publish any events if variables node has invalid format`() {
            // given
            val invalidVariablesNode = StringNode("just text, no variable name with value")

            // when
            shouldThrow<RpcException> { startProcessOperation.rpc(PROCESS_DEFINITION_KEY, BUSINESS_KEY, invalidVariablesNode) }

            // then
            verify { spyMockEventPublisher wasNot Called }
        }
    }
}
