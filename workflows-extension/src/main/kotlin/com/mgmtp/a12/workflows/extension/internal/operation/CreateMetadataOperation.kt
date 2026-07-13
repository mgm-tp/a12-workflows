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
import com.mgmtp.a12.dataservices.document.DocumentReferenceResult
import com.mgmtp.a12.dataservices.document.DocumentService
import com.mgmtp.a12.dataservices.document.persistence.DocumentComputationStrategy
import com.mgmtp.a12.dataservices.document.persistence.DocumentValidationStrategy
import com.mgmtp.a12.dataservices.model.ModelService
import com.mgmtp.a12.dataservices.model.document.persistence.DocumentModelLoader
import com.mgmtp.a12.dataservices.rpc.RemoteOperation
import com.mgmtp.a12.kernel.md.document.apiV2.DocumentPointer
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.DocumentV2
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.GroupInstanceV2
import com.mgmtp.a12.kernel.md.facade.DocumentModelServiceFactory
import com.mgmtp.a12.kernel.md.model.api.IDocumentModel
import com.mgmtp.a12.kernel.md.model.api.IGroup
import com.mgmtp.a12.kernel.md.model.api.visitor.DocumentModelVisitor
import com.mgmtp.a12.kernel.md.model.api.visitor.DocumentModelWalker
import com.mgmtp.a12.uaa.authorization.DecisionContext
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.CREATE_METADATA_OPERATION
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcException
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcExceptionHandler
import com.mgmtp.a12.workflows.extension.internal.operation.constants.InternalDocumentStatus
import com.mgmtp.a12.workflows.extension.internal.utils.WorkflowsDocumentService
import com.mgmtp.a12.workflows.extension.internal.utils.getReferencedDocumentModelName
import com.mgmtp.a12.workflows.extension.internal.utils.getTextValueFromA12wfMetadata
import com.mgmtp.a12.workflows.shared.dtos.ProcessMetadata
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPath.RELATIVE_TASK_ID_PATH
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.A12WF
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.ASSIGNEE
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.BUSINESS_KEY
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.CREATION_DATE
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.DUE_DATE
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.FORM_KEY
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.ID
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.INITIATOR
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.KEY
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.NAME
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.PROCESS
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.PROCESS_DEFINITION
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.TASK
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.TASK_DEFINITION
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.WORKFLOWS_INTERNAL_DOCUMENT_STATUS
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import jakarta.annotation.Nullable
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.time.ZoneId

const val CDM_QUERY_ROOT = "cdm.queryRoot"

data class DocumentAlreadyReferencedException(val errorMessage: String) : RuntimeException(errorMessage)

private fun IDocumentModel.isCdm(): Boolean = getCdmRootDocumentModelName() != null

private fun IDocumentModel.getCdmRootDocumentModelName(): String? = this.header.annotations.find { it.name.equals(CDM_QUERY_ROOT) }?.value

@Component
@RemoteOperation(name = CREATE_METADATA_OPERATION)
class CreateMetadataOperation(
    private val workflowsDocumentService: WorkflowsDocumentService,
    private val modelService: ModelService,
    private val documentModelLoader: DocumentModelLoader,
    private val documentService: DocumentService,
    private val exceptionHandler: WorkflowsRpcExceptionHandler,
) {
    private val documentModelWalker = DocumentModelWalker()
    private val documentModelService = DocumentModelServiceFactory().createDocumentModelService()

    @PreAuthorize(
        """
        (hasUAAPermission('Query') && hasUAAPermission('Document Update') && hasUAAPermission('Document Create'))
            || hasUAAPermission('CreateMetadata')
    """,
    )
    @Transactional("dsTransactionManager")
    fun rpc(
        @DecisionContext("docRef") @Nullable @JsonRpcParam("docRef") docRef: DocumentReference?,
        @DecisionContext("metadata") @JsonRpcParam("metadata") metadata: ProcessMetadata,
    ): DocumentReferenceResult {
        try {
            logger().info(
                "{} called with parameters [docRef={}]",
                CREATE_METADATA_OPERATION,
                docRef,
            )
            logger().debug("{} called with metadata: {}", CREATE_METADATA_OPERATION, metadata)

            val outputDocRef = if (docRef == null) {
                createEmptyDocumentWithMetadata(metadata)
            } else {
                updateExistingDocumentWithMetadata(docRef, metadata)
            }
            return DocumentReferenceResult(outputDocRef).also {
                logger().debug("{} is completed.", CREATE_METADATA_OPERATION)
            }
        } catch (de: DocumentAlreadyReferencedException) {
            logger().debug(de.stackTraceToString())
            logger().debug("{} failed", CREATE_METADATA_OPERATION)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, de.message)
        } catch (ne: NoSuchElementException) {
            logger().debug(ne.stackTraceToString())
            logger().debug("{} failed", CREATE_METADATA_OPERATION)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ne.message)
        } catch (e: Exception) {
            logger().debug(e.stackTraceToString())
            throw exceptionHandler.handleWorkflowsRpcException(WorkflowsRpcException(e, CREATE_METADATA_OPERATION, docRef = docRef))
        }
    }

    private fun createEmptyDocumentWithMetadata(metadata: ProcessMetadata): DocumentReference {
        val formModelName =
            metadata.taskDefinition.formKey ?: throw NullPointerException("FormKey in given WorkflowsMetadata is null.")
        var documentModelName = getDocumentModelName(formModelName)
        logger().info("DM name extracted from FM: [documentModelName = $documentModelName]")

        var documentModel = documentModelLoader.loadModel(documentModelName)
        if (documentModel.isCdm()) {
            documentModelName = documentModel.getCdmRootDocumentModelName()!!
            documentModel = documentModelLoader.loadModel(documentModelName)
            logger().info("DM is CDM. Root document model name is: [rootDocumentModelName = $documentModelName]")
        }

        val a12wfPointer = findPathToA12wfGroup(documentModel)
        val updatedMetadata = metadata.copy(
            workflowsInternalDocumentStatus = InternalDocumentStatus.EMPTY.toString(),
        )

        val timeZone = documentModel.content.documentModelConfig.timeZone
        val emptyDocument = DocumentV2.empty(documentModelName)
        val document = addMetadataToDocument(emptyDocument, a12wfPointer, updatedMetadata, timeZone)

        return documentService.create(
            document,
            null,
            DocumentValidationStrategy.NO_VALIDATION,
            DocumentComputationStrategy.DEFAULT_CONFIGURATION,
        ).metadata.docRef
    }

    private fun updateExistingDocumentWithMetadata(
        docRef: DocumentReference,
        metadata: ProcessMetadata,
    ): DocumentReference {
        val document = workflowsDocumentService.loadDocument(docRef)

        val taskIdField = document.getTextValueFromA12wfMetadata(RELATIVE_TASK_ID_PATH)
        val isAlreadyReferencedByAnotherTask = taskIdField != null
        if (isAlreadyReferencedByAnotherTask) {
            throw DocumentAlreadyReferencedException("Metadata for document [$docRef] is not empty")
        }

        val updatedMetadata = metadata.copy(
            workflowsInternalDocumentStatus = InternalDocumentStatus.NON_EMPTY.toString(),
        )

        val documentModel = documentModelLoader.loadModel(docRef.documentModelName)
        val timeZone = documentModel.content.documentModelConfig.timeZone
        val a12WfPointer = findPathToA12wfGroup(documentModel)
        val updatedDocument = addMetadataToDocument(document, a12WfPointer, updatedMetadata, timeZone)
        return documentService.update(
            docRef,
            updatedDocument,
            null,
            DocumentValidationStrategy.NO_VALIDATION,
            DocumentComputationStrategy.DEFAULT_CONFIGURATION,
        ).metadata.docRef
    }

    private fun findPathToA12wfGroup(documentModel: IDocumentModel): DocumentPointer {
        val a12wfGroup = findA12wfGroup(documentModel)

        val pathToA12wfGroup = documentModelService
            .getPath(a12wfGroup)
        return DocumentPointer.of(pathToA12wfGroup)
    }

    private fun findA12wfGroup(documentModel: IDocumentModel): IGroup {
        var a12wfGroup: IGroup? = null
        documentModelWalker.acceptDocumentModel(
            documentModel,
            object : DocumentModelVisitor() {
                override fun visitGroup(group: IGroup): DocumentModelWalker.VisitProcess = when {
                    group.name == A12WF -> {
                        a12wfGroup = group
                        DocumentModelWalker.VisitProcess.STOP
                    }
                    else -> super.visitGroup(group)
                }
            },
        )
        return a12wfGroup ?: throw NoSuchElementException("Could not find A12WF group in model ${documentModel.header.id}")
    }

    private fun addMetadataToDocument(
        document: DocumentV2,
        a12wfPointer: DocumentPointer,
        processMetadata: ProcessMetadata,
        timeZone: String,
    ): DocumentV2 {
        val processMetadataGroup = mapProcessMetadataToGroupInstanceV2(processMetadata, timeZone)
        return document.withGroup(a12wfPointer, processMetadataGroup)
    }

    private fun mapProcessMetadataToGroupInstanceV2(processMetadata: ProcessMetadata, timeZone: String): GroupInstanceV2 = with(processMetadata) {
        logger().debug("Inside mapProcessMetadataToGroupInstanceV2")
        logger().debug("[task.creationDate = ${task.creationDate}]")
        logger().debug("[task.dueDate = ${task.dueDate}]")
        GroupInstanceV2.empty()
            .withGroup(
                PROCESS_DEFINITION,
                GroupInstanceV2.empty()
                    .withFieldValue(KEY, processDefinition.key)
                    .withFieldValue(NAME, processDefinition.name),
            )
            .withGroup(
                TASK_DEFINITION,
                GroupInstanceV2.empty()
                    .withFieldValue(KEY, taskDefinition.key)
                    .withFieldValue(NAME, taskDefinition.name)
                    .withFieldValue(FORM_KEY, taskDefinition.formKey),
            )
            .withGroup(
                PROCESS,
                GroupInstanceV2.empty()
                    .withFieldValue(ID, process.id)
                    .withFieldValue(BUSINESS_KEY, process.businessKey)
                    .withFieldValue(INITIATOR, process.initiator),
            )
            .withGroup(
                TASK,
                GroupInstanceV2.empty()
                    .withFieldValue(ID, task.id)
                    .withFieldValue(
                        CREATION_DATE,
                        LocalDateTime.parse(task.creationDate).atZone(ZoneId.of(timeZone)).toInstant(),
                    )
                    .withFieldValue(DUE_DATE, null) // Initial value for task.dueDate is always null.
                    .withFieldValue(ASSIGNEE, task.assignee),
            )
            .withFieldValue(WORKFLOWS_INTERNAL_DOCUMENT_STATUS, workflowsInternalDocumentStatus)
    }

    private fun getDocumentModelName(formModelName: String): String {
        val genericModel = modelService.load(formModelName)
        return genericModel.getReferencedDocumentModelName()
    }
}
