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
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mgmtp.a12.dataservices.client.rpc.RequestBuilderFactory
import com.mgmtp.a12.dataservices.client.rpc.RpcOperationsClient
import com.mgmtp.a12.dataservices.rpc.JsonRpc2Response
import com.mgmtp.a12.dataservices.rpc.JsonRpc2ResponseError
import com.mgmtp.a12.workflows.shared.functions.testUtils.readResource
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClientException
import org.springframework.web.server.ResponseStatusException

private class DataServicesRpcUtilTest {

    val rpcOperationsClient: RpcOperationsClient = mockk(relaxed = true)
    val objectMapper = jacksonObjectMapper()
    val dsRequestBuilderFactory: RequestBuilderFactory = RequestBuilderFactory(objectMapper)

    val jsonRpcRequestBuilder = JsonRpcRequestBuilder(
        rpcOperationsClient,
        dsRequestBuilderFactory,
    )

    companion object {
        const val DOCUMENT_MODEL_NAME = "SimplePerson_DM"
        const val ERROR_MESSAGE = "someErrorMessage"
        val document = readResource<JsonNode>(this::class, "/documents/someDocumentWithoutMetadata_SimplePerson_DM.json")
    }

    @Test
    fun `should successfully return rpc response when result is positive`() {
        // given
        val response = JsonRpc2Response(TextNode("somePositiveResult"))

        // when / then
        shouldNotThrowAny { trySingleRpcOrFail { response } }
    }

    @Test
    fun `should throw ResponseStatusException when response contains error`() {
        // given
        val error = JsonRpc2ResponseError(
            JsonRpc2ResponseError.INTERNAL_ERROR,
            ERROR_MESSAGE,
            objectMapper.nodeFactory.textNode("someErrorNode"),
        )
        val response = JsonRpc2Response(error)

        // when / then
        shouldThrow<ResponseStatusException> { trySingleRpcOrFail { response } }
    }

    @Test
    fun `should throw RestClientException when Data Services could not be reached`() {
        // given
        val request = jsonRpcRequestBuilder.buildAddDocumentRequest(
            DOCUMENT_MODEL_NAME,
            document,
        ).buildRequests()
        every { rpcOperationsClient.invoke(request) } throws RestClientException(ERROR_MESSAGE)

        // when / then
        shouldThrow<RestClientException> { trySingleRpcOrFail { rpcOperationsClient.invoke(request).single() } }
    }
}
