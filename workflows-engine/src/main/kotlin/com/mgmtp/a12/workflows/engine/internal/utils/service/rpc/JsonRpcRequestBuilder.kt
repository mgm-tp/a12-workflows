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

import com.mgmtp.a12.dataservices.client.rpc.RequestBuilderFactory
import com.mgmtp.a12.dataservices.client.rpc.RpcOperationsClient
import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.dataservices.document.operation.CoreOperationConstants
import com.mgmtp.a12.dataservices.query.Paging
import com.mgmtp.a12.dataservices.query.constraint.matching.ExactMatchOperator
import com.mgmtp.a12.dataservices.query.topology.QueryLink
import com.mgmtp.a12.dataservices.query.topology.QueryRoot
import com.mgmtp.a12.dataservices.relationship.spec.LinkDescriptor
import com.mgmtp.a12.dataservices.relationship.spec.RelationshipLinkSpec
import com.mgmtp.a12.dataservices.rpc.JsonRpc2Request
import com.mgmtp.a12.dataservices.rpc.JsonRpc2Response
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.data.JsonRpcRequestIds
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.data.JsonRpcRequestIds.ADD_DOCUMENT_OPERATION_ID
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.data.JsonRpcRequestIds.ADD_LINK_OPERATION_ID
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.data.JsonRpcRequestIds.LIST_LINKS_OPERATION_ID
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.data.JsonRpcRequestParams.DOCUMENT
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.data.JsonRpcRequestParams.DOCUMENT_MODEL_NAME
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.data.JsonRpcRequestParams.LINK_DESCRIPTOR
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.data.JsonRpcRequestParams.LINK_REFERENCE
import com.mgmtp.a12.workflows.shared.internal.constants.JsonRpcRequestQueryParams
import com.mgmtp.a12.workflows.shared.internal.constants.QueryConstants
import com.mgmtp.a12.workflows.shared.logging.utils.logger

class JsonRpcRequestBody(val method: String, val operationId: String, val parameters: Map<String, Any>)
data class RoleDocDescriptor(val relationshipModel: String, val role: String, val docRef: DocumentReference)
data class QueryLinkProperties(val queryDocumentModelName: String, val relationshipModelName: String, val targetRole: String, val sourceDocRef: DocumentReference)

class JsonRpcRequestBuilder(
    private val rpcOperationsClient: RpcOperationsClient,
    private val dsRequestBuilderFactory: RequestBuilderFactory,
) {

    private val jsonRpcRequestsList: MutableList<JsonRpcRequestBody> = mutableListOf()

    fun execute(): List<JsonRpc2Response> {
        val requests = buildRequests()
        logger().debug("Executing following JSON RPC requests: {}", requests)
        return rpcOperationsClient.invoke(requests)
    }

    fun buildDeleteLinkRequest(
        linkRef: RelationshipLinkSpec,
        operationId: String = JsonRpcRequestIds.DELETE_LINK_OPERATION_ID,
    ) = apply {
        jsonRpcRequestsList.add(
            JsonRpcRequestBody(
                CoreOperationConstants.DELETE_LINK_OPERATION,
                operationId,
                mapOf(LINK_REFERENCE to linkRef),
            ),
        )
    }

    fun buildRelinkDocumentRequest(
        linkRef: String,
        operationId: String = JsonRpcRequestIds.RELINK_DOCUMENT_OPERATION_ID,
        linkDescriptor: LinkDescriptor,
    ) = apply {
        jsonRpcRequestsList.add(
            JsonRpcRequestBody(
                CoreOperationConstants.RELINK_DOCUMENT_OPERATION,
                operationId,
                mapOf(
                    LINK_REFERENCE to linkRef,
                    LINK_DESCRIPTOR to linkDescriptor,
                ),
            ),
        )
    }

    fun buildListLinksRequest(
        queryLinkProperties: QueryLinkProperties,
        operationId: String = LIST_LINKS_OPERATION_ID,
    ): JsonRpcRequestBuilder = apply {
        val (queryDocumentModelName, relationShipModelName, targetRole, sourceDocRef) = queryLinkProperties

        val queryLink = QueryLink.builder()
            .relationshipModel(relationShipModelName)
            .targetRole(targetRole)
            // max depth is needed for recursive relationships
            .maxDepth(1)
            .build()
        val query = QueryRoot.builder()
            // targetDocumentModel does not mean that it's the targetDocumentModel of the modeled Relationship
            // It's rather the DM name of the documents that should be queried.
            .targetDocumentModel(queryDocumentModelName)
            .projectionName(QueryConstants.PROJECTION_NAME_DOCUMENT)
            .link(queryLink)
            .constraint(
                ExactMatchOperator<String>()
                    .apply {
                        field = QueryConstants.DATA_SERVICES_METADATA_DOC_REF
                        value = sourceDocRef.toString()
                    },
            )
            .paging(
                Paging.builder()
                    .pageNumber(0)
                    .pageSize(100)
                    .build(),
            )
            .build()

        jsonRpcRequestsList.add(
            JsonRpcRequestBody(
                CoreOperationConstants.QUERY_OPERATION,
                operationId,
                mapOf(JsonRpcRequestQueryParams.QUERY to query),
            ),
        )
    }

    fun buildAddLinkRequest(
        linkDescriptor: LinkDescriptor,
        operationId: String = ADD_LINK_OPERATION_ID,
    ): JsonRpcRequestBuilder = apply {
        jsonRpcRequestsList.add(
            JsonRpcRequestBody(
                CoreOperationConstants.ADD_LINK_OPERATION,
                operationId,
                mapOf(LINK_DESCRIPTOR to linkDescriptor),
            ),
        )
    }

    fun buildAddDocumentRequest(
        documentModelName: String,
        document: Any,
        operationId: String = ADD_DOCUMENT_OPERATION_ID,
    ): JsonRpcRequestBuilder = apply {
        jsonRpcRequestsList.add(
            JsonRpcRequestBody(
                CoreOperationConstants.ADD_DOCUMENT_OPERATION,
                operationId,
                mapOf(DOCUMENT_MODEL_NAME to documentModelName, DOCUMENT to document),
            ),
        )
    }

    fun buildRequests(): List<JsonRpc2Request> {
        val newJsonRpc2Request = dsRequestBuilderFactory.newJsonRpc2RequestBuilder()

        jsonRpcRequestsList.forEach {
            val request = newJsonRpc2Request
                .addMethodCall(it.method)
                .id(it.operationId)
            it.parameters.forEach(request::putParameter)
            request.build()
        }
        return newJsonRpc2Request.build()
    }
}
