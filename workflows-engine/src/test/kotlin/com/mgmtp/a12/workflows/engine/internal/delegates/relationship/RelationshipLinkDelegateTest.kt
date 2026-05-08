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
package com.mgmtp.a12.workflows.engine.internal.delegates.relationship

import com.mgmtp.a12.workflows.engine.internal.data.LinkTestData
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import org.cibseven.bpm.engine.ProcessEngineException
import org.cibseven.bpm.engine.delegate.BpmnError
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.IOException
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
private class RelationshipLinkDelegateTest {

    val expectedError = "Error trying to read required inputs for RelationshipLinkDelegate."
    val mockkRelationshipLinkService: RelationshipLinkService = mockk()
    lateinit var relationshipLinkDelegate: RelationshipLinkDelegate
    lateinit var mockkExecution: DelegateExecution
    val relationshipModelVar = RelationshipLinkDelegateParameters.InputVars.RELATIONSHIP_MODEL.label
    val sourceRoleVar = RelationshipLinkDelegateParameters.InputVars.SOURCE_ROLE.label
    val sourceDocRefVar = RelationshipLinkDelegateParameters.InputVars.SOURCE_DOC_REF.label
    val targetRoleVar = RelationshipLinkDelegateParameters.InputVars.TARGET_ROLE.label
    val targetDocRefVar = RelationshipLinkDelegateParameters.InputVars.TARGET_DOC_REF.label

    @BeforeEach
    fun setup() {
        relationshipLinkDelegate = RelationshipLinkDelegate(mockkRelationshipLinkService)
        mockkExecution = mockk()
        every { mockkRelationshipLinkService.createLink(any()) } returns "1"
        every { mockkExecution.setVariableLocal(any(), any()) } just Runs
    }

    @AfterEach
    fun clear() {
        clearAllMocks(currentThreadOnly = true)
    }

    fun initializeExecution(createRelationshipDelegateParams: () -> Stream<Arguments>) {
        // Avoid MockkException if it is tried to get a variable that is not defined inside createRelationshipDelegateParams function.
        every { mockkExecution.getVariable(any()) } answers { null }

        for (parameter in createRelationshipDelegateParams()) {
            val name: String = parameter.get()[0].toString()
            val value: String = parameter.get()[1].toString()

            every { mockkExecution.getVariable(name) } answers { value }
        }
    }

    @ParameterizedTest
    @MethodSource("createRelationshipDelegateParamsWithoutDocRef")
    fun `should fail if one of the parameters is not set`(input: String, value: String) {
        // given
        initializeExecution { createRelationshipDelegateParamsWithDocRef() }
        every { mockkExecution.getVariable(input) } answers { null }

        // when/then
        val exception = shouldThrow<IllegalArgumentException> {
            relationshipLinkDelegate.execute(mockkExecution)
        }
        exception.message shouldBe expectedError
    }

    @Test
    fun `should fail if docRef is not set`() {
        // given
        initializeExecution { createRelationshipDelegateParamsWithoutDocRef() }

        // when/then
        val exception = shouldThrow<IllegalArgumentException> { relationshipLinkDelegate.execute(mockkExecution) }
        exception.message?.lowercase() shouldContain "error trying to read required inputs for"
    }

    @Test
    fun `should pass when all necessary parameters with docRef is set`() {
        // given
        initializeExecution { createRelationshipDelegateParamsWithDocRef() }

        // when/then
        shouldNotThrowAny { relationshipLinkDelegate.execute(mockkExecution) }
        verify(exactly = 1) { mockkRelationshipLinkService.createLink(any()) }
    }

    @Test
    fun `should throw Bpmn Error if relationship link request fails`() {
        // given
        initializeExecution { createRelationshipDelegateParamsWithDocRef() }
        every { mockkRelationshipLinkService.createLink(any()) } throws (IOException())

        // when/then
        val thrown = shouldThrow<BpmnError> { relationshipLinkDelegate.execute(mockkExecution) }
        thrown.errorCode shouldBe LINKAGE_ERROR_ID
    }

    @Test
    fun `should throw Bpmn Error of type LinkLimitReachedException if the limit is reached`() {
        // given
        initializeExecution { createRelationshipDelegateParamsWithDocRef() }
        every { mockkRelationshipLinkService.createLink(any()) } throws (LinkLimitReachedException("someErrorMessage"))

        // when/then
        val thrown = shouldThrow<BpmnError> { relationshipLinkDelegate.execute(mockkExecution) }
        thrown.errorCode shouldBe LIMIT_REACHED_ID
    }

    @Test
    fun `should set newLinkId in local variables of the execution`() {
        // given
        initializeExecution { createRelationshipDelegateParamsWithDocRef() }

        // when/then
        shouldNotThrowAny { relationshipLinkDelegate.execute(mockkExecution) }
        verify(exactly = 1) { mockkExecution.setVariableLocal("newLinkId", any()) }
    }

    @Test
    fun `should set newLinkId in local variables of the execution with value null after LinkLimitReachedException`() {
        // given
        every { mockkRelationshipLinkService.createLink(any()) } throws LinkLimitReachedException("")
        initializeExecution { createRelationshipDelegateParamsWithDocRef() }

        // when/then
        shouldThrow<ProcessEngineException> { relationshipLinkDelegate.execute(mockkExecution) }
        verify(exactly = 1) { mockkExecution.setVariableLocal("newLinkId", null) }
    }

    @Test
    fun `should set newLinkId in local variables of the execution with value null after IOException`() {
        // given
        every { mockkRelationshipLinkService.createLink(any()) } throws IOException()
        initializeExecution { createRelationshipDelegateParamsWithDocRef() }

        // when/then
        shouldThrow<BpmnError> { relationshipLinkDelegate.execute(mockkExecution) }
        verify(exactly = 1) { mockkExecution.setVariableLocal("newLinkId", null) }
    }

    /*
     * Initializers
     * */

    fun createRelationshipDelegateParamsWithDocRef(): Stream<Arguments> = Stream.of(
        Arguments.of(relationshipModelVar, LinkTestData.RELATIONSHIP_MODEL),
        Arguments.of(sourceDocRefVar, LinkTestData.SOURCE_DOC_REF),
        Arguments.of(targetDocRefVar, LinkTestData.TARGET_DOC_REF),
        Arguments.of(sourceRoleVar, LinkTestData.SOURCE_ROLE),
        Arguments.of(targetRoleVar, LinkTestData.TARGET_ROLE),
    )

    fun createRelationshipDelegateParamsWithoutDocRef(): Stream<Arguments> = Stream.of(
        Arguments.of(relationshipModelVar, LinkTestData.RELATIONSHIP_MODEL),
        Arguments.of(sourceRoleVar, LinkTestData.SOURCE_ROLE),
        Arguments.of(targetRoleVar, LinkTestData.TARGET_ROLE),
    )
}
