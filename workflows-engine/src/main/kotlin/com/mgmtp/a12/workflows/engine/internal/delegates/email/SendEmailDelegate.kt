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

import com.mgmtp.a12.workflows.engine.internal.delegates.shared.DelegateParameters
import com.mgmtp.a12.workflows.engine.internal.delegates.shared.getDelegateStringInput
import com.mgmtp.a12.workflows.engine.internal.delegates.shared.getOrFail
import com.mgmtp.a12.workflows.engine.internal.util.getVariableAsStringOrEmpty
import com.mgmtp.a12.workflows.shared.logging.utils.logDelegateExecutionComplete
import com.mgmtp.a12.workflows.shared.logging.utils.logDelegateExecutionStart
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import io.vertx.ext.mail.MailClient
import org.cibseven.bpm.engine.delegate.BpmnError
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.cibseven.bpm.engine.delegate.JavaDelegate

const val SEND_EMAIL_ERROR_ID = "SendEmailError"

class SendEmailDelegate(
    private var sender: EmailSender,
    private var mailClient: MailClient,
) : JavaDelegate {

    private val delegateName = this::class.simpleName!!
    private val delegateInputs = SendEmailDelegateParameters().inputs
    private val recipientsVar = SendEmailDelegateParameters.InputVars.RECIPIENTS.label
    private val subjectVar = SendEmailDelegateParameters.InputVars.SUBJECT.label
    private val messageVar = SendEmailDelegateParameters.InputVars.MESSAGE.label
    private val ccVar = SendEmailDelegateParameters.InputVars.CC.label

    override fun execute(execution: DelegateExecution) {
        logger().logDelegateExecutionStart()

        val recipients = getRecipients(execution)
        val subject = getDelegateStringInput(
            execution,
            delegateName,
            subjectVar,
            delegateInputs[subjectVar],
        )
        val message = getDelegateStringInput(
            execution,
            delegateName,
            messageVar,
            delegateInputs[messageVar],
        )
        val cc = getDelegateStringInput(
            execution,
            delegateName,
            ccVar,
            delegateInputs[ccVar],
        )

        try {
            sender.sendEmail(mailClient, recipients, subject, message, cc)
            logger().logDelegateExecutionComplete()
        } catch (e: MailClientException) {
            throw BpmnError(SEND_EMAIL_ERROR_ID)
        }
    }

    private fun getRecipients(execution: DelegateExecution) = execution
        .getVariableAsStringOrEmpty(recipientsVar)
        .ifEmpty { execution.getOrFail("emailAddress", delegateName) }
}

class SendEmailDelegateParameters :
    DelegateParameters(
        mapOf(
            InputVars.RECIPIENTS.label to true,
            InputVars.SUBJECT.label to true,
            InputVars.MESSAGE.label to true,
            InputVars.CC.label to false,
        ),
    ) {
    enum class InputVars(val label: String) {
        RECIPIENTS("recipients"),
        SUBJECT("subject"),
        MESSAGE("message"),
        CC("cc"),
    }
}
