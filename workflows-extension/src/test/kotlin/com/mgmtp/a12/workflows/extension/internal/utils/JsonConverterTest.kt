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
package com.mgmtp.a12.workflows.extension.internal.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.gson.Gson
import com.mgmtp.a12.workflows.shared.dtos.ProcessEngineVariable
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

private class JsonConverterTest {

    val input = """
            {
              "a": "value/1",
              "b": ["value/2","value/3"]
            }
    """.trimIndent()

    @Test
    fun `should convert json node with different variable types to Process Engine map`() {
        val jsonNode = jacksonObjectMapper().readTree(input)
        val result = convertJsonToProcessEngineMap(jsonNode)

        result.shouldNotBeNull()
        result.size shouldBe 2

        result["a"].shouldBeInstanceOf<ProcessEngineVariable<String>>()
        result["b"].shouldBeInstanceOf<ProcessEngineVariable<String>>()

        result["a"]?.value.toString() shouldBe "value/1"
        result["b"]?.value shouldBe arrayListOf("value/2", "value/3")
    }

    @Test
    fun `should convert tested json node to Process Engine map`() {
        val someComplexVariable = """
            {
                "someComplexVariable": $input
            }
        """.trimIndent()

        val jsonNode = jacksonObjectMapper().readTree(someComplexVariable)
        val result = convertJsonToProcessEngineMap(jsonNode)

        result.shouldNotBeNull()

        result["someComplexVariable"].shouldBeInstanceOf<ProcessEngineVariable<Map<String, Any>>>()
        result["someComplexVariable"]?.value shouldBe Gson().fromJson(input, HashMap::class.java)
    }

    @Test
    fun `should return empty map if json node has empty string as value`() {
        val input = ""
        val jsonNode = jacksonObjectMapper().readTree(input)
        val result = convertJsonToProcessEngineMap(jsonNode)

        result.shouldBeEmpty()
    }

    @Test
    fun `should return empty map if json node is null`() {
        val result = convertJsonToProcessEngineMap(null)

        result.shouldBeEmpty()
    }
}
