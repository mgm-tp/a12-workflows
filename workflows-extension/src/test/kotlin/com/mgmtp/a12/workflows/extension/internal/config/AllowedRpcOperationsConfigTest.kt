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

package com.mgmtp.a12.workflows.extension.internal.config

import com.mgmtp.a12.workflows.extension.internal.config.AllowedRpcOperationsConfig.Companion.withAutomaticallyAllowedOperations
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jakarta.servlet.ServletContext
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MapPropertySource

// Deliberately not taken from the production code.
private const val AUTOMATICALLY_ALLOWED_OPERATIONS = "CREATE_DOCUMENT,CREATE_METADATA,REMOVE_METADATA,SET_STATUS"

private const val PROPERTY = "mgmtp.a12.dataservices.json-rpc.allowed-operations"

private const val INIT_PARAMETER = "mgmtp.a12.dataservices.jsonRpc.allowedOperations"

class AllowedRpcOperationsConfigTest {

    @Test
    fun `should contain exactly the mandatory operations and SET_STATUS`() {
        AllowedRpcOperationsConfig.AUTOMATICALLY_ALLOWED_OPERATIONS shouldContainExactly AUTOMATICALLY_ALLOWED_OPERATIONS.split(",")
    }

    @Test
    fun `should append the automatically allowed operations to the configured ones in their order`() {
        withAutomaticallyAllowedOperations(listOf("START_PROCESS", "A12_INTERNAL_OPERATIONS")) shouldBe
            "START_PROCESS,A12_INTERNAL_OPERATIONS,$AUTOMATICALLY_ALLOWED_OPERATIONS"
    }

    @Test
    fun `should leave a single wildcard untouched`() {
        withAutomaticallyAllowedOperations(listOf("*")).shouldBeNull()
        withAutomaticallyAllowedOperations(listOf(" * ", "")).shouldBeNull()
    }

    @Test
    fun `should append the automatically allowed operations if the wildcard is combined with other operations`() {
        // Data Services gives '*' no special meaning in that case.
        withAutomaticallyAllowedOperations(listOf("*", "START_PROCESS")) shouldBe "*,START_PROCESS,$AUTOMATICALLY_ALLOWED_OPERATIONS"
    }

    @Test
    fun `should match operation names exactly instead of by substring`() {
        withAutomaticallyAllowedOperations(listOf("MY_CREATE_METADATA", "SET_STATUS_LEGACY")) shouldBe
            "MY_CREATE_METADATA,SET_STATUS_LEGACY,$AUTOMATICALLY_ALLOWED_OPERATIONS"
    }

    @Test
    fun `should not duplicate operations that are already allowed`() {
        withAutomaticallyAllowedOperations(listOf("SET_STATUS", "START_PROCESS", "CREATE_DOCUMENT", "START_PROCESS")) shouldBe
            "SET_STATUS,START_PROCESS,CREATE_DOCUMENT,CREATE_METADATA,REMOVE_METADATA"
    }

    @Test
    fun `should trim whitespace and ignore empty entries`() {
        withAutomaticallyAllowedOperations(listOf(" START_PROCESS ", "", "SET_STATUS ")) shouldBe
            "START_PROCESS,SET_STATUS,CREATE_DOCUMENT,CREATE_METADATA,REMOVE_METADATA"
    }

    @Test
    fun `should allow only the automatically allowed operations if nothing is configured`() {
        withAutomaticallyAllowedOperations(emptyList()) shouldBe AUTOMATICALLY_ALLOWED_OPERATIONS
        withAutomaticallyAllowedOperations(listOf("")) shouldBe AUTOMATICALLY_ALLOWED_OPERATIONS
    }

    @Test
    fun `should fall back to the Data Services default if the property is unset`() {
        initParameterFor(emptyMap()) shouldBe "A12_INTERNAL_OPERATIONS,$AUTOMATICALLY_ALLOWED_OPERATIONS"
    }

    @Test
    fun `should not fall back to the Data Services default if the property is explicitly empty`() {
        // Data Services binds an empty value as "allow nothing" as well.
        initParameterFor(mapOf(PROPERTY to "")) shouldBe AUTOMATICALLY_ALLOWED_OPERATIONS
    }

    @Test
    fun `should read a comma separated list`() {
        initParameterFor(mapOf(PROPERTY to "START_PROCESS, A12_WORKFLOWS_OPERATIONS")) shouldBe
            "START_PROCESS,A12_WORKFLOWS_OPERATIONS,$AUTOMATICALLY_ALLOWED_OPERATIONS"
    }

    @Test
    fun `should read an indexed list`() {
        initParameterFor(
            mapOf(
                "$PROPERTY[0]" to "START_PROCESS",
                "$PROPERTY[1]" to "A12_WORKFLOWS_OPERATIONS",
            ),
        ) shouldBe "START_PROCESS,A12_WORKFLOWS_OPERATIONS,$AUTOMATICALLY_ALLOWED_OPERATIONS"
    }

    @Test
    fun `should read the legacy camel case property name`() {
        initParameterFor(mapOf("mgmtp.a12.dataservices.jsonRpc.allowedOperations" to "START_PROCESS")) shouldBe
            "START_PROCESS,$AUTOMATICALLY_ALLOWED_OPERATIONS"
    }

    @Test
    fun `should not set the init parameter for a single wildcard`() {
        initParameterFor(mapOf(PROPERTY to "*")).shouldBeNull()
    }

    /** Starts a context with [properties] and returns the value that the servlet context initializer sets, if any. */
    private fun initParameterFor(properties: Map<String, Any>): String? {
        val initParameter = slot<String>()
        val servletContext = mockk<ServletContext> {
            every { setInitParameter(INIT_PARAMETER, capture(initParameter)) } returns true
        }
        AnnotationConfigApplicationContext().use { context ->
            context.environment.propertySources.addFirst(MapPropertySource("test", properties))
            context.register(AllowedRpcOperationsConfig::class.java)
            context.refresh()
            context.getBean(AllowedRpcOperationsConfig::class.java).configureAllowedRpcOperations().onStartup(servletContext)
        }
        return if (initParameter.isCaptured) initParameter.captured else null
    }
}
