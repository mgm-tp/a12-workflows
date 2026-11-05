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
import com.mgmtp.a12.dataservices.document.DocumentReferenceResult
import com.mgmtp.a12.dataservices.document.DocumentService
import com.mgmtp.a12.dataservices.document.persistence.DocumentComputationStrategy
import com.mgmtp.a12.dataservices.document.persistence.DocumentValidationStrategy
import com.mgmtp.a12.dataservices.document.support.DocumentSupport
import com.mgmtp.a12.dataservices.rpc.RemoteOperation
import com.mgmtp.a12.uaa.authorization.DecisionContext
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.CREATE_DOCUMENT_OPERATION
import com.mgmtp.a12.workflows.client.operation.WorkflowsOperationConstants.WORKFLOWS_OPERATIONS_GROUP
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcException
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcExceptionHandler
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.io.StringReader

/**
 * This operation creates a new document without triggering validation.
 */
@Component
@RemoteOperation(name = CREATE_DOCUMENT_OPERATION, group = WORKFLOWS_OPERATIONS_GROUP)
class CreateDocumentOperation(
    private val documentSupport: DocumentSupport,
    private val documentService: DocumentService,
    private val objectMapper: ObjectMapper,
    private val exceptionHandler: WorkflowsRpcExceptionHandler,
) {

    @PreAuthorize("hasUAAPermission('Document Create')")
    @Transactional("dsTransactionManager")
    fun rpc(
        @DecisionContext("documentModelName") @JsonRpcParam("documentModelName") documentModelName: String,
        @DecisionContext("document") @JsonRpcParam("document") document: JsonNode,
    ): DocumentReferenceResult {
        try {
            logger().info(
                "{} called with parameters [documentModelName={}].",
                CREATE_DOCUMENT_OPERATION,
                documentModelName,
            )
            logger().debug("{} called with document: {}.", CREATE_DOCUMENT_OPERATION, document)

            val documentV2 = StringReader(objectMapper.writeValueAsString(document)).use {
                documentSupport.convertJSONToDocument(documentModelName, it)
            }

            val docRef = documentService.create(
                documentV2,
                null,
                DocumentValidationStrategy.NO_VALIDATION,
                DocumentComputationStrategy.DEFAULT_CONFIGURATION,
            ).also {
                logger().debug("{} is completed.", CREATE_DOCUMENT_OPERATION)
            }.metadata.docRef

            return DocumentReferenceResult(docRef)
        } catch (e: Exception) {
            logger().debug(e.stackTraceToString())
            throw exceptionHandler.handleWorkflowsRpcException(WorkflowsRpcException(e, CREATE_DOCUMENT_OPERATION, documentNode = document))
        }
    }
}
