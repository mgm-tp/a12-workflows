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

import com.mgmtp.a12.dataservices.document.DocumentService
import com.mgmtp.a12.dataservices.document.support.DocumentSupport
import com.mgmtp.a12.dataservices.rpc.RpcException
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.CREATE_DOCUMENT_OPERATION
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcExceptionHandler
import com.mgmtp.a12.workflows.extension.testFixtures.TestResources.Names.DOCUMENT_MODEL_NAME_WITH_METADATA
import com.mgmtp.a12.workflows.shared.functions.testUtils.readResource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.io.Reader

private class CreateDocumentOperationTest {

    val mockDocumentSupport: DocumentSupport = mockk(relaxed = true)
    val mockDocumentService: DocumentService = mockk(relaxed = true)
    val spyObjectMapper: ObjectMapper = spyk(jacksonObjectMapper())

    // Subject under test
    val createDocumentOperation = CreateDocumentOperation(
        mockDocumentSupport,
        mockDocumentService,
        spyObjectMapper,
        WorkflowsRpcExceptionHandler(spyObjectMapper),
    )

    companion object {
        val documentWithMetadataNode = readResource<JsonNode>(this::class, "/documents/validDocument.json")
    }

    @Test
    fun `should throw RpcException with correct payload if CREATE_DOCUMENT_OPERATION fails`() {
        // given
        every { mockDocumentSupport.convertJSONToDocument(any(), any<JsonNode>()) } throws Exception()
        every { mockDocumentSupport.convertJSONToDocument(any(), any<Reader>()) } throws Exception()

        // when/then
        val exception = shouldThrow<RpcException> {
            createDocumentOperation.rpc(DOCUMENT_MODEL_NAME_WITH_METADATA, documentWithMetadataNode)
        }

        // then
        exception.operationError.operationId shouldBe CREATE_DOCUMENT_OPERATION
        exception.message?.lowercase() shouldContain "could not create document"
    }
}
