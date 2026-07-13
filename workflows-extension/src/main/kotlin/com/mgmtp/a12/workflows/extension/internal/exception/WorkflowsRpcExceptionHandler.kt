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
import com.mgmtp.a12.contentstore.client.exception.RestErrorDetail
import com.mgmtp.a12.dataservices.common.exception.NotFoundException
import com.mgmtp.a12.dataservices.common.exception.NotFoundException.NOT_FOUND_EXCEPTION_CODE
import com.mgmtp.a12.dataservices.exception.ExceptionKeys.RPC_OPERATION_ERROR_KEY
import com.mgmtp.a12.dataservices.rpc.RpcException
import com.mgmtp.a12.dataservices.rpc.RpcExceptionSupport
import com.mgmtp.a12.model.repository.ResourceAccessException
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.CREATE_DOCUMENT_OPERATION
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.CREATE_METADATA_OPERATION
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.GET_PROCESS_VARIABLE_UPDATES_OPERATION
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.REMOVE_METADATA_OPERATION
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.SET_STATUS_OPERATION
import com.mgmtp.a12.workflows.client.operation.WorkflowsOperationConstants.SEND_MESSAGE_OPERATION
import com.mgmtp.a12.workflows.client.operation.WorkflowsOperationConstants.START_PROCESS_OPERATION
import com.mgmtp.a12.workflows.client.operation.WorkflowsOperationConstants.TASK_ASSIGN_OPERATION
import com.mgmtp.a12.workflows.client.operation.WorkflowsOperationConstants.TASK_COMPLETE_OPERATION
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.A12_CLIENT_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.COMMON_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.CREATE_DOCUMENT_OPERATION_COMMON_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.CREATE_DOCUMENT_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.CREATE_METADATA_OPERATION_COMMON_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.CREATE_METADATA_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.GET_PROCESS_VARIABLE_UPDATES_OPERATION_COMMON_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.GET_PROCESS_VARIABLE_UPDATES_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.REMOVE_METADATA_OPERATION_COMMON_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.REMOVE_METADATA_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.REST_CLIENT_ACCESS_DENIED_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.REST_CLIENT_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.SEND_MESSAGE_OPERATION_A12_CLIENT_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.SEND_MESSAGE_OPERATION_COMMON_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.SEND_MESSAGE_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.SEND_MESSAGE_OPERATION_REST_CLIENT_ACCESS_DENIED_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.SEND_MESSAGE_OPERATION_REST_CLIENT_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.SET_STATUS_OPERATION_COMMON_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.SET_STATUS_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.START_PROCESS_OPERATION_A12_CLIENT_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.START_PROCESS_OPERATION_COMMON_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.START_PROCESS_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.START_PROCESS_OPERATION_REST_CLIENT_ACCESS_DENIED_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.START_PROCESS_OPERATION_REST_CLIENT_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.TASK_ASSIGN_OPERATION_A12_CLIENT_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.TASK_ASSIGN_OPERATION_COMMON_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.TASK_ASSIGN_OPERATION_DOCUMENT_NOT_FOUND_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.TASK_ASSIGN_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.TASK_ASSIGN_OPERATION_REST_CLIENT_ACCESS_DENIED_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.TASK_ASSIGN_OPERATION_REST_CLIENT_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.TASK_COMPLETE_OPERATION_A12_CLIENT_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.TASK_COMPLETE_OPERATION_COMMON_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.TASK_COMPLETE_OPERATION_DOCUMENT_NOT_FOUND_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.TASK_COMPLETE_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.TASK_COMPLETE_OPERATION_REST_CLIENT_ACCESS_DENIED_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionCodes.TASK_COMPLETE_OPERATION_REST_CLIENT_EXCEPTION_CODE
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionKeys.ASSIGN_TASK_HTTP_CLIENT_EXCEPTION
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionKeys.COMPLETE_TASK_HTTP_CLIENT_EXCEPTION
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionKeys.PROCESS_ENGINE_ACCESS_EXCEPTION
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionKeys.SEND_MESSAGE_HTTP_CLIENT_EXCEPTION
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionKeys.START_PROCESS_HTTP_CLIENT_EXCEPTION
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionKeys.TASK_DOCUMENT_NOT_FOUND_EXCEPTION
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.CREATE_DOCUMENT_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.CREATE_METADATA_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.GET_PROCESS_VARIABLE_UPDATES_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.PROCESS_ENGINE_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.REMOVE_METADATA_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.SEND_MESSAGE_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.SET_STATUS_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.START_PROCESS_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.TASK_ASSIGN_ERROR_TITLE
import com.mgmtp.a12.workflows.extension.internal.exception.constants.WorkflowsExceptionTitleBase.TASK_COMPLETE_ERROR_TITLE
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.ObjectMapper
import kotlin.String

data class ProcessEngineExceptionResponse(val type: String, val message: String, val code: Int)

@Component
class WorkflowsRpcExceptionHandler(
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val ACCESS_DENIED_PREFIX = "Access denied exception occurred"
        private const val GENERIC_PREFIX = "Exception occurred"
    }

    fun handleWorkflowsRpcException(wfre: WorkflowsRpcException): RpcException {
        logger().debug("Handler is called with [WorkflowsRpcException={}].", wfre.toString())

        val rpcException = mapException(wfre)
        logger().debug("{} failed.", wfre.operationId)
        throw rpcException
    }

    private fun mapException(wfre: WorkflowsRpcException): RpcException = when (wfre.cause) {
        is RpcException -> wfre.cause
        is RestClientResponseException -> handleRestClientResponseException(wfre.cause, wfre.operationId)
        is A12ClientException -> handleA12ClientException(wfre, wfre.cause)
        is ResourceAccessException -> handleResourceAccessException(wfre)
        is NotFoundException -> handleNotFoundException(wfre)
        else -> handleException(wfre)
    }

    private fun handleRestClientResponseException(
        rcre: RestClientResponseException,
        operationId: String,
    ): RpcException {
        logger().debug("Handler is called with [RestClientResponseException={}].", rcre.message)

        val rpcExceptionKey = when (operationId) {
            START_PROCESS_OPERATION -> START_PROCESS_HTTP_CLIENT_EXCEPTION
            TASK_ASSIGN_OPERATION -> ASSIGN_TASK_HTTP_CLIENT_EXCEPTION
            TASK_COMPLETE_OPERATION -> COMPLETE_TASK_HTTP_CLIENT_EXCEPTION
            SEND_MESSAGE_OPERATION -> SEND_MESSAGE_HTTP_CLIENT_EXCEPTION
            else -> RPC_OPERATION_ERROR_KEY
        }

        val response = objectMapper
            .readValue(rcre.responseBodyAsString, ProcessEngineExceptionResponse::class.java)
            ?: throw createGenericException(rcre, rpcExceptionKey, operationId)

        return when (response.type) {
            "AccessDeniedException" -> createAccessDeniedException(rcre, rpcExceptionKey, operationId)
            else -> createGenericException(rcre, rpcExceptionKey, operationId)
        }
    }

    private fun createAccessDeniedException(
        rce: RestClientException,
        rpcExceptionKey: String,
        operationId: String,
    ): RpcException {
        val exceptionTitle = buildTitle(ACCESS_DENIED_PREFIX, rpcExceptionKey)
        val exceptionCode = when (operationId) {
            START_PROCESS_OPERATION -> START_PROCESS_OPERATION_REST_CLIENT_ACCESS_DENIED_EXCEPTION_CODE
            TASK_ASSIGN_OPERATION -> TASK_ASSIGN_OPERATION_REST_CLIENT_ACCESS_DENIED_EXCEPTION_CODE
            TASK_COMPLETE_OPERATION -> TASK_COMPLETE_OPERATION_REST_CLIENT_ACCESS_DENIED_EXCEPTION_CODE
            SEND_MESSAGE_OPERATION -> SEND_MESSAGE_OPERATION_REST_CLIENT_ACCESS_DENIED_EXCEPTION_CODE
            else -> REST_CLIENT_ACCESS_DENIED_EXCEPTION_CODE
        }
        return RpcExceptionSupport.createException(
            exceptionCode,
            "$rpcExceptionKey.access.denied",
            exceptionTitle,
            rce.message,
            operationId,
        )
    }

    private fun createGenericException(
        rce: RestClientException,
        rpcExceptionKey: String,
        operationId: String,
    ): RpcException {
        val exceptionTitle = buildTitle(GENERIC_PREFIX, rpcExceptionKey)
        val exceptionCode = when (operationId) {
            START_PROCESS_OPERATION -> START_PROCESS_OPERATION_REST_CLIENT_EXCEPTION_CODE
            TASK_ASSIGN_OPERATION -> TASK_ASSIGN_OPERATION_REST_CLIENT_EXCEPTION_CODE
            TASK_COMPLETE_OPERATION -> TASK_COMPLETE_OPERATION_REST_CLIENT_EXCEPTION_CODE
            SEND_MESSAGE_OPERATION -> SEND_MESSAGE_OPERATION_REST_CLIENT_EXCEPTION_CODE
            else -> REST_CLIENT_EXCEPTION_CODE
        }
        return RpcExceptionSupport.createException(
            exceptionCode,
            rpcExceptionKey,
            exceptionTitle,
            rce.message,
            operationId,
        )
    }

    private fun handleA12ClientException(wfre: WorkflowsRpcException, a12ce: A12ClientException): RpcException {
        val errorDetail = a12ce.errorDetail
        val errorCode = when (errorDetail) {
            is RestErrorDetail -> errorDetail.responseCode
            else -> when (wfre.operationId) {
                START_PROCESS_OPERATION -> START_PROCESS_OPERATION_A12_CLIENT_EXCEPTION_CODE
                TASK_ASSIGN_OPERATION -> TASK_ASSIGN_OPERATION_A12_CLIENT_EXCEPTION_CODE
                TASK_COMPLETE_OPERATION -> TASK_COMPLETE_OPERATION_A12_CLIENT_EXCEPTION_CODE
                SEND_MESSAGE_OPERATION -> SEND_MESSAGE_OPERATION_A12_CLIENT_EXCEPTION_CODE
                else -> A12_CLIENT_EXCEPTION_CODE
            }
        }
        val errorMessage = when (errorDetail) {
            is RestErrorDetail -> errorDetail.response.toString()
            else -> errorDetail.toString()
        }

        return RpcExceptionSupport.createException(
            errorCode,
            RPC_OPERATION_ERROR_KEY,
            getExceptionTitleForOperationIdAndExceptionType(wfre),
            errorMessage,
            wfre.operationId,
        )
    }

    private fun handleResourceAccessException(wfre: WorkflowsRpcException): RpcException {
        val exceptionCode = when (wfre.operationId) {
            START_PROCESS_OPERATION -> START_PROCESS_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
            TASK_ASSIGN_OPERATION -> TASK_ASSIGN_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
            TASK_COMPLETE_OPERATION -> TASK_COMPLETE_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
            SEND_MESSAGE_OPERATION -> SEND_MESSAGE_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
            CREATE_DOCUMENT_OPERATION -> CREATE_DOCUMENT_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
            CREATE_METADATA_OPERATION -> CREATE_METADATA_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
            REMOVE_METADATA_OPERATION -> REMOVE_METADATA_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
            GET_PROCESS_VARIABLE_UPDATES_OPERATION -> GET_PROCESS_VARIABLE_UPDATES_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
            SET_STATUS_OPERATION -> SET_STATUS_OPERATION_PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
            else -> PROCESS_ENGINE_ACCESS_EXCEPTION_CODE
        }
        return RpcExceptionSupport.createException(
            exceptionCode,
            PROCESS_ENGINE_ACCESS_EXCEPTION,
            "$PROCESS_ENGINE_ERROR_TITLE. Cause: ${wfre.cause}",
            wfre.message,
            wfre.operationId,
        )
    }

    private fun handleNotFoundException(wfre: WorkflowsRpcException): RpcException {
        val exceptionCode = when (wfre.operationId) {
            TASK_ASSIGN_OPERATION -> TASK_ASSIGN_OPERATION_DOCUMENT_NOT_FOUND_EXCEPTION_CODE
            TASK_COMPLETE_OPERATION -> TASK_COMPLETE_OPERATION_DOCUMENT_NOT_FOUND_EXCEPTION_CODE
            else -> NOT_FOUND_EXCEPTION_CODE
        }

        val rpcExceptionKey = when (wfre.operationId) {
            TASK_ASSIGN_OPERATION -> TASK_DOCUMENT_NOT_FOUND_EXCEPTION
            TASK_COMPLETE_OPERATION -> TASK_DOCUMENT_NOT_FOUND_EXCEPTION
            else -> RPC_OPERATION_ERROR_KEY
        }

        return RpcExceptionSupport.createException(
            exceptionCode,
            rpcExceptionKey,
            getExceptionTitleForOperationIdAndExceptionType(wfre),
            wfre.message,
            wfre.operationId,
        )
    }

    private fun handleException(wfre: WorkflowsRpcException): RpcException {
        val exceptionCode = when (wfre.operationId) {
            START_PROCESS_OPERATION -> START_PROCESS_OPERATION_COMMON_EXCEPTION_CODE
            TASK_ASSIGN_OPERATION -> TASK_ASSIGN_OPERATION_COMMON_EXCEPTION_CODE
            TASK_COMPLETE_OPERATION -> TASK_COMPLETE_OPERATION_COMMON_EXCEPTION_CODE
            SEND_MESSAGE_OPERATION -> SEND_MESSAGE_OPERATION_COMMON_EXCEPTION_CODE
            CREATE_DOCUMENT_OPERATION -> CREATE_DOCUMENT_OPERATION_COMMON_EXCEPTION_CODE
            CREATE_METADATA_OPERATION -> CREATE_METADATA_OPERATION_COMMON_EXCEPTION_CODE
            REMOVE_METADATA_OPERATION -> REMOVE_METADATA_OPERATION_COMMON_EXCEPTION_CODE
            GET_PROCESS_VARIABLE_UPDATES_OPERATION -> GET_PROCESS_VARIABLE_UPDATES_OPERATION_COMMON_EXCEPTION_CODE
            SET_STATUS_OPERATION -> SET_STATUS_OPERATION_COMMON_EXCEPTION_CODE
            else -> COMMON_EXCEPTION_CODE
        }

        return RpcExceptionSupport.createException(
            exceptionCode,
            RPC_OPERATION_ERROR_KEY,
            getExceptionTitleForOperationIdAndExceptionType(wfre),
            wfre.message,
            wfre.operationId,
        )
    }

    private fun getExceptionTitleForOperationIdAndExceptionType(wfre: WorkflowsRpcException): String {
        val title =
            when (wfre.operationId) {
                START_PROCESS_OPERATION -> "$START_PROCESS_ERROR_TITLE of ${wfre.processDefinition}."
                TASK_ASSIGN_OPERATION -> when (wfre.cause) {
                    is NotFoundException -> "$TASK_ASSIGN_ERROR_TITLE: task document with [docRef=${wfre.docRef}] was not found."
                    else -> "$TASK_ASSIGN_ERROR_TITLE."
                }
                TASK_COMPLETE_OPERATION -> when (wfre.cause) {
                    is NotFoundException -> "$TASK_COMPLETE_ERROR_TITLE: task document with [docRef=${wfre.docRef}] was not found."
                    else -> "$TASK_COMPLETE_ERROR_TITLE."
                }
                SEND_MESSAGE_OPERATION -> "$SEND_MESSAGE_ERROR_TITLE."
                CREATE_DOCUMENT_OPERATION -> "$CREATE_DOCUMENT_ERROR_TITLE [document=${wfre.documentNode}]."
                CREATE_METADATA_OPERATION -> "$CREATE_METADATA_ERROR_TITLE in ${wfre.docRef ?: "new document"}."
                REMOVE_METADATA_OPERATION -> "$REMOVE_METADATA_ERROR_TITLE from document ${wfre.docRef}."
                GET_PROCESS_VARIABLE_UPDATES_OPERATION -> "$GET_PROCESS_VARIABLE_UPDATES_ERROR_TITLE."
                SET_STATUS_OPERATION -> "$SET_STATUS_ERROR_TITLE [docRef=${wfre.docRef}]."
                else -> "Failed to execute RPC operation ${wfre.operationId}."
            }

        return title + " Cause: ${wfre.cause}"
    }
}
