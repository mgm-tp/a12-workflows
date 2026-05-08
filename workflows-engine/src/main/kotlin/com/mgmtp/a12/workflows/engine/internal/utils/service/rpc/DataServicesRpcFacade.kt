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
import com.mgmtp.a12.dataservices.client.rpc.RequestBuilderFactory
import com.mgmtp.a12.dataservices.client.rpc.RpcOperationsClient
import com.mgmtp.a12.dataservices.document.DocumentPart
import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.dataservices.document.operation.CoreOperationConstants
import com.mgmtp.a12.dataservices.rpc.JsonRpc2Response
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.data.JsonRpcRequestIds
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.data.JsonRpcRequestParams
import com.mgmtp.a12.workflows.shared.dtos.ProcessMetadata
import org.springframework.stereotype.Component

@Component
class DataServicesRpcFacade(
    private val rpcOperationsClient: RpcOperationsClient,
    private val dsRequestBuilderFactory: RequestBuilderFactory,
) {

    fun tryCreateDocument(newDocument: JsonNode, docModelName: String): DocumentReference {
        val response = trySingleRpcOrFail { invokeCreateDocument(docModelName, newDocument) }.result
        val docRef = response.get("docRef").textValue()

        return DocumentReference(docRef)
    }

    fun tryAddDocument(newDocument: JsonNode, docModelName: String): DocumentReference {
        val response = trySingleRpcOrFail { invokeAddDocument(docModelName, newDocument) }.result
        val docRef = response.get("docRef").textValue()

        return DocumentReference(docRef)
    }

    fun tryCreateMetadata(docRef: DocumentReference?, metadata: ProcessMetadata): JsonRpc2Response = trySingleRpcOrFail { invokeCreateMetadata(docRef, metadata) }

    fun tryRemoveMetadata(docRef: DocumentReference): JsonRpc2Response = trySingleRpcOrFail { invokeRemoveMetadata(docRef) }

    fun tryGetProcessVariableUpdates(documentReferences: List<DocumentReference>): JsonRpc2Response = trySingleRpcOrFail { invokeGetProcessVariableUpdates(documentReferences) }

    fun trySetStatus(
        docRef: DocumentReference,
        path: String,
        repetitions: IntArray,
        newValue: Any,
    ): JsonRpc2Response = trySingleRpcOrFail { invokeSetStatus(docRef, path, repetitions, newValue) }

    private fun invokeCreateDocument(
        documentModelName: String,
        document: Any,
        operationId: String = JsonRpcRequestIds.CREATE_DOCUMENT_ID,
    ): JsonRpc2Response = rpcOperationsClient
        .invoke(
            listOf(
                dsRequestBuilderFactory
                    .newJsonRpc2RequestBuilder()
                    .addMethodCall(InternalOperationConstants.CREATE_DOCUMENT_OPERATION)
                    .id(operationId)
                    .putParameter(JsonRpcRequestParams.DOCUMENT_MODEL_NAME, documentModelName)
                    .putParameter(JsonRpcRequestParams.DOCUMENT, document)
                    .build(),
            ),
        )
        .single()

    private fun invokeAddDocument(
        documentModelName: String,
        document: Any,
        operationId: String = JsonRpcRequestIds.ADD_DOCUMENT_OPERATION_ID,
    ): JsonRpc2Response = rpcOperationsClient
        .invoke(
            listOf(
                dsRequestBuilderFactory
                    .newJsonRpc2RequestBuilder()
                    .addMethodCall(CoreOperationConstants.ADD_DOCUMENT_OPERATION)
                    .id(operationId)
                    .putParameter(JsonRpcRequestParams.DOCUMENT_MODEL_NAME, documentModelName)
                    .putParameter(JsonRpcRequestParams.DOCUMENT, document)
                    .build(),
            ),
        )
        .single()

    private fun invokeCreateMetadata(
        docRef: DocumentReference?,
        metadata: ProcessMetadata,
        operationId: String = JsonRpcRequestIds.CREATE_METADATA_ID,
    ): JsonRpc2Response = rpcOperationsClient
        .invoke(
            listOf(
                dsRequestBuilderFactory
                    .newJsonRpc2RequestBuilder()
                    .addMethodCall(InternalOperationConstants.CREATE_METADATA_OPERATION)
                    .id(operationId)
                    .putParameter(JsonRpcRequestParams.DOCUMENT_REFERENCE, docRef)
                    .putParameter(JsonRpcRequestParams.METADATA, metadata)
                    .build(),
            ),
        )
        .single()

    private fun invokeRemoveMetadata(
        docRef: DocumentReference,
        operationId: String = JsonRpcRequestIds.REMOVE_METADATA_ID,
    ): JsonRpc2Response = rpcOperationsClient
        .invoke(
            listOf(
                dsRequestBuilderFactory
                    .newJsonRpc2RequestBuilder()
                    .addMethodCall(InternalOperationConstants.REMOVE_METADATA_OPERATION)
                    .id(operationId)
                    .putParameter(JsonRpcRequestParams.DOCUMENT_REFERENCE, docRef)
                    .build(),
            ),
        )
        .single()

    private fun invokeGetProcessVariableUpdates(
        documentReferences: List<DocumentReference>,
        operationId: String = JsonRpcRequestIds.GET_PROCESS_VARIABLE_UPDATES_ID,
    ): JsonRpc2Response = rpcOperationsClient
        .invoke(
            listOf(
                dsRequestBuilderFactory
                    .newJsonRpc2RequestBuilder()
                    .addMethodCall(InternalOperationConstants.GET_PROCESS_VARIABLE_UPDATES_OPERATION)
                    .id(operationId)
                    .putParameter(JsonRpcRequestParams.DOCUMENT_REFERENCE_LIST, documentReferences)
                    .build(),
            ),
        )
        .single()

    private fun invokeSetStatus(
        docRef: DocumentReference,
        path: String,
        repetitions: IntArray,
        newValue: Any,
        operationId: String = JsonRpcRequestIds.SET_STATUS_ID,
    ): JsonRpc2Response {
        val documentPart = DocumentPart(path, newValue, repetitions)

        return rpcOperationsClient
            .invoke(
                listOf(
                    dsRequestBuilderFactory
                        .newJsonRpc2RequestBuilder()
                        .addMethodCall(InternalOperationConstants.SET_STATUS_OPERATION)
                        .id(operationId)
                        .putParameter(JsonRpcRequestParams.DOCUMENT_REFERENCE, docRef)
                        .putParameter(JsonRpcRequestParams.DOCUMENT_PARTS, documentPart)
                        .build(),
                ),
            )
            .single()
    }
}
