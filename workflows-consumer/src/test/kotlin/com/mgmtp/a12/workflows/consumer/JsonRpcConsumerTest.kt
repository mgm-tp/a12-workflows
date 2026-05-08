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

import com.mgmtp.a12.workflows.consumer.JsonRpcConsumerTest.TestFixtures.assignTaskCommand
import com.mgmtp.a12.workflows.consumer.JsonRpcConsumerTest.TestFixtures.completeTaskCommand
import com.mgmtp.a12.workflows.consumer.JsonRpcConsumerTest.TestFixtures.sendMessageCommand
import com.mgmtp.a12.workflows.consumer.JsonRpcConsumerTest.TestFixtures.startProcessOutboxCommand
import com.mgmtp.a12.workflows.consumer.internal.commands.AssignTaskOutboxCommand
import com.mgmtp.a12.workflows.consumer.internal.commands.CompleteTaskOutboxCommand
import com.mgmtp.a12.workflows.consumer.internal.commands.SendMessageOutboxCommand
import com.mgmtp.a12.workflows.consumer.internal.commands.StartProcessOutboxCommand
import com.mgmtp.a12.workflows.engine.client.internal.ProcessEngineClient
import com.mgmtp.a12.workflows.shared.dtos.MessageDto
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class JsonRpcConsumerTest {
    private val mockProcessEngineClient: ProcessEngineClient = spyk()

    // Subject under test
    private val jsonRpcConsumer = JsonRpcConsumer(mockProcessEngineClient)

    object TestFixtures {
        val assignTaskCommand = AssignTaskOutboxCommand("someTaskId", "someBusinessKey", "someAssignee")
        val completeTaskCommand = CompleteTaskOutboxCommand("someTaskId", "someBusinessKey")
        val sendMessageCommand =
            SendMessageOutboxCommand(MessageDto("someMessageName", "someBusinessKey", "someProcessInstanceId"))
        val startProcessOutboxCommand = StartProcessOutboxCommand("someProcess", "someBusinessKey", emptyMap())
    }

    @BeforeEach
    fun setupMocks() {
        every { mockProcessEngineClient.assign(any(), any(), any()) } just Runs
        every { mockProcessEngineClient.completeTask(any(), any()) } just Runs
    }

    @Test
    fun `should send task assign request to Process Engine`() {
        // when
        jsonRpcConsumer.consume(assignTaskCommand)

        // then
        verify(
            exactly = 1,
        ) { mockProcessEngineClient.assign(assignTaskCommand.taskId, assignTaskCommand.businessKey, assignTaskCommand.assignee) }
    }

    @Test
    fun `should send task complete request to Process Engine`() {
        // when
        jsonRpcConsumer.consume(completeTaskCommand)

        // then
        verify(exactly = 1) { mockProcessEngineClient.completeTask(completeTaskCommand.taskId, completeTaskCommand.businessKey) }
    }

    @Test
    fun `should send message to Process Engine`() {
        // when
        jsonRpcConsumer.consume(sendMessageCommand)

        // then
        verify(exactly = 1) { mockProcessEngineClient.sendMessage(sendMessageCommand.message) }
    }

    @Test
    fun `should send start process request to Process Engine`() {
        // when
        jsonRpcConsumer.consume(startProcessOutboxCommand)

        // then
        verify(exactly = 1) {
            mockProcessEngineClient.startProcessInstance(
                startProcessOutboxCommand.processDefinition,
                startProcessOutboxCommand.businessKey,
                startProcessOutboxCommand.variables,
            )
        }
    }
}
