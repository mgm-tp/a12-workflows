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

// Aliases needed or else it gets mixed up with IllegalStateException from Kotlin
import com.mgmtp.a12.dataservices.common.anonymizing.Anonymizer
import com.mgmtp.a12.dataservices.common.exception.NotFoundException
import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.dataservices.document.DocumentService
import com.mgmtp.a12.dataservices.document.persistence.DocumentComputationStrategy
import com.mgmtp.a12.dataservices.document.persistence.DocumentValidationStrategy
import com.mgmtp.a12.dataservices.rpc.RpcException
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.DocumentV2
import com.mgmtp.a12.workflows.client.operation.WorkflowsOperationConstants
import com.mgmtp.a12.workflows.engine.client.internal.ProcessEngineOperationBroker
import com.mgmtp.a12.workflows.extension.event.TaskAfterAssignEvent
import com.mgmtp.a12.workflows.extension.event.TaskBeforeAssignEvent
import com.mgmtp.a12.workflows.extension.internal.exception.ProcessEngineExceptionResponse
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcExceptionHandler
import com.mgmtp.a12.workflows.extension.internal.utils.WorkflowsDocumentService
import com.mgmtp.a12.workflows.extension.internal.utils.getTextValueFromA12wfMetadata
import com.mgmtp.a12.workflows.extension.testFixtures.TestResources.Names.DOCUMENT_MODEL_NAME_WITH_METADATA
import com.mgmtp.a12.workflows.extension.testFixtures.TestResources.Names.DOCUMENT_MODEL_NAME_WITH_NESTED_METADATA
import com.mgmtp.a12.workflows.extension.testFixtures.testUtils.deserializeDocumentV2
import com.mgmtp.a12.workflows.shared.functions.testUtils.readResource
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPath.RELATIVE_TASK_ASSIGNEE_PATH
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.A12WF
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.context.ApplicationEventPublisher
import org.springframework.web.client.RestClientResponseException
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.stream.Stream

private class AssignTaskOperationTest {

    val mockAnonymizer: Anonymizer = mockk()
    val mockWorkflowsDocumentService: WorkflowsDocumentService = mockk(relaxed = true)
    val mockDocumentService: DocumentService = mockk(relaxed = true)
    val spyEventPublisher: ApplicationEventPublisher = spyk()
    val spyProcessEngineOperationBroker: ProcessEngineOperationBroker = spyk()

    // Subject under test
    val assignTaskOperation = AssignTaskOperation(
        mockAnonymizer,
        mockWorkflowsDocumentService,
        mockDocumentService,
        spyEventPublisher,
        spyProcessEngineOperationBroker,
        WorkflowsRpcExceptionHandler(jacksonObjectMapper()),
    )

    companion object {
        const val TASK_ID = "530d14f2-107f-11ec-8308-803253632adb"
        const val NEW_ASSIGNEE = "newAssignee"

        val documentModelString = readResource<String>(this::class, "/models/documentModels/$DOCUMENT_MODEL_NAME_WITH_METADATA.json")
        val documentModelWithNestedMetadataString =
            readResource<String>(this::class, "/models/documentModels/$DOCUMENT_MODEL_NAME_WITH_NESTED_METADATA.json")

        val documentWithMetadataString = readResource<String>(this::class, "/documents/validDocument.json")
        val documentWithNestedMetadataString = readResource<String>(this::class, "/documents/validDocumentWithNestedMetadata.json")

        val documentWithMetadata = deserializeDocumentV2(documentWithMetadataString, documentModelString, DOCUMENT_MODEL_NAME_WITH_METADATA)
        val documentWithNestedMetadata = deserializeDocumentV2(documentWithNestedMetadataString, documentModelWithNestedMetadataString, DOCUMENT_MODEL_NAME_WITH_NESTED_METADATA)

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

        @JvmStatic
        fun documentsUnderTestProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(documentWithMetadata),
            Arguments.of(documentWithNestedMetadata),
        )
    }

    @BeforeEach
    fun setupMocks() {
        every { mockAnonymizer.apply(any()) } returns "***"
        every { mockWorkflowsDocumentService.loadDocument(any()) } returns documentWithMetadata
    }

    @Nested
    @DisplayName("Given existing task document...")
    inner class GivenExistingTaskDocument {

        val existingTaskDocRef = DocumentReference("Existing", "1")

        @BeforeEach
        fun given() {
            every { mockWorkflowsDocumentService.loadDocument(any()) } returns documentWithMetadata
        }

        @Test
        fun `should call DocumentService internally`() {
            // when
            assignTaskOperation.rpc(existingTaskDocRef, NEW_ASSIGNEE)

            // then
            verify(exactly = 1) {
                mockDocumentService.update(
                    existingTaskDocRef,
                    any<DocumentV2>(),
                    null,
                    DocumentValidationStrategy.NO_VALIDATION,
                    DocumentComputationStrategy.DEFAULT_CONFIGURATION,
                )
            }
        }

        @ParameterizedTest
        @MethodSource("com.mgmtp.a12.workflows.extension.internal.operation.AssignTaskOperationTest#documentsUnderTestProvider")
        fun `should update assignee field`(document: DocumentV2) {
            // given
            every { mockWorkflowsDocumentService.loadDocument(any()) } returns document
            val documentSlot = mutableListOf<DocumentV2>()

            // when
            assignTaskOperation.rpc(existingTaskDocRef, NEW_ASSIGNEE)

            // then
            verify { mockDocumentService.update(existingTaskDocRef, capture(documentSlot), null, DocumentValidationStrategy.NO_VALIDATION, DocumentComputationStrategy.DEFAULT_CONFIGURATION) }
            documentSlot.last().getTextValueFromA12wfMetadata(RELATIVE_TASK_ASSIGNEE_PATH) shouldBe NEW_ASSIGNEE
        }

        @Test
        fun `should NOT update anything except the assignee field`() {
            // given
            val originalDocument = mockWorkflowsDocumentService.loadDocument(existingTaskDocRef)
            val documentSlot = mutableListOf<DocumentV2>()

            // when
            assignTaskOperation.rpc(existingTaskDocRef, NEW_ASSIGNEE)

            // then
            verify { mockDocumentService.update(existingTaskDocRef, capture(documentSlot), any(), DocumentValidationStrategy.NO_VALIDATION, DocumentComputationStrategy.DEFAULT_CONFIGURATION) }
            documentSlot.last() shouldBeEqualIgnoringAssignee originalDocument
        }

        @Test
        fun `should publish TaskBeforeAssignEvent`() {
            // when
            assignTaskOperation.rpc(existingTaskDocRef, NEW_ASSIGNEE)

            // then
            val expectedEvent = TaskBeforeAssignEvent(existingTaskDocRef, NEW_ASSIGNEE)
            verify(exactly = 1) { spyEventPublisher.publishEvent(expectedEvent) }
        }

        @Test
        fun `should publish TaskAfterAssignEvent`() {
            // when
            assignTaskOperation.rpc(existingTaskDocRef, NEW_ASSIGNEE)

            // then
            val expectedEvent = TaskAfterAssignEvent(existingTaskDocRef, NEW_ASSIGNEE)
            verify(exactly = 1) { spyEventPublisher.publishEvent(expectedEvent) }
        }

        @ParameterizedTest
        @MethodSource("com.mgmtp.a12.workflows.extension.internal.operation.AssignTaskOperationTest#documentsUnderTestProvider")
        fun `should send task assign request to Process Engine with correct taskId`(document: DocumentV2) {
            // given
            every { mockWorkflowsDocumentService.loadDocument(any()) } returns document

            // when
            assignTaskOperation.rpc(existingTaskDocRef, NEW_ASSIGNEE)

            // then
            verify(exactly = 1) { spyProcessEngineOperationBroker.assign(TASK_ID, any(), NEW_ASSIGNEE) }
        }
    }

    @Nested
    @DisplayName("Given non-existing task document...")
    inner class GivenNonExistingTaskDocument {

        val nonExistingTaskDocRef = DocumentReference("NonExisting", "1")

        @BeforeEach
        fun given() {
            every { mockWorkflowsDocumentService.loadDocument(nonExistingTaskDocRef) } throws NotFoundException("", "")
        }

        @Test
        fun `should throw RpcException with correct payload`() {
            // when/then
            val exception =
                shouldThrow<RpcException> { assignTaskOperation.rpc(nonExistingTaskDocRef, NEW_ASSIGNEE) }

            // then
            exception.operationError.operationId shouldBe WorkflowsOperationConstants.TASK_ASSIGN_OPERATION
            exception.message?.lowercase() shouldContain "could not assign"
            exception.message?.lowercase() shouldContain "not found"
        }

        @Test
        fun `should NOT call DocumentService`() {
            // when/then
            shouldThrow<RpcException> { assignTaskOperation.rpc(nonExistingTaskDocRef, NEW_ASSIGNEE) }

            // then
            verify(exactly = 0) { mockDocumentService.update(any(), any<DocumentV2>(), any(), any(), any()) }
        }

        @Test
        fun `should NOT send task assign request to Process Engine`() {
            // when/then
            shouldThrow<RpcException> { assignTaskOperation.rpc(nonExistingTaskDocRef, NEW_ASSIGNEE) }

            // then
            verify(exactly = 0) { spyProcessEngineOperationBroker.assign(any(), any(), any()) }
        }

        @Test
        fun `should still publish TaskBeforeAssignEvent`() {
            // when
            shouldThrow<RpcException> { assignTaskOperation.rpc(nonExistingTaskDocRef, NEW_ASSIGNEE) }

            // then
            val expectedEvent = TaskBeforeAssignEvent(nonExistingTaskDocRef, NEW_ASSIGNEE)
            verify(exactly = 1) { spyEventPublisher.publishEvent(expectedEvent) }
        }

        @Test
        fun `should NOT publish TaskAfterAssignEvent`() {
            // when
            shouldThrow<RpcException> { assignTaskOperation.rpc(nonExistingTaskDocRef, NEW_ASSIGNEE) }

            // then
            val nonExpectedEvent = TaskAfterAssignEvent(nonExistingTaskDocRef, NEW_ASSIGNEE)
            verify(exactly = 0) { spyEventPublisher.publishEvent(nonExpectedEvent) }
        }
    }

    @Nested
    @DisplayName("Given AccessDeniedException from Process Engine...")
    inner class GivenAccessDeniedFromProcessEngine {

        val someTaskDocRef = DocumentReference("SomeModel", "1")

        @BeforeEach
        fun given() {
            // given
            every { spyProcessEngineOperationBroker.assign(any(), any(), any()) } throws accessDeniedResponse
        }

        @Test
        fun `should throw RpcException if request to Process Engine fails`() {
            // when/then
            shouldThrow<RpcException> {
                assignTaskOperation.rpc(someTaskDocRef, NEW_ASSIGNEE)
            }
        }

        @Test
        fun `should publish ONLY before event, NOT after event`() {
            // when
            shouldThrow<RpcException> {
                assignTaskOperation.rpc(someTaskDocRef, NEW_ASSIGNEE)
            }

            // then
            val expectedEvent = TaskBeforeAssignEvent(someTaskDocRef, NEW_ASSIGNEE)
            val nonExpectedEvent = TaskAfterAssignEvent(someTaskDocRef, NEW_ASSIGNEE)

            verify(exactly = 1) { spyEventPublisher.publishEvent(expectedEvent) }
            verify(exactly = 0) { spyEventPublisher.publishEvent(nonExpectedEvent) }
        }
    }

    infix fun DocumentV2.shouldBeEqualIgnoringAssignee(other: DocumentV2) {
        this.withFieldRemoved("$A12WF$RELATIVE_TASK_ASSIGNEE_PATH") shouldBe other.withFieldRemoved("$A12WF$RELATIVE_TASK_ASSIGNEE_PATH")
    }
}
