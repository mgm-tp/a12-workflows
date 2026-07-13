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

import com.mgmtp.a12.workflows.engine.internal.delegates.api.DelegateBeanNames
import io.vertx.core.Vertx
import io.vertx.ext.mail.LoginOption
import io.vertx.ext.mail.MailClient
import io.vertx.ext.mail.MailConfig
import io.vertx.ext.mail.StartTLSOptions
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["mgmtp.a12.workflows.engine.delegates.email.host"])
class MailConfiguration {

    @Bean(DelegateBeanNames.SEND_EMAIL_DELEGATE)
    fun sendEmailDelegate(emailSender: EmailSender, mailClient: MailClient) = SendEmailDelegate(emailSender, mailClient)

    @Bean
    fun emailSender(): EmailSender = EmailSender()

    @Bean
    fun mailClient(emailConfig: EmailConfig): MailClient = MailClient.create(
        Vertx.vertx(),
        MailConfig().apply {
            hostname = emailConfig.host
            port = emailConfig.port
            login = if (emailConfig.login.enabled) LoginOption.REQUIRED else LoginOption.DISABLED
            starttls = if (emailConfig.startTls.enabled) StartTLSOptions.REQUIRED else StartTLSOptions.DISABLED
            username = emailConfig.user
            password = emailConfig.password
            keepAliveTimeout = emailConfig.keepAliveTimeout
        },
    )
}

@ConfigurationProperties(prefix = "mgmtp.a12.workflows.engine.delegates.email")
// Could not find a nicer way to have these optional so the condition is repeated here
@ConditionalOnProperty(name = ["mgmtp.a12.workflows.engine.delegates.email.host"])
class EmailConfig(
    val host: String,
    val port: Int,
    val user: String,
    val password: String,
    val startTls: StartTls,
    val login: Login,
    val keepAliveTimeout: Int,
)

data class StartTls(val enabled: Boolean)

data class Login(val enabled: Boolean)
