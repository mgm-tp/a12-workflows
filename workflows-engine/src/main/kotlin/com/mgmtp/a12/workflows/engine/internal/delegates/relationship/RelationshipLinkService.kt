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

package com.mgmtp.a12.workflows.engine.internal.delegates.relationship

import com.mgmtp.a12.dataservices.relationship.spec.LinkDescriptor
import com.mgmtp.a12.dataservices.relationship.spec.RelationshipLinkSpec
import com.mgmtp.a12.dataservices.rpc.JsonRpc2Response
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.JsonRpcRequestBuilder
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.JsonRpcRequestBuilderFactory
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException

data class JsonRpcException(val errorMessage: String) : RuntimeException()

data class LinkLimitReachedException(val errorMessage: String) : RuntimeException()

@Service("workflowsRelationshipLinkService")
class RelationshipLinkService(
    private val rpcRequestBuilderFactory: JsonRpcRequestBuilderFactory,
) {
    fun createLink(linkDescriptor: LinkDescriptor): String {
        logger().info("Creating Link [linkDescriptor={}]", linkDescriptor)

        return executeCreateLinkRequest(linkDescriptor).first { it.isSuccess }
            .result.get("id")
            .textValue()
    }

    fun relinkDocument(relinkDocumentInfo: RelinkDocumentInfo): String = executeRelinkDocumentRequest(relinkDocumentInfo).first { it.isSuccess }
        .result.get("id")
        .textValue()

    fun deleteLink(relationshipLinkSpec: RelationshipLinkSpec) {
        logger().info("Deleting link [id=${relationshipLinkSpec.id}]")
        val rpcRequest = deleteLinkRpcRequest(relationshipLinkSpec)
        sendLinkBatchRequest(rpcRequest)
    }

    fun executeRelinkDocumentRequest(relinkDocumentInfo: RelinkDocumentInfo): List<JsonRpc2Response> {
        val rpcRequest = relinkDocumentRpcRequest(
            relinkDocumentInfo.linkRef,
            relinkDocumentInfo.linkDescriptor,
        )
        return sendLinkBatchRequest(rpcRequest)
    }

    fun executeCreateLinkRequest(linkDescriptor: LinkDescriptor): List<JsonRpc2Response> {
        val rpcRequest = createLinkRpcRequest(linkDescriptor)
        return sendLinkBatchRequest(rpcRequest)
    }

    private fun sendLinkBatchRequest(request: JsonRpcRequestBuilder): List<JsonRpc2Response> {
        try {
            val response = request.execute()
            logAndThrowIfAnyError(response)
            return response
        } catch (e: RestClientException) {
            logger().error("Error occurred when creating relationship: ${e.message}")
            throw e
        }
    }

    private fun logAndThrowIfAnyError(response: List<JsonRpc2Response>) {
        response.filter { !it.isSuccess }.forEach {
            logger().debug("Error occurred when creating relationship: {}, {}", it.error.message, it.error.data)

            if (it.error.message.contains("Limit Reached")) {
                throw LinkLimitReachedException(it.error.message)
            } else {
                throw JsonRpcException("Error occurred when creating relationship")
            }
        }
    }

    private fun relinkDocumentRpcRequest(linkRef: String, linkDescriptor: LinkDescriptor): JsonRpcRequestBuilder = rpcRequestBuilderFactory.getJsonRpcRequestBuilder()
        .buildRelinkDocumentRequest(linkRef = linkRef, linkDescriptor = linkDescriptor)

    private fun deleteLinkRpcRequest(relationshipLinkSpec: RelationshipLinkSpec): JsonRpcRequestBuilder = rpcRequestBuilderFactory.getJsonRpcRequestBuilder().buildDeleteLinkRequest(relationshipLinkSpec)

    private fun createLinkRpcRequest(linkDescriptor: LinkDescriptor): JsonRpcRequestBuilder = rpcRequestBuilderFactory.getJsonRpcRequestBuilder().buildAddLinkRequest(linkDescriptor)
}
