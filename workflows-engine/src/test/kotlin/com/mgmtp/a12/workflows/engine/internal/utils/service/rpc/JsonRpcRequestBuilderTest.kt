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

package com.mgmtp.a12.workflows.engine.internal.utils.service.rpc

import com.mgmtp.a12.dataservices.client.rpc.RequestBuilderFactory
import com.mgmtp.a12.dataservices.client.rpc.RpcOperationsClient
import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.dataservices.document.operation.CoreOperationConstants
import com.mgmtp.a12.dataservices.relationship.spec.LinkDescriptor
import com.mgmtp.a12.dataservices.relationship.spec.RelationshipLinkSpec
import com.mgmtp.a12.dataservices.relationship.spec.RelationshipRoleSpec
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.data.JsonRpcRequestIds
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.data.JsonRpcRequestParams
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.data.JsonRpcRequestParams.DOCUMENT
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.data.JsonRpcRequestParams.LINK_DESCRIPTOR
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.data.JsonRpcRequestParams.LINK_REFERENCE
import com.mgmtp.a12.workflows.shared.functions.testUtils.readResource
import com.mgmtp.a12.workflows.shared.internal.constants.JsonRpcRequestQueryParams
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper

private class JsonRpcRequestBuilderTest {

    val mockRpcOperationsClient: RpcOperationsClient = mockk(relaxed = true)
    val objectMapper = jacksonObjectMapper()
    val dsRequestBuilderFactory: RequestBuilderFactory = RequestBuilderFactory(objectMapper)

    val jsonRpcRequestBuilder = JsonRpcRequestBuilder(
        mockRpcOperationsClient,
        dsRequestBuilderFactory,
    )

    companion object {
        const val DOCUMENT_MODEL_NAME = "SimplePerson_DM"
        val docRef = DocumentReference("$DOCUMENT_MODEL_NAME/1")

        val document = readResource<JsonNode>(this::class, "/documents/someDocumentWithoutMetadata_SimplePerson_DM.json")

        const val CUSTOM_OPERATION_ID = "someCustomOperationId"

        const val RELATIONSHIP_MODEL_NAME = "someRelationshipModel"

        const val TARGET_ROLE_NAME = "someTargetRole"
        const val SOURCE_ROLE_NAME = "someSourceRole"
        val targetRoleSpec = RelationshipRoleSpec(TARGET_ROLE_NAME, docRef)
        val sourceRoleSpec = RelationshipRoleSpec(SOURCE_ROLE_NAME, docRef)

        val entities = listOf(targetRoleSpec, sourceRoleSpec)
        val linkDescriptor = LinkDescriptor(RELATIONSHIP_MODEL_NAME, entities)
        val linkRef = RelationshipLinkSpec(linkDescriptor, "someId")
    }

    @Test
    fun `should successfully build DELETE_LINK requests`() {
        // when
        val request = jsonRpcRequestBuilder.buildDeleteLinkRequest(
            linkRef,
        ).buildRequests().first()
        val secondRequest = jsonRpcRequestBuilder.buildDeleteLinkRequest(
            linkRef,
            CUSTOM_OPERATION_ID,
        ).buildRequests().last()

        // then
        request.method shouldBe CoreOperationConstants.DELETE_LINK_OPERATION
        request.id shouldBe JsonRpcRequestIds.DELETE_LINK_OPERATION_ID
        request.params.findValue(LINK_REFERENCE) shouldBe objectMapper.valueToTree(linkRef)
        secondRequest.id shouldBe CUSTOM_OPERATION_ID
    }

    @Test
    fun `should successfully build RELINK_DOCUMENT requests`() {
        // given
        val linkRefString = "someLinkRef"

        // when
        val request = jsonRpcRequestBuilder.buildRelinkDocumentRequest(
            linkRef = linkRefString,
            linkDescriptor = linkDescriptor,
        ).buildRequests().first()
        val secondRequest = jsonRpcRequestBuilder.buildRelinkDocumentRequest(
            linkRef = linkRefString,
            CUSTOM_OPERATION_ID,
            linkDescriptor = linkDescriptor,
        ).buildRequests().last()

        // then
        request.method shouldBe CoreOperationConstants.RELINK_DOCUMENT_OPERATION
        request.id shouldBe JsonRpcRequestIds.RELINK_DOCUMENT_OPERATION_ID
        request.params.findValue(LINK_REFERENCE).textValue() shouldBe linkRefString
        request.params.findValue(LINK_DESCRIPTOR) shouldBe objectMapper.valueToTree(linkDescriptor)
        secondRequest.id shouldBe CUSTOM_OPERATION_ID
    }

    @Test
    fun `should successfully build LIST_LINKS requests`() {
        // given
        val queryLinkProperties = QueryLinkProperties(DOCUMENT_MODEL_NAME, RELATIONSHIP_MODEL_NAME, TARGET_ROLE_NAME, docRef)

        // when
        val request = jsonRpcRequestBuilder.buildListLinksRequest(
            queryLinkProperties,
        ).buildRequests().first()
        val secondRequest = jsonRpcRequestBuilder.buildListLinksRequest(
            queryLinkProperties,
            CUSTOM_OPERATION_ID,
        ).buildRequests().last()

        // then
        request.method shouldBe CoreOperationConstants.QUERY_OPERATION
        request.id shouldBe JsonRpcRequestIds.LIST_LINKS_OPERATION_ID

        val actualQuery = request.params.findValue(JsonRpcRequestQueryParams.QUERY)
        actualQuery.findValue("targetDocumentModel").textValue() shouldBe queryLinkProperties.queryDocumentModelName
        actualQuery.findValue("relationshipModel").textValue() shouldBe queryLinkProperties.relationshipModelName
        actualQuery.findValue("targetRole").textValue() shouldBe queryLinkProperties.targetRole
        actualQuery.findValue("constraint").findValue("value").textValue() shouldBe queryLinkProperties.sourceDocRef.toString()

        secondRequest.id shouldBe CUSTOM_OPERATION_ID
    }

    @Test
    fun `should successfully build ADD_LINK requests`() {
        // when
        val request = jsonRpcRequestBuilder.buildAddLinkRequest(
            linkDescriptor,
        ).buildRequests().first()
        val secondRequest = jsonRpcRequestBuilder.buildAddLinkRequest(
            linkDescriptor,
            CUSTOM_OPERATION_ID,
        ).buildRequests().last()

        // then
        request.method shouldBe CoreOperationConstants.ADD_LINK_OPERATION
        request.id shouldBe JsonRpcRequestIds.ADD_LINK_OPERATION_ID
        request.params.findValue(LINK_DESCRIPTOR) shouldBe objectMapper.valueToTree(linkDescriptor)
        secondRequest.id shouldBe CUSTOM_OPERATION_ID
    }

    @Test
    fun `should successfully build ADD_DOCUMENT requests`() {
        // when
        val request = jsonRpcRequestBuilder.buildAddDocumentRequest(
            DOCUMENT_MODEL_NAME,
            document,
        ).buildRequests().first()
        val secondRequest = jsonRpcRequestBuilder.buildAddDocumentRequest(
            DOCUMENT_MODEL_NAME,
            document,
            CUSTOM_OPERATION_ID,
        ).buildRequests().last()

        // then
        request.method shouldBe CoreOperationConstants.ADD_DOCUMENT_OPERATION
        request.id shouldBe JsonRpcRequestIds.ADD_DOCUMENT_OPERATION_ID
        request.params.findValue(JsonRpcRequestParams.DOCUMENT_MODEL_NAME).textValue() shouldBe DOCUMENT_MODEL_NAME
        request.params.findValue(DOCUMENT) shouldBe document
        secondRequest.id shouldBe CUSTOM_OPERATION_ID
    }
}
