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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.dataservices.document.DocumentService
import com.mgmtp.a12.dataservices.document.persistence.DocumentComputationStrategy
import com.mgmtp.a12.dataservices.document.persistence.DocumentValidationStrategy
import com.mgmtp.a12.dataservices.rpc.RpcException
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.DocumentV2
import com.mgmtp.a12.uaa.authorization.DecisionContext
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.REMOVE_METADATA_OPERATION
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcExceptionHandler
import com.mgmtp.a12.workflows.extension.internal.operation.constants.InternalDocumentStatus
import com.mgmtp.a12.workflows.extension.internal.utils.WorkflowsDocumentService
import com.mgmtp.a12.workflows.extension.internal.utils.getDocumentPointerToA12wfMetadata
import com.mgmtp.a12.workflows.extension.testFixtures.TestResources.Names.DOCUMENT_MODEL_NAME_WITH_METADATA
import com.mgmtp.a12.workflows.extension.testFixtures.TestResources.Names.DOCUMENT_MODEL_NAME_WITH_NESTED_METADATA
import com.mgmtp.a12.workflows.extension.testFixtures.testUtils.deserializeDocumentV2
import com.mgmtp.a12.workflows.shared.functions.testUtils.readResource
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPath.PROCESS_DEFINITION_PATH
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPath.PROCESS_PATH
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPath.TASK_DEFINITION_PATH
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPath.TASK_PATH
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPath.WORKFLOWS_INTERNAL_DOCUMENT_STATUS_PATH
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.WORKFLOWS_INTERNAL_DOCUMENT_STATUS
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.valueParameters

private class RemoveMetadataOperationTest {

    val mockWorkflowsDocumentService: WorkflowsDocumentService = mockk(relaxed = true)
    val mockDocumentService: DocumentService = mockk(relaxed = true)

    // Subject under test
    val removeMetadataOperation = RemoveMetadataOperation(
        mockWorkflowsDocumentService,
        mockDocumentService,
        WorkflowsRpcExceptionHandler(jacksonObjectMapper()),
    )

    companion object {
        const val DOC_REF_STRING = "someDocumentModel/1"
        val docRef = DocumentReference(DOC_REF_STRING)

        val documentModelString = readResource<String>(this::class, "/models/documentModels/$DOCUMENT_MODEL_NAME_WITH_METADATA.json")
        val documentModelWithNestedMetadataString =
            readResource<String>(this::class, "/models/documentModels/$DOCUMENT_MODEL_NAME_WITH_NESTED_METADATA.json")

        val documentWithMetadataString = readResource<String>(this::class, "/documents/validDocument.json")
        val documentWithNestedMetadataString = readResource<String>(this::class, "/documents/validDocumentWithNestedMetadata.json")
        val documentWithoutMetadataString = readResource<String>(this::class, "/documents/validDocumentWithoutMetadata.json")

        val documentWithMetadata = deserializeDocumentV2(documentWithMetadataString, documentModelString, DOCUMENT_MODEL_NAME_WITH_METADATA)
        val documentWithNestedMetadata = deserializeDocumentV2(documentWithNestedMetadataString, documentModelWithNestedMetadataString, DOCUMENT_MODEL_NAME_WITH_NESTED_METADATA)
        val documentWithoutMetadata = deserializeDocumentV2(documentWithoutMetadataString, documentModelString, DOCUMENT_MODEL_NAME_WITH_METADATA)
    }

    @Nested
    inner class GivenMetadataIsMissing {

        @Test
        fun `should throw RpcException if metadata is missing`() {
            // given
            every { mockWorkflowsDocumentService.loadDocument(any()) } returns documentWithoutMetadata

            // when
            val exception: RpcException = shouldThrow<RpcException> {
                removeMetadataOperation.rpc(docRef)
            }

            // then
            verify(exactly = 0) { mockDocumentService.update(any(), any<DocumentV2>(), any(), any(), any()) }
            exception.operationError.operationId shouldBe REMOVE_METADATA_OPERATION
            exception.message?.lowercase() shouldContain "could not remove metadata from document"
        }
    }

    @Nested
    inner class GivenMetadataExists {

        @BeforeEach
        fun setup() {
            // The property workflowsInternalDocumentStatusProperty is added to the document so its removal can be asserted in the test.
            val modifiedDocument = addWorkflowsInternalDocumentStatusPropertyInDocument(documentWithMetadata)
            every { mockWorkflowsDocumentService.loadDocument(any()) } returns modifiedDocument
        }

        @Test
        fun `should successfully remove task, taskDefinition and workflowsInternalDocumentStatus from document`() {
            // given
            val document = slot<DocumentV2>()

            // when
            removeMetadataOperation.rpc(docRef)

            // then
            verify(exactly = 1) {
                mockDocumentService.update(
                    docRef,
                    capture(document),
                    null,
                    DocumentValidationStrategy.NO_VALIDATION,
                    DocumentComputationStrategy.DEFAULT_CONFIGURATION,
                )
            }

            document.captured.group(TASK_PATH) shouldBe null
            document.captured.group(TASK_DEFINITION_PATH) shouldBe null
            document.captured.field(WORKFLOWS_INTERNAL_DOCUMENT_STATUS_PATH) shouldBe null
        }

        @Test
        fun `should successfully remove task, taskDefinition and workflowsInternalDocumentStatus from document if metadata is nested`() {
            // given
            // The property workflowsInternalDocumentStatusProperty is added to the document so its removal can be asserted in the test.
            val modifiedDocument = addWorkflowsInternalDocumentStatusPropertyInDocument(documentWithNestedMetadata)
            every { mockWorkflowsDocumentService.loadDocument(any()) } returns modifiedDocument
            val document = slot<DocumentV2>()

            // when
            removeMetadataOperation.rpc(docRef)

            // then
            verify(exactly = 1) {
                mockDocumentService.update(
                    docRef,
                    capture(document),
                    null,
                    DocumentValidationStrategy.NO_VALIDATION,
                    DocumentComputationStrategy.DEFAULT_CONFIGURATION,
                )
            }

            document.captured.group("/person$TASK_PATH") shouldBe null
            document.captured.group("/person$TASK_DEFINITION_PATH") shouldBe null
            document.captured.field("/person$WORKFLOWS_INTERNAL_DOCUMENT_STATUS_PATH") shouldBe null
        }

        @Test
        fun `should not remove process metadata`() {
            // given
            val document = slot<DocumentV2>()

            // when
            removeMetadataOperation.rpc(docRef)

            // then
            verify(exactly = 1) {
                mockDocumentService.update(
                    docRef,
                    capture(document),
                    null,
                    DocumentValidationStrategy.NO_VALIDATION,
                    DocumentComputationStrategy.DEFAULT_CONFIGURATION,
                )
            }

            document.captured.group(TASK_PATH) shouldBe null
            document.captured.group(TASK_DEFINITION_PATH) shouldBe null
            document.captured.field(WORKFLOWS_INTERNAL_DOCUMENT_STATUS_PATH) shouldBe null
            document.captured.group(PROCESS_PATH) shouldNotBe null
            document.captured.group(PROCESS_DEFINITION_PATH) shouldNotBe null
        }
    }

    /**
     * Testing the EvaluationContext for this operation would take too much additional setup effort
     * so instead we just assume @DecisionContext works correctly and check if the annotations are present
     */
    @Test
    fun `should have correct @DecisionContext annotations`() {
        // given
        val opClass = RemoveMetadataOperation::class
        val annotatedMethod = opClass.memberFunctions.find { it.name == "rpc" }
        val methodParameters = annotatedMethod!!.valueParameters
        val annotatedParameters = methodParameters.filter { it.findAnnotation<DecisionContext>() != null }
        val unAnnotatedParameters = methodParameters.minus(annotatedParameters)

        // Add parameter names here if they are intentionally unannotated
        val excludedParameters = listOf<String>()

        // then

        // With this check we can make sure that there are no unintentionally unannotated parameters
        unAnnotatedParameters.map { it.name } shouldContainOnly excludedParameters
        annotatedParameters.map { it.name } shouldContainOnly listOf("docRef")
        annotatedParameters.forEach { it.name shouldBe it.findAnnotation<DecisionContext>()!!.value }
    }

    fun addWorkflowsInternalDocumentStatusPropertyInDocument(document: DocumentV2): DocumentV2 {
        val a12WfPointer = document.getDocumentPointerToA12wfMetadata() ?: fail("A12WF does not exist in document.")
        var a12WfGroup = document.group(a12WfPointer)
        a12WfGroup = a12WfGroup.withFieldValue("/$WORKFLOWS_INTERNAL_DOCUMENT_STATUS", InternalDocumentStatus.NON_EMPTY.toString())
        return document.withGroup(a12WfPointer, a12WfGroup)
    }
}
