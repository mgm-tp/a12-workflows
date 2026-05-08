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
package com.mgmtp.a12.workflows.engine.internal.delegates.relationship

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mgmtp.a12.dataservices.client.rpc.RequestBuilderFactory
import com.mgmtp.a12.dataservices.client.rpc.RpcOperationsClient
import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.dataservices.document.operation.CoreOperationConstants
import com.mgmtp.a12.dataservices.relationship.spec.LinkDescriptor
import com.mgmtp.a12.dataservices.relationship.spec.RelationshipRoleSpec
import com.mgmtp.a12.dataservices.rpc.JsonRpc2Request
import com.mgmtp.a12.workflows.engine.internal.data.LinkTestData
import com.mgmtp.a12.workflows.engine.internal.data.LinkTestData.SOURCE_DOC_ID
import com.mgmtp.a12.workflows.engine.internal.data.LinkTestData.SOURCE_DOC_MODEL
import com.mgmtp.a12.workflows.engine.internal.data.LinkTestData.SOURCE_ROLE
import com.mgmtp.a12.workflows.engine.internal.data.LinkTestData.TARGET_DOC_ID
import com.mgmtp.a12.workflows.engine.internal.data.LinkTestData.TARGET_DOC_MODEL
import com.mgmtp.a12.workflows.engine.internal.data.LinkTestData.TARGET_ROLE
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.JsonRpcRequestBuilderFactory
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.data.JsonRpcRequestIds
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class RelationshipLinkServiceTest {

    val mockkRpcOperationsClient: RpcOperationsClient = mockk()
    val requestBuilderFactory = RequestBuilderFactory(jacksonObjectMapper())
    val dataServicesRpcClientFactory: JsonRpcRequestBuilderFactory =
        JsonRpcRequestBuilderFactory(mockkRpcOperationsClient, requestBuilderFactory)
    val slot = slot<List<JsonRpc2Request>>()
    var relationshipLinkService = RelationshipLinkService(dataServicesRpcClientFactory)

    companion object {
        val linkEntitySource =
            RelationshipRoleSpec(SOURCE_ROLE, DocumentReference("$SOURCE_DOC_MODEL/$SOURCE_DOC_ID"))
        val linkEntityTarget =
            RelationshipRoleSpec(TARGET_ROLE, DocumentReference("$TARGET_DOC_MODEL/$TARGET_DOC_ID"))
        val linkDescriptor =
            LinkDescriptor(LinkTestData.RELATIONSHIP_MODEL, listOf(linkEntitySource, linkEntityTarget))
    }

    @BeforeEach
    fun setup() {
        every { mockkRpcOperationsClient.invoke(any()) } answers { listOf() }
    }

    @Test
    fun `should successfully build the RPC request`() {
        // when
        relationshipLinkService.executeCreateLinkRequest(linkDescriptor)

        // then
        verify(exactly = 1) { mockkRpcOperationsClient.invoke(capture(slot)) }
        val result: JsonRpc2Request = slot.captured[0]

        result shouldNotBe null

        result.id shouldBe JsonRpcRequestIds.ADD_LINK_OPERATION_ID
        result.method shouldBe CoreOperationConstants.ADD_LINK_OPERATION

        val sourceDocRef = linkEntitySource.docRef
        val targetDocRef = linkEntityTarget.docRef

        jacksonObjectMapper().writeValueAsString(result.params) shouldBe expectedRequestParameters(
            sourceDocRef.split("/")[0],
            sourceDocRef.toString(),
            targetDocRef.split("/")[0],
            targetDocRef.toString(),
        )
    }

    fun expectedRequestParameters(
        sourceDocModel: String?,
        sourceDocRef: String?,
        targetDocModel: String?,
        targetDocRef: String?,
    ): String = """
            {
                    "linkDescriptor": {
                        "relationshipModel": "${linkDescriptor.relationshipModel}",
                            "entities": [
                                {
                                    "role": "${linkEntitySource.role}",
                                    "modelName": "$sourceDocModel",
                                    "docRef": "$sourceDocRef"
                                 },
                                 {
                                    "role": "${linkEntityTarget.role}",
                                    "modelName": "$targetDocModel",
                                    "docRef": "$targetDocRef"
                                  }
                            ],
                         "predecessorLinkRef": null,
                         "position": "TOP"
                    }        
            }
    """.trimIndent().replace("\\s".toRegex(), "")
}
