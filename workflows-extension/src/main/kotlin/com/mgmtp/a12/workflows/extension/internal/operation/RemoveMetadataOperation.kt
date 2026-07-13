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

import com.googlecode.jsonrpc4j.JsonRpcParam
import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.dataservices.document.DocumentService
import com.mgmtp.a12.dataservices.document.persistence.DocumentComputationStrategy
import com.mgmtp.a12.dataservices.document.persistence.DocumentValidationStrategy
import com.mgmtp.a12.dataservices.rpc.RemoteOperation
import com.mgmtp.a12.dataservices.rpc.RemoteOperation.RemoteOperationHelper
import com.mgmtp.a12.dataservices.rpc.RpcExceptionSupport
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.DocumentV2
import com.mgmtp.a12.uaa.authorization.DecisionContext
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.REMOVE_METADATA_OPERATION
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionKeys.TASK_METADATA_NOT_FOUND_EXCEPTION
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcException
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcExceptionHandler
import com.mgmtp.a12.workflows.extension.internal.utils.WorkflowsDocumentService
import com.mgmtp.a12.workflows.extension.internal.utils.getDocumentPointerToA12wfMetadata
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.TASK
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.TASK_DEFINITION
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.WORKFLOWS_INTERNAL_DOCUMENT_STATUS
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

data class UnableToUpdateMetadataException(override val message: String) : RuntimeException()

@Component
@RemoteOperation(name = REMOVE_METADATA_OPERATION)
class RemoveMetadataOperation(
    private val workflowsDocumentService: WorkflowsDocumentService,
    private val documentService: DocumentService,
    private val exceptionHandler: WorkflowsRpcExceptionHandler,
) {

    @PreAuthorize("(hasUAAPermission('Query') && hasUAAPermission('Document Update')) || hasUAAPermission('RemoveMetadata')")
    @Transactional("dsTransactionManager")
    fun rpc(
        @DecisionContext("docRef") @JsonRpcParam("docRef") docRef: DocumentReference,
    ) {
        logger().info(
            "{} called with parameters [docRef={}]",
            REMOVE_METADATA_OPERATION,
            docRef,
        )

        try {
            val document = workflowsDocumentService.loadDocument(docRef)

            val updatedDocument = removeWorkflowsMetadata(document, docRef)

            documentService.update(
                docRef,
                updatedDocument,
                null,
                DocumentValidationStrategy.NO_VALIDATION,
                DocumentComputationStrategy.DEFAULT_CONFIGURATION,
            )
        } catch (e: Exception) {
            logger().debug(e.stackTraceToString())
            exceptionHandler.handleWorkflowsRpcException(WorkflowsRpcException(e, REMOVE_METADATA_OPERATION, docRef = docRef))
        }
        logger().debug("{} is completed.", REMOVE_METADATA_OPERATION)
    }

    private fun removeWorkflowsMetadata(document: DocumentV2, docRef: DocumentReference): DocumentV2 {
        try {
            val a12WfPointer = document.getDocumentPointerToA12wfMetadata() ?: throwMissingTaskMetadataRemoveRpcException(docRef)
            var a12WfGroup = document.group(a12WfPointer)
            a12WfGroup = a12WfGroup
                .withFieldRemoved(WORKFLOWS_INTERNAL_DOCUMENT_STATUS)
                .withGroupRemoved(TASK_DEFINITION)
                .withGroupRemoved(TASK)
            return document.withGroup(a12WfPointer, a12WfGroup)
        } catch (e: IllegalStateException) {
            logger().error("Can't update workflows metadata. \"A12WF\" node either couldn't be found or is not an ObjectNode:{}", e.cause.toString())
            logger().debug(e.stackTraceToString())
            throw UnableToUpdateMetadataException("Can't update workflows metadata due to following exception $e")
        }
    }

    private fun throwMissingTaskMetadataRemoveRpcException(taskDocRef: DocumentReference): Nothing = throw RpcExceptionSupport.createException(
        HttpStatus.NOT_FOUND.value(),
        TASK_METADATA_NOT_FOUND_EXCEPTION,
        "Could not remove metadata from document: metadata not found on document with docRef [$taskDocRef]",
        "Could not remove metadata from document: metadata not found on document with docRef [$taskDocRef]",
        RemoteOperationHelper.getOperationId(RemoveMetadataOperation::class.java),
    )
}
