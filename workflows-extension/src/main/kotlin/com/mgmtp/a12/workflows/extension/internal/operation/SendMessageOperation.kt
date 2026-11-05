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
import com.mgmtp.a12.workflows.client.operation.WorkflowsOperationConstants.SEND_MESSAGE_OPERATION
import com.mgmtp.a12.workflows.client.operation.WorkflowsOperationConstants.WORKFLOWS_OPERATIONS_GROUP
import com.mgmtp.a12.workflows.engine.client.internal.ProcessEngineOperationBroker
import com.mgmtp.a12.workflows.extension.event.ProcessAfterMessageSendEvent
import com.mgmtp.a12.workflows.extension.event.ProcessBeforeMessageSendEvent
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcException
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcExceptionHandler
import com.mgmtp.a12.workflows.extension.internal.utils.convertJsonToProcessEngineMap
import com.mgmtp.a12.workflows.shared.dtos.MessageDto
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import jakarta.annotation.Nullable
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode

@Component
@RemoteOperation(name = SEND_MESSAGE_OPERATION, group = WORKFLOWS_OPERATIONS_GROUP)
class SendMessageOperation(
    private val anonymizer: Anonymizer,
    private val processEngineOperationBroker: ProcessEngineOperationBroker,
    private val eventPublisher: ApplicationEventPublisher,
    private val exceptionHandler: WorkflowsRpcExceptionHandler,
) {

    @PreAuthorize("hasUAAPermission('SendMessage')")
    @Transactional("dsTransactionManager")
    fun rpc(
        @DecisionContext("messageName") @JsonRpcParam("messageName") messageName: String,
        @DecisionContext("businessKey") @JsonRpcParam("businessKey") businessKey: String,
        @DecisionContext("processInstanceId") @Nullable @JsonRpcParam("processInstanceId") processInstanceId: String?,
        @DecisionContext("payload") @Nullable @JsonRpcParam("payload") payload: JsonNode?,
    ) {
        val anonProcessId = anonymizer.apply(processInstanceId)
        val anonMessageName = anonymizer.apply(messageName)
        val anonBusinessKey = anonymizer.apply(businessKey)
        logger().info(
            "{} is called with parameters [processInstanceId={}, messageName={}, businessKey={}].",
            SEND_MESSAGE_OPERATION,
            anonProcessId,
            anonMessageName,
            anonBusinessKey,
        )
        logger().debug(
            "{} is called with parameters [processInstanceId={}, messageName={}, businessKey={}].",
            SEND_MESSAGE_OPERATION,
            processInstanceId,
            messageName,
            businessKey,
        )
        try {
            val processEngineVariables = convertJsonToProcessEngineMap(payload)

            eventPublisher.publishEvent(
                ProcessBeforeMessageSendEvent(messageName, businessKey, processInstanceId, payload),
            )

            val messageBody = MessageDto(messageName, businessKey, processInstanceId, processEngineVariables)
            processEngineOperationBroker.sendMessage(messageBody)
            logger().debug("Message with name {} is sent", messageName)

            eventPublisher.publishEvent(
                ProcessAfterMessageSendEvent(messageName, businessKey, processInstanceId, payload),
            )
        } catch (iae: IllegalArgumentException) {
            logger().debug(iae.stackTraceToString())
            logger().debug("{} failed.", SEND_MESSAGE_OPERATION)
            throw iae
        } catch (e: Exception) {
            logger().debug(e.stackTraceToString())
            exceptionHandler.handleWorkflowsRpcException(WorkflowsRpcException(e, SEND_MESSAGE_OPERATION))
        }
        logger().debug("{} is completed.", SEND_MESSAGE_OPERATION)
    }
}
