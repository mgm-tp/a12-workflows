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
package com.mgmtp.a12.workflows.engine.internal.delegates.email

import com.mgmtp.a12.workflows.engine.internal.delegates.email.SendEmailDelegateTest.EmailDelegateParameters.message
import com.mgmtp.a12.workflows.engine.internal.delegates.email.SendEmailDelegateTest.EmailDelegateParameters.recipients
import com.mgmtp.a12.workflows.engine.internal.delegates.email.SendEmailDelegateTest.EmailDelegateParameters.subject
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.vertx.ext.mail.MailClient
import org.cibseven.bpm.engine.delegate.BpmnError
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

private class SendEmailDelegateTest {

    val expectedError = "Error trying to read required inputs for SendEmailDelegate."

    val mailClientMock: MailClient = mockk()
    val emailSender: EmailSender = mockk()

    lateinit var execution: DelegateExecution
    lateinit var sendEmailDelegate: SendEmailDelegate

    val recipientsVar = SendEmailDelegateParameters.InputVars.RECIPIENTS.label
    val subjectVar = SendEmailDelegateParameters.InputVars.SUBJECT.label
    val messageVar = SendEmailDelegateParameters.InputVars.MESSAGE.label
    val ccVar = SendEmailDelegateParameters.InputVars.CC.label

    val emailAddress = "sampletorecipient@example.com"
    val cc = "sampletorecipient3@example.com"
    val severalCc = "$cc, sampletorecipient4@example.com"

    object EmailDelegateParameters {
        val recipients = "sampletorecipient1@example.com, sampletorecipient2@example.com"
        val subject = "test subject"
        val message = "test message"
    }

    @BeforeEach
    fun setup() {
        execution = mockk()
        sendEmailDelegate = SendEmailDelegate(emailSender, mailClientMock)
        justRun { emailSender.sendEmail(any(), any(), any(), any(), any()) }
        every { execution.getVariable(recipientsVar) } returns recipients
        every { execution.getVariable(subjectVar) } returns subject
        every { execution.getVariable(messageVar) } returns message
        every { execution.getVariable("emailAddress") } returns null
        every { execution.getVariable(ccVar) } returns null
    }

    @Test
    fun `should pass if all parameters are set`() {
        // when/then
        shouldNotThrowAny { sendEmailDelegate.execute(execution) }
        verify(exactly = 1) { emailSender.sendEmail(any(), recipients, subject, message) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["recipients", "subject", "message"])
    fun `should throw exception if one of the required parameters is not set`(variableName: String) {
        // given
        every { execution.getVariable(variableName) } returns null

        // when/then
        val exception = shouldThrow<IllegalArgumentException> { sendEmailDelegate.execute(execution) }
        exception.message shouldBe expectedError
    }

    @Test
    fun `should use 'emailAddress' if 'recipients' parameter is null`() {
        // given
        every { execution.getVariable(recipientsVar) } returns null
        every { execution.getVariable("emailAddress") } returns emailAddress

        // when/then
        shouldNotThrowAny { sendEmailDelegate.execute(execution) }
        verify(exactly = 1) { emailSender.sendEmail(any(), emailAddress, any(), any()) }
    }

    @Test
    fun `should use 'recipients' if both parameters are available`() {
        // given
        val expectedRecipients = recipients
        every { execution.getVariable("emailAddress") } returns emailAddress

        // when/then
        shouldNotThrowAny { sendEmailDelegate.execute(execution) }
        verify(exactly = 1) { emailSender.sendEmail(any(), expectedRecipients, any(), any()) }
    }

    @Test
    fun `should throw Bpmn Error if send email request fails`() {
        // given
        every { emailSender.sendEmail(any(), any(), any(), any()) } throws (MailClientException("someErrorMessage"))

        // then
        val thrown = shouldThrow<BpmnError> { sendEmailDelegate.execute(execution) }
        thrown.errorCode shouldBe SEND_EMAIL_ERROR_ID
    }

    @Test
    fun `should pass if cc is empty`() {
        // when/then
        shouldNotThrowAny { sendEmailDelegate.execute(execution) }
        verify(exactly = 1) { emailSender.sendEmail(any(), any(), any(), any(), "") }
    }

    @Test
    fun `should pass if one cc is given`() {
        // given
        every { execution.getVariable(ccVar) } returns cc

        // then
        shouldNotThrowAny { sendEmailDelegate.execute(execution) }
        verify(exactly = 1) { emailSender.sendEmail(any(), any(), any(), any(), cc) }
    }

    @Test
    fun `should pass if several ccs are given`() {
        // given
        every { execution.getVariable(ccVar) } returns severalCc

        // when/then
        shouldNotThrowAny { sendEmailDelegate.execute(execution) }
        verify(exactly = 1) { emailSender.sendEmail(any(), any(), any(), any(), severalCc) }
    }
}
