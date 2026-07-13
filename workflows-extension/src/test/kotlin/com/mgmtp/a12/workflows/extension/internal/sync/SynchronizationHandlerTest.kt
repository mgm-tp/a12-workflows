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

package com.mgmtp.a12.workflows.extension.internal.sync

import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.dataservices.document.internal.DefaultDataServicesDocument
import com.mgmtp.a12.dataservices.document.internal.DefaultDocumentMetadata
import com.mgmtp.a12.dataservices.document.persistence.internal.DefaultDocumentModelResolver
import com.mgmtp.a12.dataservices.model.document.persistence.DocumentModelLoader
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.DocumentV2
import com.mgmtp.a12.kernel.md.model.internal.service.DocumentModelSearchServiceImpl
import com.mgmtp.a12.workflows.engine.client.internal.ProcessEngineClient
import com.mgmtp.a12.workflows.extension.testFixtures.TestResources.Names.DOCUMENT_MODEL_NAME_WITH_METADATA
import com.mgmtp.a12.workflows.extension.testFixtures.testUtils.deserializeDocumentModel
import com.mgmtp.a12.workflows.shared.functions.testUtils.readResource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

private class SynchronizationHandlerTest {

    val mockDocumentModelLoader: DocumentModelLoader = mockk(relaxed = true)
    val mockProcessEngineClient: ProcessEngineClient = mockk(relaxed = true)
    val mockDefaultDocumentModelResolver: DefaultDocumentModelResolver = mockk(relaxed = true)
    val workflowsDocumentModelWalker = WorkflowsDocumentModelWalker(mockDefaultDocumentModelResolver)

    val synchronizationHandler = SynchronizationHandler(
        mockDocumentModelLoader,
        mockProcessEngineClient,
        workflowsDocumentModelWalker,
    )

    val kernelDocument: DocumentV2 = DocumentV2.empty(DOCUMENT_MODEL_NAME_WITH_METADATA)
    val mockDefaultDocumentMetadata: DefaultDocumentMetadata = mockk(relaxed = true)

    companion object {
        const val TASK_ID = "someTaskId"
        val docRef = DocumentReference("$DOCUMENT_MODEL_NAME_WITH_METADATA/1")
        val documentModelString = readResource<String>(
            this::class,
            "/models/documentModels/$DOCUMENT_MODEL_NAME_WITH_METADATA.json",
        )
        val documentModel = deserializeDocumentModel(documentModelString)
    }

    @BeforeEach
    fun setup() {
        every { mockDocumentModelLoader.loadModel(any()) } returns documentModel
        every { mockDefaultDocumentModelResolver.getDocumentModelSearchService(any()) } returns Optional.of(DocumentModelSearchServiceImpl(documentModel))
    }

    @Test
    fun `should call updateTask on ProcessEngineClient`() {
        // given
        val dataServicesDocument = DefaultDataServicesDocument(
            kernelDocument,
            mockDefaultDocumentMetadata,
        )

        every { dataServicesDocument.metadata.docRef } returns docRef

        // when
        synchronizationHandler.propagateValues(dataServicesDocument, TASK_ID)

        // then
        verify { mockProcessEngineClient.updateTask(TASK_ID, any()) }
    }
}
