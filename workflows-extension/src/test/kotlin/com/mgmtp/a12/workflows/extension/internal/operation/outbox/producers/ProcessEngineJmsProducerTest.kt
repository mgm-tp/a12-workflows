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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.mgmtp.a12.workflows.consumer.internal.commands.AssignTaskOutboxCommand
import com.mgmtp.a12.workflows.consumer.internal.commands.CompleteTaskOutboxCommand
import com.mgmtp.a12.workflows.consumer.internal.commands.SendMessageOutboxCommand
import com.mgmtp.a12.workflows.extension.internal.operation.outbox.postprocessors.OrderingByBusinessKeyPostProcessor
import com.mgmtp.a12.workflows.shared.dtos.MessageDto
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPath.RELATIVE_BUSINESS_KEY_PATH
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPath.RELATIVE_TASK_ID_PATH
import com.mgmtp.a12.workflows.shared.internal.constants.OutboxConstants.WORKFLOWS_OUTBOX_QUEUE
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jms.core.JmsTemplate

private class ProcessEngineJmsProducerTest {

    val mockJmsTemplate: JmsTemplate = mockk(relaxed = true)

    val mockOrderingByBusinessKeyPostProcessor: OrderingByBusinessKeyPostProcessor = mockk()

    // Subject under test
    val processEngineJmsProducer = ProcessEngineJmsProducer(mockJmsTemplate, mockOrderingByBusinessKeyPostProcessor)

    companion object {
        val taskDocumentString = this::class.java.getResource("/documents/validDocument.json")
        val taskDocument = jacksonObjectMapper().readValue<JsonNode>(taskDocumentString)
        val taskId: String = taskDocument.at(RELATIVE_TASK_ID_PATH).asText()
        val businessKey: String = taskDocument.at(RELATIVE_BUSINESS_KEY_PATH).asText()
        val newAssignee = "newAssignee"
        val message = MessageDto("someMessageName", "someBusinessKey", "someProcessInstanceId")
    }

    @BeforeEach
    fun setupMocks() {
        every { mockJmsTemplate.convertAndSend(WORKFLOWS_OUTBOX_QUEUE, any(), any()) } just Runs
    }

    @Test
    fun `should send AssignTaskCommand with correct inputs by JmsTemplate`() {
        // when
        processEngineJmsProducer.assign(taskId, businessKey, newAssignee)

        // then
        val assignTaskCommand = AssignTaskOutboxCommand(taskId, businessKey, "newAssignee")
        verify(exactly = 1) { mockJmsTemplate.convertAndSend(WORKFLOWS_OUTBOX_QUEUE, assignTaskCommand, mockOrderingByBusinessKeyPostProcessor) }
    }

    @Test
    fun `should send CompleteTaskCommand with correct inputs by JmsTemplate`() {
        // when
        processEngineJmsProducer.completeTask(taskId, businessKey)

        // then
        val completeTaskCommand = CompleteTaskOutboxCommand(taskId, businessKey)
        verify(exactly = 1) { mockJmsTemplate.convertAndSend(WORKFLOWS_OUTBOX_QUEUE, completeTaskCommand, mockOrderingByBusinessKeyPostProcessor) }
    }

    @Test
    fun `should send SendMessageCommand with correct inputs by JmsTemplate`() {
        // when
        processEngineJmsProducer.sendMessage(message)

        // then
        val sendMessageCommand = SendMessageOutboxCommand(message)
        verify(exactly = 1) { mockJmsTemplate.convertAndSend(WORKFLOWS_OUTBOX_QUEUE, sendMessageCommand, mockOrderingByBusinessKeyPostProcessor) }
    }
}
