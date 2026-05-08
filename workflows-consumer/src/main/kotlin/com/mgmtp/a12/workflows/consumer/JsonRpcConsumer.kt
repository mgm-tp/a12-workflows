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
package com.mgmtp.a12.workflows.consumer

import com.mgmtp.a12.workflows.consumer.internal.commands.*
import com.mgmtp.a12.workflows.engine.client.internal.ProcessEngineClient
import com.mgmtp.a12.workflows.shared.internal.constants.OutboxConstants.WORKFLOWS_OUTBOX_QUEUE
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.springframework.jms.annotation.JmsListener

class JsonRpcConsumer(
    private val processEngineClient: ProcessEngineClient,
) {
    @JmsListener(destination = WORKFLOWS_OUTBOX_QUEUE)
    fun consume(outboxCommand: OutboxCommand) {
        when (outboxCommand) {
            is AssignTaskOutboxCommand -> consumeAssignTaskOutboxCommand(outboxCommand)
            is CompleteTaskOutboxCommand -> consumeCompleteTaskOutboxCommand(outboxCommand)
            is SendMessageOutboxCommand -> consumeSendMessageOutboxCommand(outboxCommand)
            is StartProcessOutboxCommand -> consumeStartProcessOutboxCommand(outboxCommand)
        }
    }

    private fun consumeAssignTaskOutboxCommand(assignTaskCommand: AssignTaskOutboxCommand) {
        logger().debug(
            "Starting to consume outbox command of type {}: {}",
            AssignTaskOutboxCommand::class.simpleName,
            assignTaskCommand,
        )

        val (taskId, businessKey, assignee) = assignTaskCommand
        processEngineClient.assign(taskId, businessKey, assignee)

        logger().debug(
            "Successfully assigned task [{}] to [{}] based on outbox command: {}",
            taskId,
            assignee,
            assignTaskCommand,
        )
    }

    private fun consumeCompleteTaskOutboxCommand(completeTaskCommand: CompleteTaskOutboxCommand) {
        logger().debug(
            "Starting to consume outbox command of type {}: {}",
            CompleteTaskOutboxCommand::class.simpleName,
            completeTaskCommand,
        )

        val (taskId, businessKey) = completeTaskCommand
        processEngineClient.completeTask(taskId, businessKey)

        logger().debug("Successfully completed task [{}] based on outbox command: {}", taskId, completeTaskCommand)
    }

    private fun consumeSendMessageOutboxCommand(sendMessageCommand: SendMessageOutboxCommand) {
        logger().debug(
            "Starting to consume outbox command of type {}: {}",
            SendMessageOutboxCommand::class.simpleName,
            sendMessageCommand,
        )

        processEngineClient.sendMessage(sendMessageCommand.message)

        val (_, businessKey, processInstanceId) = sendMessageCommand.message
        logger().debug(
            "Successfully sent message for businessKey [{}] and processInstanceId [{}] based on outbox command: {}",
            businessKey,
            processInstanceId,
            sendMessageCommand,
        )
    }

    private fun consumeStartProcessOutboxCommand(startProcessOutboxCommand: StartProcessOutboxCommand) {
        logger().debug(
            "Starting to consume outbox command of type: {}: {}",
            StartProcessOutboxCommand::class.simpleName,
            startProcessOutboxCommand,
        )

        logger().info("Requesting process engine to start instance of ${startProcessOutboxCommand.processDefinition}")
        val (processDefinition, businessKey, variables) = startProcessOutboxCommand
        processEngineClient.startProcessInstance(
            processDefinition,
            businessKey,
            variables,
        )
        logger().info("Successfully started instance of ${startProcessOutboxCommand.processDefinition}")
    }
}
