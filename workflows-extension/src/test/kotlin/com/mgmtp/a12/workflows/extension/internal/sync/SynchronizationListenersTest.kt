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

package com.mgmtp.a12.workflows.extension.internal.sync

import com.mgmtp.a12.dataservices.document.DataServicesDocument
import com.mgmtp.a12.dataservices.document.events.DocumentAfterUpdateEvent
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.DocumentV2
import com.mgmtp.a12.workflows.extension.testFixtures.testUtils.deserializeDocumentV2
import com.mgmtp.a12.workflows.shared.functions.testUtils.readResource
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.*

private class SynchronizationListenersTest {

    val mockSynchronizationHandler = mockk<SynchronizationHandler>()
    val spySynchronizationListeners = spyk(SynchronizationListeners(mockSynchronizationHandler), recordPrivateCalls = true)

    val mockEvent = mockk<DocumentAfterUpdateEvent>()
    val mockOldDocument = mockk<DataServicesDocument>()
    val mockNewDocument = mockk<DataServicesDocument>()
    val mockDocumentV2 = mockk<DocumentV2>()

    @BeforeEach
    fun setup() {
        every { mockOldDocument.kernelDocument } returns mockDocumentV2
        every { mockEvent.oldDocument } returns mockOldDocument
        every { mockEvent.newDocument } returns mockOldDocument
        justRun { mockSynchronizationHandler.propagateValues(any(), any()) }
    }

    @Test
    fun `should call handler if task id is present`() {
        // given
        every { spySynchronizationListeners["getTaskId"](any<DocumentV2>()) } returns "1234"

        // when
        spySynchronizationListeners.onModifyDocument(mockEvent)

        // then
        verify(exactly = 1) { mockSynchronizationHandler.propagateValues(any(), any()) }
    }

    @Test
    fun `should not call handler if task id is missing`() {
        // given
        every { spySynchronizationListeners["getTaskId"](any<DocumentV2>()) } returns null

        // when
        spySynchronizationListeners.onModifyDocument(mockEvent)

        // then
        verify(exactly = 0) { mockSynchronizationHandler.propagateValues(any(), any()) }
    }

    @Test
    fun `should not call handler if task ids differ`() {
        // given
        every { mockEvent.newDocument } returns mockNewDocument
        every { spySynchronizationListeners["getTaskId"](mockOldDocument.kernelDocument) } returns "1234"
        every { spySynchronizationListeners["getTaskId"](mockNewDocument.kernelDocument) } returns "2345"

        // when
        spySynchronizationListeners.onModifyDocument(mockEvent)

        // then
        verify(exactly = 0) { mockSynchronizationHandler.propagateValues(any(), any()) }
    }

    @Nested
    @DisplayName("Test getTaskId")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TestGetTaskId {

        val documentModelName = "Person_DM"
        val documentName = "taskDocument"
        val expectedTaskId = "8a6bd517-ec0e-11e8-8b7a-34e12db78816"

        @Test
        fun `should get taskId in document`() {
            // given
            val taskDocument = readResource<String>(this::class, "/requests/onComplete/$documentName.json")
            val documentModelString = readResource<String>(this::class, "/models/documentModels/$documentModelName.json")
            val document = deserializeDocumentV2(taskDocument, documentModelString, documentModelName)

            // Use reflection to access the private method
            val method = SynchronizationListeners::class.java.getDeclaredMethod("getTaskId", DocumentV2::class.java)
            method.isAccessible = true

            // when
            val taskId = method.invoke(spySynchronizationListeners, document) as String?

            // then
            taskId shouldBe expectedTaskId
        }
    }
}
