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
import com.mgmtp.a12.dataservices.document.persistence.internal.DefaultDocumentModelResolver
import com.mgmtp.a12.dataservices.rpc.RemoteOperation
import com.mgmtp.a12.uaa.authorization.DecisionContext
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.GET_PROCESS_VARIABLE_UPDATES_OPERATION
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcException
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcExceptionHandler
import com.mgmtp.a12.workflows.extension.internal.sync.WorkflowsDocumentModelWalker
import com.mgmtp.a12.workflows.shared.dtos.FieldUpdatesDto
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import com.mgmtp.a12.dataservices.document.DocumentService as DataServicesDocumentService

@Component
@RemoteOperation(name = GET_PROCESS_VARIABLE_UPDATES_OPERATION)
class GetProcessVariableUpdatesOperation(
    private val defaultDocumentModelResolver: DefaultDocumentModelResolver,
    private val documentService: DataServicesDocumentService,
    private val workflowsDocumentModelWalker: WorkflowsDocumentModelWalker,
    private val exceptionHandler: WorkflowsRpcExceptionHandler,
) {

    @PreAuthorize("hasUAAPermission('GetProcessVariableUpdates')")
    @Transactional("dsTransactionManager")
    fun rpc(
        @DecisionContext("documentReferences") @JsonRpcParam("documentReferences") documentReferences: List<DocumentReference>,
    ): FieldUpdatesDto {
        logger().info(
            "{} called with parameters [docRefs={}]",
            GET_PROCESS_VARIABLE_UPDATES_OPERATION,
            documentReferences,
        )

        try {
            return getProcessVariableUpdatesByDocRefs(documentReferences).also {
                logger().debug("{} is completed.", GET_PROCESS_VARIABLE_UPDATES_OPERATION)
            }
        } catch (e: Exception) {
            logger().debug(e.stackTraceToString())
            throw exceptionHandler.handleWorkflowsRpcException(WorkflowsRpcException(e, GET_PROCESS_VARIABLE_UPDATES_OPERATION))
        }
    }

    private fun getProcessVariableUpdatesByDocRefs(documentReferences: List<DocumentReference>): FieldUpdatesDto {
        val allModifications = mutableMapOf<String, Any?>()

        for (docRef in documentReferences) {
            val documentModel = defaultDocumentModelResolver.getDocumentModelById(docRef.documentModelName)
            val document = documentService.load(docRef).get().kernelDocument
            val (modifications) = workflowsDocumentModelWalker.collectSyncAnnotatedFields(
                documentModel,
                document,
            )
            allModifications += modifications.mapValues { it.value.value }
        }

        return FieldUpdatesDto(allModifications)
    }
}
