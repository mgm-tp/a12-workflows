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
package com.mgmtp.a12.workflows.extension.internal.operation.outbox.postprocessors

import com.mgmtp.a12.workflows.consumer.internal.commands.AssignTaskOutboxCommand
import com.mgmtp.a12.workflows.consumer.internal.commands.CompleteTaskOutboxCommand
import com.mgmtp.a12.workflows.consumer.internal.commands.OutboxCommand
import com.mgmtp.a12.workflows.consumer.internal.commands.SendMessageOutboxCommand
import com.mgmtp.a12.workflows.consumer.internal.commands.StartProcessOutboxCommand
import com.mgmtp.a12.workflows.extension.internal.operation.outbox.JmsConstants
import com.mgmtp.a12.workflows.shared.dtos.MessageDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.jms.Message
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

private class OrderingByBusinessKeyPostProcessorTest {

    val mockMessage: Message = mockk(relaxed = true)

    // Subject under test
    val postProcessor = OrderingByBusinessKeyPostProcessor()

    companion object {

        @JvmStatic
        fun outboxCommandProvider() = setOf(
            StartProcessOutboxCommand("someProcessDefinition", "someBusinessKey", emptyMap()),
            AssignTaskOutboxCommand("someTaskId", "someBusinessKey", "someAssignee"),
            CompleteTaskOutboxCommand("someTaskId", "someBusinessKey"),
            SendMessageOutboxCommand(MessageDto("someMessageName", "someBusinessKey", "someProcessInstanceId")),
        )
    }

    @ParameterizedTest
    @MethodSource("outboxCommandProvider")
    fun `should set correct message group header`(outboxCommand: OutboxCommand) {
        // given
        every { mockMessage.getBody(OutboxCommand::class.java) } returns outboxCommand

        // when
        postProcessor.postProcessMessage(mockMessage)

        // then
        verify(exactly = 1) { mockMessage.setStringProperty(JmsConstants.JMS_GROUP_HEADER, outboxCommand.businessKey) }
    }

    @ParameterizedTest
    @MethodSource("outboxCommandProvider")
    fun `should set timestamp header`(outboxCommand: OutboxCommand) {
        // given
        every { mockMessage.getBody(OutboxCommand::class.java) } returns outboxCommand

        // when
        postProcessor.postProcessMessage(mockMessage)

        // then
        verify(exactly = 1) { mockMessage.setLongProperty(JmsConstants.TIMESTAMP_HEADER, any()) }
    }
}
