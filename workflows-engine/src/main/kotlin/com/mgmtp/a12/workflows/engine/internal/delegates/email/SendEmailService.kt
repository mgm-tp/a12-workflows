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

package com.mgmtp.a12.workflows.engine.internal.delegates.email

import com.mgmtp.a12.workflows.shared.logging.utils.logger
import io.vertx.ext.mail.MailClient
import io.vertx.ext.mail.MailMessage
import org.springframework.beans.factory.annotation.Value

data class MailClientException(val errorMessage: String) : RuntimeException(errorMessage)

class EmailSender {

    @Value("\${mgmtp.a12.workflows.engine.delegates.email.from}")
    private lateinit var emailFrom: String

    fun sendEmail(mailClient: MailClient, recipients: String, theSubject: String, message: String, theCc: String = "") {
        val mailMessage = MailMessage().apply {
            from = emailFrom
            to = recipients.parseRecipients()
            subject = theSubject
            text = message
            if (theCc.isNotEmpty()) {
                cc = theCc.parseRecipients()
            }
        }

        logger().info("Attempting to send notification email.")
        logger().debug("Attempting to send email to $recipients...")

        mailClient.sendMail(mailMessage) { result ->
            if (result.succeeded()) {
                logger().info("Notification email is sent.")
                logger().debug("Notification email sent to $recipients and the Cc $theCc")
            } else {
                logger().warn("Could not send notification email.")
                logger().debug("Could not send email to $recipients")
                throw MailClientException("Could not send email to $recipients: ${result.cause()}")
            }
        }
    }

    private fun String.parseRecipients(): List<String> = this
        .split(",")
        .map { it.trim() }
}
