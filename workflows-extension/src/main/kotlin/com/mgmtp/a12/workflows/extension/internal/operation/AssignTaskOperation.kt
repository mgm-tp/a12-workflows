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
import com.mgmtp.a12.dataservices.common.anonymizing.Anonymizer
import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.dataservices.document.DocumentService
import com.mgmtp.a12.dataservices.document.persistence.DocumentComputationStrategy
import com.mgmtp.a12.dataservices.document.persistence.DocumentValidationStrategy
import com.mgmtp.a12.dataservices.rpc.RemoteOperation
import com.mgmtp.a12.dataservices.rpc.RemoteOperation.RemoteOperationHelper
import com.mgmtp.a12.dataservices.rpc.RpcExceptionSupport
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.DocumentV2
import com.mgmtp.a12.uaa.authorization.DecisionContext
import com.mgmtp.a12.workflows.client.operation.WorkflowsOperationConstants.TASK_ASSIGN_OPERATION
import com.mgmtp.a12.workflows.client.operation.WorkflowsOperationConstants.WORKFLOWS_OPERATIONS_GROUP
import com.mgmtp.a12.workflows.engine.client.internal.ProcessEngineOperationBroker
import com.mgmtp.a12.workflows.extension.event.TaskAfterAssignEvent
import com.mgmtp.a12.workflows.extension.event.TaskBeforeAssignEvent
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionKeys.METADATA_FIELD_NOT_FOUND_EXCEPTION
import com.mgmtp.a12.workflows.extension.exception.WorkflowsRpcExceptionKeys.TASK_METADATA_NOT_FOUND_EXCEPTION
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcException
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcExceptionHandler
import com.mgmtp.a12.workflows.extension.internal.utils.WorkflowsDocumentService
import com.mgmtp.a12.workflows.extension.internal.utils.getDocumentPointerToA12wfMetadata
import com.mgmtp.a12.workflows.extension.internal.utils.getTextValueFromA12wfMetadata
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPath.RELATIVE_BUSINESS_KEY_PATH
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPath.RELATIVE_TASK_ID_PATH
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.A12WF
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.ASSIGNEE
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.TASK
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * This operation updates the assignee in the referenced task document and triggers an assignee update in Process Engine.
 */
@Component
@RemoteOperation(name = TASK_ASSIGN_OPERATION, group = WORKFLOWS_OPERATIONS_GROUP)
class AssignTaskOperation(
    private val anonymizer: Anonymizer,
    private val workflowsDocumentService: WorkflowsDocumentService,
    private val documentService: DocumentService,
    private val eventPublisher: ApplicationEventPublisher,
    private val processEngineOperationBroker: ProcessEngineOperationBroker,
    private val exceptionHandler: WorkflowsRpcExceptionHandler,
) {
    @PreAuthorize("hasUAAPermission('TaskAssign')")
    @Transactional("dsTransactionManager")
    fun rpc(
        @DecisionContext("docRef") @JsonRpcParam("docRef") taskDocRef: DocumentReference,
        @DecisionContext("assignee") @JsonRpcParam("assignee") assignee: String,
    ) {
        logger().info(
            "{} called with parameters [docRef={}, assignee={}]",
            TASK_ASSIGN_OPERATION,
            taskDocRef,
            anonymizer.apply(assignee),
        )
        logger().debug(
            "{} called with parameters [docRef={}, assignee={}]",
            TASK_ASSIGN_OPERATION,
            taskDocRef,
            assignee,
        )

        try {
            eventPublisher.publishEvent(TaskBeforeAssignEvent(taskDocRef, assignee))

            val document = workflowsDocumentService.loadDocument(taskDocRef)

            val updatedDocument = updateAssigneeField(document, taskDocRef, assignee)
            documentService.update(
                taskDocRef,
                updatedDocument,
                null,
                DocumentValidationStrategy.NO_VALIDATION,
                DocumentComputationStrategy.DEFAULT_CONFIGURATION,
            )

            val taskId = updatedDocument.getTextValueFromA12wfMetadata(RELATIVE_TASK_ID_PATH) ?: throwMissingMetadataFieldAssignRpcException(taskDocRef, "$A12WF$RELATIVE_TASK_ID_PATH")
            val businessKey = updatedDocument.getTextValueFromA12wfMetadata(RELATIVE_BUSINESS_KEY_PATH) ?: throwMissingMetadataFieldAssignRpcException(taskDocRef, "$A12WF$RELATIVE_BUSINESS_KEY_PATH")
            processEngineOperationBroker.assign(taskId, businessKey, assignee)

            eventPublisher.publishEvent(TaskAfterAssignEvent(taskDocRef, assignee))
        } catch (e: Exception) {
            logger().debug(e.stackTraceToString())
            exceptionHandler.handleWorkflowsRpcException(WorkflowsRpcException(e, TASK_ASSIGN_OPERATION, docRef = taskDocRef))
        }
        logger().debug("{} is completed.", TASK_ASSIGN_OPERATION)
    }

    private fun updateAssigneeField(document: DocumentV2, taskDocRef: DocumentReference, newAssignee: String): DocumentV2 {
        val a12WfPointer = document.getDocumentPointerToA12wfMetadata() ?: throwMissingTaskMetadataAssignRpcException(taskDocRef)

        val taskGroup = document.group(a12WfPointer)?.group(TASK) ?: throwMissingTaskMetadataAssignRpcException(taskDocRef)

        val updatedTaskGroup = taskGroup.withFieldValue(ASSIGNEE, newAssignee)
        return document.withGroup("$a12WfPointer/$TASK", updatedTaskGroup)
    }

    private fun throwMissingTaskMetadataAssignRpcException(taskDocRef: DocumentReference): Nothing = throw RpcExceptionSupport.createException(
        HttpStatus.NOT_FOUND.value(),
        TASK_METADATA_NOT_FOUND_EXCEPTION,
        "Could not assign task: task metadata not found on document with docRef [$taskDocRef]",
        RemoteOperationHelper.getOperationId(AssignTaskOperation::class.java),
    )

    private fun throwMissingMetadataFieldAssignRpcException(taskDocRef: DocumentReference, metadataFieldName: String): Nothing = throw RpcExceptionSupport.createException(
        HttpStatus.NOT_FOUND.value(),
        METADATA_FIELD_NOT_FOUND_EXCEPTION,
        "Could not assign task: metadata field $metadataFieldName not found on document with docRef [$taskDocRef]",
        RemoteOperationHelper.getOperationId(AssignTaskOperation::class.java),
    )
}
