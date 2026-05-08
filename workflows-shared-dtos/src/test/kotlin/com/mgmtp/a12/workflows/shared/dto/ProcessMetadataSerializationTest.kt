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
package com.mgmtp.a12.workflows.shared.dto

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mgmtp.a12.workflows.shared.dtos.Process
import com.mgmtp.a12.workflows.shared.dtos.ProcessDefinition
import com.mgmtp.a12.workflows.shared.dtos.ProcessMetadata
import com.mgmtp.a12.workflows.shared.dtos.Task
import com.mgmtp.a12.workflows.shared.dtos.TaskDefinition
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

private class ProcessMetadataSerializationTest {

    @Test
    fun `should serialize correctly when using object mapper with Kotlin module`() {
        // given
        val objectMapper = jacksonObjectMapper()
        val metadata = ProcessMetadata(
            ProcessDefinition("key", "name"),
            TaskDefinition("key", "name", "formKey"),
            Process("id", "bk", "initiator"),
            Task("id", "2022-02-11T09:20:00", "2022-02-11", "assignee"),
            "EMPTY",
        )

        // when
        val metadataString = objectMapper.writeValueAsString(metadata)
        println(metadataString)

        // then
        metadataString.shouldContain("ProcessDefinition")
        metadataString.shouldContain("TaskDefinition")
        metadataString.shouldContain("ID")
    }

    @Test
    fun `should deserialize successfully using object mapper with Kotlin module`() {
        // given
        val metadataString = this::class.java.getResource("/a12wf-metadata.json")!!.readText()
        val objectMapper = jacksonObjectMapper()

        // when
        val metadata = objectMapper.readValue(metadataString, ProcessMetadata::class.java)

        // then
        metadata.process.id shouldBe "530d14f2-107f-11ec-8308-803253632ada"
        metadata.task.creationDate shouldBe "2022-02-11T09:20:00"
    }
}
