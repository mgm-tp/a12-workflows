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
package com.mgmtp.a12.workflows.engine.internal.delegates.document

import com.mgmtp.a12.workflows.engine.internal.util.ProcessEngineVariableNames
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.*
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.junit.jupiter.api.Test

private class CreateDocumentDelegateTest {

    val service: CreateDocumentService = mockk(relaxed = true)
    val delegate = CreateDocumentDelegate(service)
    val execution: DelegateExecution = mockk()
    val documentModelNameVar = CreateDocumentDelegateParameters.InputVars.DOCUMENT_MODEL_NAME.label
    val documentModelName = "Test_DM"

    @Test
    fun `should call service and set variable if doc model name is provided`() {
        // given
        every { execution.getVariable(documentModelNameVar) } returns documentModelName
        every { execution.setVariableLocal(any(), any()) } just runs

        // when/then
        shouldNotThrowAny { delegate.execute(execution) }
        verify(exactly = 1) { service.createDocument(documentModelName) }
        verify(exactly = 1) { execution.setVariableLocal(ProcessEngineVariableNames.NEW_DOC_REF_VARIABLE, any()) }
    }

    @Test
    fun `should throw exception if doc model name is missing`() {
        // given
        every { execution.getVariable(documentModelNameVar) } returns null

        // when/then
        shouldThrow<IllegalArgumentException> { delegate.execute(execution) }
    }
}
