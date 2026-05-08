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
package com.mgmtp.a12.workflows.engine.internal.delegates.setstatus

import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.dataservices.document.exception.InvalidDocumentReferenceException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

private class SetStatusDelegateTest {

    val service: SetStatusService = mockk(relaxed = true)
    val delegate = SetStatusDelegate(service)

    val execution: DelegateExecution = mockk()

    val docRefVar = SetStatusDelegateParameters.InputVars.DOCUMENT_REFERENCE.label
    val newValueVar = SetStatusDelegateParameters.InputVars.NEW_VALUE.label
    val pathToFieldVar = SetStatusDelegateParameters.InputVars.PATH_TO_FIELD.label
    val repetitionsVar = SetStatusDelegateParameters.InputVars.REPETITIONS.label

    companion object {
        const val DOCREF = "Test/1"
        const val PATH = "/group1/group2/field1"
        const val NEW_VALUE = "newValue"
        const val REPETITIONS = "3,2,1"
        const val INVALID_REPETITIONS = "4, 3, 2, 1"
        val repetitionsArray = intArrayOf(3, 2, 1)
    }

    @BeforeEach
    fun setup() {
        every { execution.getVariable(docRefVar) } returns DOCREF
        every { execution.getVariable(pathToFieldVar) } returns PATH
        every { execution.getVariable(repetitionsVar) } returns REPETITIONS
        every { execution.getVariable(newValueVar) } returns NEW_VALUE
    }

    @Test
    fun `should call service if all not optional parameters are provided`() {
        // when/then
        shouldNotThrowAny { delegate.execute(execution) }
        verify(exactly = 1) { service.setStatus(DocumentReference(DOCREF), PATH, repetitionsArray, NEW_VALUE) }
    }

    @Test
    fun `should throw exception if given repetitions don't match given path`() {
        // given
        every { execution.getVariable(repetitionsVar) } returns INVALID_REPETITIONS

        // when/then
        shouldThrow<IllegalArgumentException> { delegate.execute(execution) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["documentReference", "pathToField", "newValue"])
    fun `should throw exception if a required parameter is missing`(parameterName: String) {
        // given
        every { execution.getVariable(parameterName) } returns null

        // when/then
        shouldThrow<IllegalArgumentException> { delegate.execute(execution) }
    }

    @Test
    fun `should throw exception if documentReference is invalid`() {
        // given
        every { execution.getVariable(docRefVar) } returns "INVALID_DOC_REF"

        // when/then
        shouldThrow<InvalidDocumentReferenceException> { delegate.execute(execution) }
    }

    @Nested
    @DisplayName("Given repetitions parameter is not set...")
    inner class GivenRepetitionsParameterNotSet {

        @BeforeEach
        fun setup() {
            every { execution.getVariable(repetitionsVar) } returns null
        }

        @Test
        fun `should use default repetition if repetitions parameter is missing`() {
            // given
            every { execution.getVariable(pathToFieldVar) } returns "/root"

            // when
            delegate.execute(execution)

            // then
            val expectedDefaultRepetitions = intArrayOf(1)
            verify(exactly = 1) { service.setStatus(any(), any(), expectedDefaultRepetitions, any()) }
        }

        @Test
        fun `should use default repetition if repetitions parameter is missing for longer path`() {
            // given
            every { execution.getVariable(pathToFieldVar) } returns "/root/g1/g2/g3/field"

            // when
            delegate.execute(execution)

            // then
            val expectedDefaultRepetitions = intArrayOf(1, 1, 1, 1, 1)
            verify(exactly = 1) { service.setStatus(any(), any(), expectedDefaultRepetitions, any()) }
        }
    }
}
