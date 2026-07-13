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

import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.dataservices.document.persistence.internal.DefaultDocumentModelResolver
import com.mgmtp.a12.dataservices.rpc.RpcException
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.GET_PROCESS_VARIABLE_UPDATES_OPERATION
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcExceptionHandler
import com.mgmtp.a12.workflows.extension.internal.sync.WorkflowsDocumentModelWalker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import com.mgmtp.a12.dataservices.document.DocumentService as DataServicesDocumentService

private class GetProcessVariableUpdatesOperationTest {

    val mockDocumentModelResolver: DefaultDocumentModelResolver = mockk(relaxed = true)
    val mockDocumentService: DataServicesDocumentService = mockk(relaxed = true)
    val workflowsDocumentModelWalker = WorkflowsDocumentModelWalker(mockDocumentModelResolver)

    // Subject under test
    val getProcessVariableUpdatesOperation = GetProcessVariableUpdatesOperation(
        mockDocumentModelResolver,
        mockDocumentService,
        workflowsDocumentModelWalker,
        WorkflowsRpcExceptionHandler(jacksonObjectMapper()),
    )

    @Test
    fun `should throw RpcException with correct payload if GET_PROCESS_VARIABLE_UPDATES_OPERATION fails`() {
        // given
        val someDocumentReferences = listOf(DocumentReference("SomeModel/1"))
        every { mockDocumentModelResolver.getDocumentModelById(any()) } throws Exception()

        // when/then
        val exception = shouldThrow<RpcException> {
            getProcessVariableUpdatesOperation.rpc(someDocumentReferences)
        }

        // then
        exception.operationError.operationId shouldBe GET_PROCESS_VARIABLE_UPDATES_OPERATION
        exception.message?.lowercase() shouldContain "could not get annotated fields"
    }
}
