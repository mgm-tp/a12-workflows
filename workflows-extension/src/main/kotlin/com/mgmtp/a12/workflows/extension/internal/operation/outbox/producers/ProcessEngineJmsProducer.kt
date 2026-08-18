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
package com.mgmtp.a12.workflows.extension.internal.operation.outbox.producers

import com.mgmtp.a12.workflows.consumer.internal.commands.AssignTaskOutboxCommand
import com.mgmtp.a12.workflows.consumer.internal.commands.CompleteTaskOutboxCommand
import com.mgmtp.a12.workflows.consumer.internal.commands.SendMessageOutboxCommand
import com.mgmtp.a12.workflows.consumer.internal.commands.StartProcessOutboxCommand
import com.mgmtp.a12.workflows.engine.client.internal.ProcessEngineOperationBroker
import com.mgmtp.a12.workflows.extension.internal.operation.outbox.postprocessors.OrderingByBusinessKeyPostProcessor
import com.mgmtp.a12.workflows.shared.dtos.MessageDto
import com.mgmtp.a12.workflows.shared.dtos.ProcessEngineVariable
import com.mgmtp.a12.workflows.shared.internal.constants.OutboxConstants.WORKFLOWS_OUTBOX_QUEUE
import org.springframework.jms.core.JmsTemplate

/**
 * Outbox producer using JmsTemplate to send messages to outbox.
 *
 * @deprecated Asynchronous mode is deprecated and will be removed in 2026.06.
 */
@Deprecated("Asynchronous mode is deprecated and will be removed in 2026.06.")
class ProcessEngineJmsProducer(
    private val jmsTemplate: JmsTemplate,
    private val orderingByBusinessKeyPostProcessor: OrderingByBusinessKeyPostProcessor,
) : ProcessEngineOperationBroker {

    override fun startProcessInstance(
        processDefinitionKey: String,
        businessKey: String,
        variables: Map<String, ProcessEngineVariable<*>>,
    ) {
        val outboxCommand = StartProcessOutboxCommand(processDefinitionKey, businessKey, variables)
        jmsTemplate.convertAndSend(WORKFLOWS_OUTBOX_QUEUE, outboxCommand, orderingByBusinessKeyPostProcessor)
    }

    override fun completeTask(taskId: String, businessKey: String, variables: Map<String, ProcessEngineVariable<*>>) {
        val outboxCommand = CompleteTaskOutboxCommand(taskId, businessKey)
        jmsTemplate.convertAndSend(WORKFLOWS_OUTBOX_QUEUE, outboxCommand, orderingByBusinessKeyPostProcessor)
    }

    override fun assign(taskId: String, businessKey: String, assignee: String) {
        val outboxCommand = AssignTaskOutboxCommand(taskId, businessKey, assignee)
        jmsTemplate.convertAndSend(WORKFLOWS_OUTBOX_QUEUE, outboxCommand, orderingByBusinessKeyPostProcessor)
    }

    override fun sendMessage(message: MessageDto) {
        val outboxCommand = SendMessageOutboxCommand(message)
        jmsTemplate.convertAndSend(WORKFLOWS_OUTBOX_QUEUE, outboxCommand, orderingByBusinessKeyPostProcessor)
    }
}
