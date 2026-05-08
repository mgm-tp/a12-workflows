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
package com.mgmtp.a12.workflows.engine.internal.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mgmtp.a12.workflows.engine.internal.defaultDateFormat
import com.mgmtp.a12.workflows.engine.internal.util.MetadataRetrieverTest.TestConstants.INITIATOR
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.cibseven.bpm.engine.RepositoryService
import org.cibseven.bpm.engine.RuntimeService
import org.cibseven.bpm.engine.delegate.DelegateTask
import org.cibseven.bpm.model.bpmn.BpmnModelInstance
import org.cibseven.bpm.model.bpmn.instance.Task
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.format.DateTimeFormatter
import java.util.*

private class MetadataRetrieverTest {

    val repositoryServiceMock: RepositoryService = mockk(relaxed = true)
    val runtimeServiceMock: RuntimeService = mockk(relaxed = true)
    val delegateTaskMock: DelegateTask = mockk(relaxed = true)
    val bpmnModelMock: BpmnModelInstance = mockk(relaxed = true)
    val bpmnTaskMock: Task = mockk(relaxed = true)
    val objectMapper = jacksonObjectMapper().apply { dateFormat = defaultDateFormat }

    val metadataRetriever = MetadataRetriever(repositoryServiceMock, runtimeServiceMock, objectMapper)

    object TestConstants {
        const val INITIATOR = "someUserName"
    }

    @BeforeEach
    fun setup() {
        every { bpmnModelMock.getModelElementById<Task>(any()) } returns bpmnTaskMock
        every { repositoryServiceMock.getBpmnModelInstance(any()) } returns bpmnModelMock
        every { runtimeServiceMock.getVariable(any(), "initiator") } returns INITIATOR

        // What we're actually interested in: a date value
        every { delegateTaskMock.createTime } returns Date()
    }

    @Test
    fun `should use date formatter from object mapper`() {
        // given
        val expectedFormat = DateTimeFormatter.ofPattern(defaultDateFormat.toPattern())

        // when
        val metadata = metadataRetriever.retrieveMetadata(delegateTaskMock)

        // then
        shouldNotThrowAny { expectedFormat.parse(metadata.task.creationDate) }
    }

    @Test
    fun `should pick up initiator as process variable`() {
        // given
        val metaData = metadataRetriever.retrieveMetadata(delegateTaskMock)

        // then
        metaData.process.initiator shouldBe INITIATOR
    }
}
