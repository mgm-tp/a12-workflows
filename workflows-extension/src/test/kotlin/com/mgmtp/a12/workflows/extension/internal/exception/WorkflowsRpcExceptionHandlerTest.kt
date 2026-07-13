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

package com.mgmtp.a12.workflows.extension.internal.exception

import com.mgmtp.a12.contentstore.client.exception.A12ClientException
import com.mgmtp.a12.dataservices.common.exception.NotFoundException
import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.dataservices.exception.ExceptionKeys.RPC_OPERATION_ERROR_KEY
import com.mgmtp.a12.dataservices.rpc.RpcException
import com.mgmtp.a12.dataservices.rpc.RpcExceptionSupport
import com.mgmtp.a12.model.repository.ResourceAccessException
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.CREATE_DOCUMENT_OPERATION
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.CREATE_METADATA_OPERATION
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.GET_PROCESS_VARIABLE_UPDATES_OPERATION
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.REMOVE_METADATA_OPERATION
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.SET_STATUS_OPERATION
import com.mgmtp.a12.workflows.client.operation.WorkflowsOperationConstants.START_PROCESS_OPERATION
import com.mgmtp.a12.workflows.client.operation.WorkflowsOperationConstants.TASK_ASSIGN_OPERATION
import com.mgmtp.a12.workflows.client.operation.WorkflowsOperationConstants.TASK_COMPLETE_OPERATION
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.CREATE_DOCUMENT_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.CREATE_METADATA_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.GET_PROCESS_VARIABLE_UPDATES_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.PROCESS_ENGINE_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.REMOVE_METADATA_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.SET_STATUS_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.START_PROCESS_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.TASK_ASSIGN_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.TASK_COMPLETE_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.testFixtures.TestResources.Names.DOCUMENT_MODEL_NAME_WITH_METADATA
import com.mgmtp.a12.workflows.shared.functions.testUtils.readResource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientException
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.stream.Stream

private class WorkflowsRpcExceptionHandlerTest {

    // Subject under test
    val exceptionHandler = WorkflowsRpcExceptionHandler(jacksonObjectMapper())

    companion object {
        const val PROCESS_DEFINITION = "SomeProcessDefinition"
        const val ERROR_MESSAGE = "someErrorMessage"

        val docRef = DocumentReference("$DOCUMENT_MODEL_NAME_WITH_METADATA/1")
        val documentWithMetadataNode = readResource<JsonNode>(this::class, "/documents/validDocument.json")

        val rpcException = RpcExceptionSupport.createException(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            RPC_OPERATION_ERROR_KEY,
            "RPC Exception that should be rethrown without further mapping or wrapping.",
            ERROR_MESSAGE,
            START_PROCESS_OPERATION,
        )
        val restClientException = RestClientException(ERROR_MESSAGE)
        val a12ClientException: A12ClientException = mockk(relaxed = true)
        val resourceAccessException = ResourceAccessException("someResourceName", ERROR_MESSAGE)
        val notFoundException = NotFoundException(ERROR_MESSAGE)
        val runtimeException = RuntimeException(ERROR_MESSAGE)

        @JvmStatic
        fun provideArgumentsForParameterizedTest(): Stream<Arguments> = Stream.of(
            Arguments.of(START_PROCESS_OPERATION, START_PROCESS_ERROR_TITLE, "RestClientException", WorkflowsRpcException(restClientException, START_PROCESS_OPERATION, processDefinition = PROCESS_DEFINITION)),
            Arguments.of(START_PROCESS_OPERATION, START_PROCESS_ERROR_TITLE, "A12ClientException", WorkflowsRpcException(a12ClientException, START_PROCESS_OPERATION, processDefinition = PROCESS_DEFINITION)),
            Arguments.of(START_PROCESS_OPERATION, PROCESS_ENGINE_ERROR_TITLE, "ResourceAccessException", WorkflowsRpcException(resourceAccessException, START_PROCESS_OPERATION, processDefinition = PROCESS_DEFINITION)),

            Arguments.of(TASK_ASSIGN_OPERATION, TASK_ASSIGN_ERROR_TITLE, "NotFoundException", WorkflowsRpcException(notFoundException, TASK_ASSIGN_OPERATION, docRef = docRef)),
            Arguments.of(TASK_ASSIGN_OPERATION, TASK_ASSIGN_ERROR_TITLE, "RuntimeException", WorkflowsRpcException(runtimeException, TASK_ASSIGN_OPERATION, docRef = docRef)),

            Arguments.of(TASK_COMPLETE_OPERATION, TASK_COMPLETE_ERROR_TITLE, "A12ClientException", WorkflowsRpcException(a12ClientException, TASK_COMPLETE_OPERATION, docRef = docRef)),
            Arguments.of(TASK_COMPLETE_OPERATION, TASK_COMPLETE_ERROR_TITLE, "NotFoundException", WorkflowsRpcException(notFoundException, TASK_COMPLETE_OPERATION, docRef = docRef)),

            Arguments.of(CREATE_DOCUMENT_OPERATION, CREATE_DOCUMENT_ERROR_TITLE, "RuntimeException", WorkflowsRpcException(runtimeException, CREATE_DOCUMENT_OPERATION, documentNode = documentWithMetadataNode)),

            Arguments.of(CREATE_METADATA_OPERATION, CREATE_METADATA_ERROR_TITLE, "A12ClientException", WorkflowsRpcException(a12ClientException, CREATE_METADATA_OPERATION, docRef = docRef)),

            Arguments.of(REMOVE_METADATA_OPERATION, REMOVE_METADATA_ERROR_TITLE, "RuntimeException", WorkflowsRpcException(runtimeException, REMOVE_METADATA_OPERATION, docRef = docRef)),

            Arguments.of(GET_PROCESS_VARIABLE_UPDATES_OPERATION, GET_PROCESS_VARIABLE_UPDATES_ERROR_TITLE, "A12ClientException", WorkflowsRpcException(a12ClientException, GET_PROCESS_VARIABLE_UPDATES_OPERATION)),

            Arguments.of(SET_STATUS_OPERATION, SET_STATUS_ERROR_TITLE, "RuntimeException", WorkflowsRpcException(runtimeException, SET_STATUS_OPERATION, docRef = docRef)),
        )
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForParameterizedTest")
    fun `should throw RpcException with correct mapping regarding operationId, errorTitle and innerException`(expectedOperationId: String, expectedBaseErrorTitle: String, expectedInnerExceptionName: String, workflowsRpcException: WorkflowsRpcException) {
        // when
        val thrownRpcException = shouldThrow<RpcException> { exceptionHandler.handleWorkflowsRpcException(workflowsRpcException) }

        // then
        thrownRpcException.message shouldContain expectedBaseErrorTitle
        thrownRpcException.message shouldContain expectedInnerExceptionName
        thrownRpcException.operationError.operationId shouldBe expectedOperationId
    }

    @Test
    fun `should rethrow RpcException without further mapping and wrapping`() {
        // given
        val workflowsRpcException = WorkflowsRpcException(rpcException, START_PROCESS_OPERATION, processDefinition = PROCESS_DEFINITION)

        // when
        val rethrownRpcException = shouldThrow<RpcException> { exceptionHandler.handleWorkflowsRpcException(workflowsRpcException) }

        // then
        rethrownRpcException shouldBe rpcException
    }

    @Test
    fun `should throw RpcException with correct mapping for not handled inner exception`() {
        // given
        val someUnmappedException = SomeUnmappedException(Exception(ERROR_MESSAGE))
        val workflowsRpcException = WorkflowsRpcException(someUnmappedException, TASK_ASSIGN_OPERATION, docRef = docRef)
        val expectedRpcException = RpcExceptionSupport.createException(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            RPC_OPERATION_ERROR_KEY,
            "$TASK_ASSIGN_ERROR_TITLE. Cause: $someUnmappedException",
            someUnmappedException.message,
            TASK_ASSIGN_OPERATION,
        )

        // when
        val thrownRpcException = shouldThrow<RpcException> { exceptionHandler.handleWorkflowsRpcException(workflowsRpcException) }

        // then
        thrownRpcException shouldBe expectedRpcException
    }

    data class SomeUnmappedException(
        override val cause: Exception,
    ) : Exception(cause)
}
