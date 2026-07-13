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
import com.mgmtp.a12.dataservices.common.exception.NotFoundException
import com.mgmtp.a12.dataservices.document.DataServicesDocument
import com.mgmtp.a12.dataservices.document.DataServicesDocumentFactory
import com.mgmtp.a12.dataservices.document.DocumentPart
import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.dataservices.document.DocumentService
import com.mgmtp.a12.dataservices.document.persistence.DocumentComputationStrategy
import com.mgmtp.a12.dataservices.document.persistence.DocumentValidationStrategy
import com.mgmtp.a12.dataservices.document.persistence.IDocumentRepository
import com.mgmtp.a12.dataservices.rpc.RemoteOperation
import com.mgmtp.a12.kernel.md.document.apiV2.DocumentPointer
import com.mgmtp.a12.uaa.authorization.DecisionContext
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.SET_STATUS_OPERATION
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcException
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcExceptionHandler
import com.mgmtp.a12.workflows.extension.internal.operation.workarounds.WorkaroundToCreateDocumentMetadata
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull

@Component
@RemoteOperation(name = SET_STATUS_OPERATION)
class SetStatusOperation(
    private val documentRepository: IDocumentRepository,
    private val workaroundToCreateDocumentMetadata: WorkaroundToCreateDocumentMetadata,
    private val dataServicesDocumentFactory: DataServicesDocumentFactory,
    private val documentService: DocumentService,
    private val exceptionHandler: WorkflowsRpcExceptionHandler,
) {

    @PreAuthorize("hasUAAPermission('Document Update')")
    @Transactional("dsTransactionManager")
    fun rpc(
        @DecisionContext("docRef") @JsonRpcParam("docRef") docRef: DocumentReference,
        @DecisionContext("documentPart") @JsonRpcParam("documentPart") documentPart: DocumentPart,
    ) {
        try {
            logger().info(
                "{} called with parameters [docRef={}].",
                SET_STATUS_OPERATION,
                docRef,
            )
            logger().debug(
                "{} called with updatedDocument [docRef={}, documentPart={}]",
                SET_STATUS_OPERATION,
                docRef,
                documentPart,
            )

            val oldDocument = documentRepository.findByDocumentReference(docRef).getOrNull() ?: throw NotFoundException(
                "Updating document without validation failed, document not found for [docRef=$docRef]",
            )
            updateDocument(oldDocument, documentPart)
        } catch (e: Exception) {
            logger().debug(e.stackTraceToString())
            exceptionHandler.handleWorkflowsRpcException(WorkflowsRpcException(e, SET_STATUS_OPERATION, docRef = docRef))
        }
        logger().debug("{} is completed.", SET_STATUS_OPERATION)
    }

    private fun updateDocument(oldDocument: DataServicesDocument, documentPart: DocumentPart) {
        val originalDocument = oldDocument.kernelDocument
        val documentWithModifiedField = originalDocument.withFieldValue(DocumentPointer.of(documentPart.path, documentPart.repetitions.toList()), documentPart.value)
        val documentWithUpdatedMetadata = workaroundToCreateDocumentMetadata.enrichDocumentWithUpdateMetadata(originalDocument, documentWithModifiedField, oldDocument.metadata.docRef)

        documentService.update(
            oldDocument.metadata.docRef,
            documentWithUpdatedMetadata,
            null,
            DocumentValidationStrategy.NO_VALIDATION,
            DocumentComputationStrategy.DEFAULT_CONFIGURATION,
        )
        val updatedDocument = dataServicesDocumentFactory.newDataServicesDocument(documentWithUpdatedMetadata)
        documentRepository.update(updatedDocument)
    }
}
