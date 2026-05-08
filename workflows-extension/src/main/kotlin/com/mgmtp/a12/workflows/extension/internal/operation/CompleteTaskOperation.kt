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
package com.mgmtp.a12.workflows.extension.internal.operation

import com.googlecode.jsonrpc4j.JsonRpcParam
import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.dataservices.rpc.RemoteOperation
import com.mgmtp.a12.dataservices.rpc.RemoteOperation.RemoteOperationHelper
import com.mgmtp.a12.dataservices.rpc.RpcExceptionSupport
import com.mgmtp.a12.uaa.authorization.DecisionContext
import com.mgmtp.a12.workflows.client.operation.WorkflowsOperationConstants.TASK_COMPLETE_OPERATION
import com.mgmtp.a12.workflows.engine.client.internal.ProcessEngineOperationBroker
import com.mgmtp.a12.workflows.extension.event.TaskAfterCompleteEvent
import com.mgmtp.a12.workflows.extension.event.TaskBeforeCompleteEvent
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionKeys.METADATA_FIELD_NOT_FOUND_EXCEPTION
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcException
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcExceptionHandler
import com.mgmtp.a12.workflows.extension.internal.utils.WorkflowsDocumentService
import com.mgmtp.a12.workflows.extension.internal.utils.getTextValueFromA12wfMetadata
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPath.RELATIVE_BUSINESS_KEY_PATH
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPath.RELATIVE_TASK_ID_PATH
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.A12WF
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * This operation sends request to Process Engine to complete a process.
 * The operation as well removes the task metadata from the document with given documentReference
 */
@Component
@RemoteOperation(name = TASK_COMPLETE_OPERATION)
class CompleteTaskOperation(
    private val workflowsDocumentService: WorkflowsDocumentService,
    private val processEngineOperationBroker: ProcessEngineOperationBroker,
    private val eventPublisher: ApplicationEventPublisher,
    private val exceptionHandler: WorkflowsRpcExceptionHandler,
) {

    @PreAuthorize("hasUAAPermission('TaskComplete')")
    @Transactional
    fun rpc(
        @DecisionContext("docRef") @JsonRpcParam("docRef") taskDocRef: DocumentReference,
    ) {
        logger().info(
            "{} called with parameters [taskDocRef={}]",
            TASK_COMPLETE_OPERATION,
            taskDocRef,
        )

        try {
            eventPublisher.publishEvent(TaskBeforeCompleteEvent(taskDocRef))

            val document = workflowsDocumentService.loadDocument(taskDocRef)

            val taskIdBeforeRemoval = document.getTextValueFromA12wfMetadata(RELATIVE_TASK_ID_PATH) ?: throwMissingMetadataFieldCompleteRpcException(taskDocRef, "$A12WF$RELATIVE_TASK_ID_PATH")
            val businessKeyBeforeRemoval = document.getTextValueFromA12wfMetadata(RELATIVE_BUSINESS_KEY_PATH) ?: throwMissingMetadataFieldCompleteRpcException(taskDocRef, "$A12WF$RELATIVE_BUSINESS_KEY_PATH")

            processEngineOperationBroker.completeTask(taskIdBeforeRemoval, businessKeyBeforeRemoval)

            eventPublisher.publishEvent(TaskAfterCompleteEvent(taskDocRef))
        } catch (e: Exception) {
            logger().debug(e.stackTraceToString())
            exceptionHandler.handleWorkflowsRpcException(WorkflowsRpcException(e, TASK_COMPLETE_OPERATION, docRef = taskDocRef))
        }
        logger().debug("{} is completed.", TASK_COMPLETE_OPERATION)
    }
}

private fun throwMissingMetadataFieldCompleteRpcException(taskDocRef: DocumentReference, metadataFieldName: String): Nothing = throw RpcExceptionSupport.createException(
    HttpStatus.NOT_FOUND.value(),
    METADATA_FIELD_NOT_FOUND_EXCEPTION,
    "Could not complete task: metadata field $metadataFieldName not found on document with docRef [$taskDocRef]",
    RemoteOperationHelper.getOperationId(CompleteTaskOperation::class.java),
)
