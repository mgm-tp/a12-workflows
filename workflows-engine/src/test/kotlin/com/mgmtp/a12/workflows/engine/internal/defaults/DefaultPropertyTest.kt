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
package com.mgmtp.a12.workflows.engine.internal.defaults

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.core.io.ClassPathResource
import java.util.*

private class DefaultPropertyTest {

    companion object {
        val configProps = Properties().apply { load(ClassPathResource("process-engine-defaults.properties").inputStream) }

        @JvmStatic
        fun expectedProperties() = listOf<Arguments>(
            Arguments.of("camunda.bpm.admin-user.id", null),
            Arguments.of("camunda.bpm.admin-user.password", null),
            Arguments.of("spring.jersey.application-path", "engine-rest"),

            // Camunda's default history level is decreased to avoid huge database size
            Arguments.of("camunda.bpm.history-level-default", "AUDIT"),
            Arguments.of("camunda.bpm.history-level", "AUDIT"),

            // Default date format must match A12 date format
            Arguments.of("spring.jackson.date-format", "yyyy-MM-dd'T'HH:mm:ss"),

            // Logging should be off due to GDPR
            Arguments.of("logging.level.root", "OFF"),

            // Actuator should be off by default for security
            // Not working for now as CIB 7 sets this to true, but it is not part of our configuration -> not tested
//            Arguments.of("management.endpoints.access.default", "none"),
            Arguments.of("management.health.ldap.enabled", "false"),
        )
    }

    @ParameterizedTest
    @MethodSource("expectedProperties")
    fun `should set desired default properties`(propertyUnderTest: String, expectedValue: String?) {
        // then
        configProps.getProperty(propertyUnderTest) shouldBe expectedValue
    }
}
