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
import com.mgmtp.a12.dataservices.rpc.RemoteOperation
import com.mgmtp.a12.uaa.authorization.DecisionContext
import com.mgmtp.a12.workflows.client.operation.WorkflowsOperationConstants.START_PROCESS_OPERATION
import com.mgmtp.a12.workflows.engine.client.internal.ProcessEngineOperationBroker
import com.mgmtp.a12.workflows.extension.event.ProcessAfterStartEvent
import com.mgmtp.a12.workflows.extension.event.ProcessBeforeStartEvent
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcException
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcExceptionHandler
import com.mgmtp.a12.workflows.extension.internal.utils.convertJsonToProcessEngineMap
import com.mgmtp.a12.workflows.shared.dtos.ProcessEngineVariable
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.JsonNode

/**
 * This operation sends request to Process Engine to start a process with given process definition.
 */
@Component
@RemoteOperation(name = START_PROCESS_OPERATION)
class StartProcessOperation(
    private val anonymizer: Anonymizer,
    private val processEngineOperationBroker: ProcessEngineOperationBroker,
    private val eventPublisher: ApplicationEventPublisher,
    private val exceptionHandler: WorkflowsRpcExceptionHandler,
) {

    @PreAuthorize("hasUAAPermission('StartProcess')")
    @Transactional("dsTransactionManager")
    fun rpc(
        @DecisionContext("processDefinition") @JsonRpcParam("processDefinition") processDefinition: String,
        @DecisionContext("businessKey") @JsonRpcParam("businessKey") businessKey: String,
        @DecisionContext("variables") @JsonRpcParam("variables") variables: JsonNode?,
    ) {
        logger().info(
            "{} is called with parameters [processDefinition={}, businessKey={}].",
            START_PROCESS_OPERATION,
            processDefinition,
            anonymizer.apply(businessKey),
        )
        logger().debug(
            "{} is called with parameters [processDefinition={}, businessKey={}, variables={}].",
            START_PROCESS_OPERATION,
            processDefinition,
            businessKey,
            variables,
        )
        try {
            val initiator = readCurrentUserAsProcessInitiator()
            val processEngineVariables = mapOf("initiator" to ProcessEngineVariable(initiator)) + convertJsonToProcessEngineMap(variables)

            eventPublisher.publishEvent(ProcessBeforeStartEvent(processDefinition, businessKey, variables))

            processEngineOperationBroker.startProcessInstance(processDefinition, businessKey, processEngineVariables)

            logger().info("Process engine started instance of {}", processDefinition)

            eventPublisher.publishEvent(ProcessAfterStartEvent(processDefinition, businessKey, variables))
        } catch (iae: IllegalArgumentException) {
            logger().debug(iae.stackTraceToString())
            logger().debug("{} failed.", START_PROCESS_OPERATION)
            throw iae
        } catch (e: Exception) {
            logger().debug(e.stackTraceToString())
            exceptionHandler.handleWorkflowsRpcException(WorkflowsRpcException(e, START_PROCESS_OPERATION, processDefinition = processDefinition))
        }
        logger().debug("{} is completed.", START_PROCESS_OPERATION)
    }

    private fun readCurrentUserAsProcessInitiator(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = authentication?.principal ?: throw ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "No current user could be found",
        )
        val initiator = when (user) {
            is UserDetails -> user.username
            else -> user.toString()
        }
        return initiator
    }
}
