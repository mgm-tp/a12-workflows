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
package com.mgmtp.a12.workflows.engine.internal.event.rollback

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.workflows.engine.internal.util.MetadataRetriever
import com.mgmtp.a12.workflows.engine.internal.util.ProcessEngineVariableNames
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.DataServicesRpcFacade
import com.mgmtp.a12.workflows.shared.dtos.ProcessMetadata
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.cibseven.bpm.engine.TaskService
import org.cibseven.bpm.engine.delegate.DelegateTask
import org.cibseven.bpm.engine.impl.cfg.TransactionState
import org.cibseven.bpm.engine.impl.context.Context
import org.springframework.context.event.EventListener

class TransactionRollbackListener(
    private val taskService: TaskService,
    private val rpcFacade: DataServicesRpcFacade,
    private val metadataRetriever: MetadataRetriever,
    private val objectMapper: ObjectMapper,
) {

    private val logMessage = "Transaction Rollback listener invoked for %s event of task %s"

    @EventListener(condition = "#delegateTask.eventName.equals('complete')")
    fun compensateTaskCompleteFailure(delegateTask: DelegateTask) {
        logger().debug("Compensate task complete failure for Delegate Task ID/ASSIGNEE : ${delegateTask.id}/${delegateTask.assignee}")
        recreateMetadataOnRollback(delegateTask, "onComplete")
    }

    @EventListener(condition = "#delegateTask.eventName.equals('delete')")
    fun compensateTaskDeleteFailure(delegateTask: DelegateTask) {
        logger().debug("Compensate task delete failure for Delegate Task ID/ASSIGNEE : ${delegateTask.id}/${delegateTask.assignee}")
        recreateMetadataOnRollback(delegateTask, "onDelete")
    }

    private fun recreateMetadataOnRollback(delegateTask: DelegateTask, eventName: String) {
        Context.getCommandContext().transactionContext.addTransactionListener(TransactionState.ROLLED_BACK) {
            logger().info(String.format(logMessage, eventName, delegateTask.id))
            it.processEngineConfiguration.commandExecutorTxRequired.execute {
                val metadata = metadataRetriever.retrieveMetadata(delegateTask)
                val metadataString = objectMapper.writeValueAsString(metadata)
                val metadataNode = objectMapper.readTree(metadataString) as JsonNode
                val processMetadata = objectMapper.treeToValue(metadataNode, ProcessMetadata::class.java)
                val documentReference =
                    taskService.getVariable(delegateTask.id, ProcessEngineVariableNames.DOCUMENT_REFERENCE_VARIABLE)
                        ?: taskService.getVariable(delegateTask.id, ProcessEngineVariableNames.NEW_DOC_REF_VARIABLE)
                if (documentReference == null) {
                    logger().error("Could not find usable document reference variable during rollback. Workflows Engine and Dataservices could be out of sync.")
                    return@execute
                }
                try {
                    rpcFacade.tryCreateMetadata(DocumentReference(documentReference.toString()), processMetadata)
                } catch (e: Exception) {
                    logger().debug("Exception while recreating metadata during transaction rollback. Workflows Engine and Dataservices could be out of sync.")
                }
            }
        }
    }
}
