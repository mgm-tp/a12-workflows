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

package com.mgmtp.a12.model.workflows.migration.bpmn

import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationResult
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationResultType
import com.mgmtp.a12.model.workflows.migration.utils.Constants
import com.mgmtp.a12.model.workflows.migration.utils.TestStreamSource
import io.kotest.matchers.iterator.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.apache.commons.io.IOUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import org.xmlunit.assertj3.XmlAssert
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private class BPMNMigratorBuilderTest {

    companion object {
        const val BPMN_MODEL = "ParallelGatewayProcess.bpmn"
        val BPMN_MODEL_PATH: Path = Paths.get(Constants.RESOURCE_PATH + BPMN_MODEL)
        val BPMN_MODEL_MIGRATED_PATH: Path =
            Paths.get(Constants.RESOURCE_PATH + "ParallelGatewayProcessMigrated.bpmn")
    }

    @Test
    fun `should migrate to version 9`(@TempDir(cleanup = CleanupMode.ALWAYS) tempDir: Path) {
        // given
        val migrator = BPMNMigratorBuilder("8.1.0", "9.0.0").build()
        val modelSource = TestStreamSource(BPMN_MODEL_PATH, tempDir.resolve(BPMN_MODEL) ?: Paths.get(""))

        // when
        val result: MigrationResult = migrator.migrate(modelSource)

        // then
        result.type shouldBe MigrationResultType.SUCCESSFUL
        result.errors.iterator().shouldBeEmpty()
        result.sourceModelVersion shouldBe "8.1.0"
        result.targetModelVersion shouldBe "9.0.0"

        val migratedModel = String(modelSource.getOutputStream(), StandardCharsets.UTF_8).trim { it <= ' ' }
        val expectedModel =
            IOUtils.toString(Files.newInputStream(BPMN_MODEL_MIGRATED_PATH), StandardCharsets.UTF_8).trim { it <= ' ' }
        XmlAssert.assertThat(migratedModel).and(expectedModel).areIdentical()
    }
}
