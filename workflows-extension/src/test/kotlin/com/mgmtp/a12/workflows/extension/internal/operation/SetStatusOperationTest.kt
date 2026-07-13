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

import com.mgmtp.a12.dataservices.document.DataServicesDocument
import com.mgmtp.a12.dataservices.document.DataServicesDocumentFactory
import com.mgmtp.a12.dataservices.document.DocumentPart
import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.dataservices.document.DocumentService
import com.mgmtp.a12.dataservices.document.persistence.DocumentComputationStrategy
import com.mgmtp.a12.dataservices.document.persistence.DocumentValidationStrategy
import com.mgmtp.a12.dataservices.document.persistence.IDocumentRepository
import com.mgmtp.a12.dataservices.rpc.RpcException
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.DocumentV2
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcExceptionHandler
import com.mgmtp.a12.workflows.extension.internal.operation.workarounds.WorkaroundToCreateDocumentMetadata
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.*

private class SetStatusOperationTest {

    val mockDocumentRepository: IDocumentRepository = mockk(relaxed = true)
    val mockWorkaroundToCreateDocumentMetadata: WorkaroundToCreateDocumentMetadata = mockk(relaxed = true)
    val mockDataDocumentServiceFactory: DataServicesDocumentFactory = mockk(relaxed = true)
    val mockDocumentService: DocumentService = mockk(relaxed = true)
    val mockDataServicesDocument: DataServicesDocument = mockk(relaxed = true)
    val documentPart = DocumentPart("/root/status", "REVIEWED", intArrayOf(1, 1))

    val setStatusOperation = SetStatusOperation(
        mockDocumentRepository,
        mockWorkaroundToCreateDocumentMetadata,
        mockDataDocumentServiceFactory,
        mockDocumentService,
        WorkflowsRpcExceptionHandler(jacksonObjectMapper()),
    )

    companion object {
        const val DOCUMENT_MODEL_NAME = "TestForValidation_DM"
        val docRef = DocumentReference(DOCUMENT_MODEL_NAME, "1")
    }

    @Test
    fun `should throw RpcException with correct payload if UPDATE_DOCUMENT_OPERATION fails`() {
        // given
        every { mockDocumentRepository.findByDocumentReference(docRef) } returns null

        // when/then
        val exception = shouldThrow<RpcException> {
            setStatusOperation.rpc(docRef, documentPart)
        }

        // then
        exception.operationError.operationId shouldBe InternalOperationConstants.SET_STATUS_OPERATION
        exception.message?.lowercase() shouldContain "could not update document"
    }

    @Test
    fun `should call document service without validation`() {
        // given
        every { mockDataServicesDocument.metadata.docRef } returns docRef
        every { mockDocumentRepository.findByDocumentReference(docRef) } returns Optional.of(mockDataServicesDocument)

        // when
        setStatusOperation.rpc(docRef, documentPart)

        // then
        verify(exactly = 1) {
            mockDocumentService.update(
                docRef,
                any<DocumentV2>(),
                null,
                DocumentValidationStrategy.NO_VALIDATION,
                DocumentComputationStrategy.DEFAULT_CONFIGURATION,
            )
        }
    }
}
