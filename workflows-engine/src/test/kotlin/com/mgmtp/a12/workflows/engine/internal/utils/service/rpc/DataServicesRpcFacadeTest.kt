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
package com.mgmtp.a12.workflows.engine.internal.utils.service.rpc

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mgmtp.a12.dataservices.client.rpc.RequestBuilderFactory
import com.mgmtp.a12.dataservices.client.rpc.RestRpcOperationsClient
import com.mgmtp.a12.dataservices.document.DocumentPart
import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.dataservices.document.operation.CoreOperationConstants
import com.mgmtp.a12.dataservices.rpc.JsonRpc2Request
import com.mgmtp.a12.dataservices.rpc.JsonRpc2Response
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.data.JsonRpcRequestParams
import com.mgmtp.a12.workflows.shared.dtos.ProcessMetadata
import com.mgmtp.a12.workflows.shared.functions.testUtils.readResource
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class DataServicesRpcFacadeTest {

    val mockRpcOperationsClient: RestRpcOperationsClient = mockk(relaxed = true)
    val objectMapper = jacksonObjectMapper()
    val dsRequestBuilderFactory: RequestBuilderFactory = RequestBuilderFactory(objectMapper)
    val dataServicesRpcFacade = DataServicesRpcFacade(mockRpcOperationsClient, dsRequestBuilderFactory)

    companion object {
        const val DOCUMENT_MODEL_NAME = "SimplePerson_DM"
        const val DOC_REF = "$DOCUMENT_MODEL_NAME/1"
        val document = readResource<JsonNode>(this::class, "/documents/someDocumentWithoutMetadata_SimplePerson_DM.json")
        val metadataString = readResource<String>(this::class, "/documents/someDocumentWithMetadata_SimplePerson_DM.json")

        val documentPart = DocumentPart("somePath", "someNewValue", intArrayOf(1, 1))

        val slot = slot<List<JsonRpc2Request>>()
    }

    @BeforeEach
    fun setup() {
        // given
        val resultNode = objectMapper.createObjectNode().apply { put(JsonRpcRequestParams.DOCUMENT_REFERENCE, DOC_REF) }
        val response = JsonRpc2Response(resultNode)
        every { mockRpcOperationsClient.invoke(capture(slot)) } returns listOf(response)
    }

    @Test
    fun `should successfully invoke ADD_DOCUMENT operation`() {
        // when
        val docRef = dataServicesRpcFacade.tryAddDocument(document, DOCUMENT_MODEL_NAME)

        // then
        docRef shouldBe DocumentReference(DOC_REF)
        val request = slot.captured.first()
        request.method shouldBe CoreOperationConstants.ADD_DOCUMENT_OPERATION
        request.params.findValue(JsonRpcRequestParams.DOCUMENT_MODEL_NAME).textValue() shouldBe DOCUMENT_MODEL_NAME
        request.params.findValue(JsonRpcRequestParams.DOCUMENT) shouldBe document
    }

    @Test
    fun `should successfully invoke REMOVE_METADATA operation`() {
        // when
        dataServicesRpcFacade.tryRemoveMetadata(
            DocumentReference(DOC_REF),
        )

        // then
        val request = slot.captured.first()
        request.method shouldBe InternalOperationConstants.REMOVE_METADATA_OPERATION
        request.params.findValue(JsonRpcRequestParams.DOCUMENT_REFERENCE) shouldBe objectMapper.valueToTree(
            DocumentReference(
                DOC_REF,
            ),
        )
    }

    @Test
    fun `should successfully invoke CREATE_METADATA operation`() {
        // given
        val metadataNode = objectMapper.readTree(metadataString).findValue("A12WF")
        val metadata = objectMapper.treeToValue(metadataNode, ProcessMetadata::class.java)

        // when
        dataServicesRpcFacade.tryCreateMetadata(
            DocumentReference(DOC_REF),
            metadata,
        )

        // then
        val request = slot.captured.first()
        request.method shouldBe InternalOperationConstants.CREATE_METADATA_OPERATION
        request.params.findValue(JsonRpcRequestParams.DOCUMENT_REFERENCE) shouldBe objectMapper.valueToTree(
            DocumentReference(
                DOC_REF,
            ),
        )
        request.params.findValue(JsonRpcRequestParams.METADATA) shouldBe objectMapper.valueToTree(metadata)
    }

    @Test
    fun `should successfully invoke GET_PROCESS_VARIABLE_UPDATES operation`() {
        // given
        val documentReferences = listOf(DocumentReference(DOC_REF), DocumentReference("$DOCUMENT_MODEL_NAME/2"))

        // when
        dataServicesRpcFacade.tryGetProcessVariableUpdates(
            documentReferences,
        )

        // then
        val request = slot.captured.first()
        request.method shouldBe InternalOperationConstants.GET_PROCESS_VARIABLE_UPDATES_OPERATION
        request.params.findValue(JsonRpcRequestParams.DOCUMENT_REFERENCE_LIST) shouldBe objectMapper.valueToTree(documentReferences)
    }

    @Test
    fun `should successfully invoke SET_STATUS operation`() {
        // when
        dataServicesRpcFacade.trySetStatus(
            DocumentReference(DOC_REF),
            documentPart.path,
            documentPart.repetitions,
            documentPart.value,
        )

        // then
        val request = slot.captured.first()
        request.method shouldBe InternalOperationConstants.SET_STATUS_OPERATION
        request.params.findValue(JsonRpcRequestParams.DOCUMENT_REFERENCE) shouldBe objectMapper.valueToTree(
            DocumentReference(
                DOC_REF,
            ),
        )
        request.params.findValue(JsonRpcRequestParams.DOCUMENT_PARTS) shouldBe objectMapper.valueToTree(documentPart)
    }

    @Test
    fun `should successfully invoke CREATE_DOCUMENT operation`() {
        // when
        dataServicesRpcFacade.tryCreateDocument(document, DOCUMENT_MODEL_NAME)

        // then
        val request = slot.captured.first()
        request.method shouldBe InternalOperationConstants.CREATE_DOCUMENT_OPERATION
        request.params.findValue(JsonRpcRequestParams.DOCUMENT_MODEL_NAME).textValue() shouldBe DOCUMENT_MODEL_NAME
        request.params.findValue(JsonRpcRequestParams.DOCUMENT) shouldBe document
    }
}
