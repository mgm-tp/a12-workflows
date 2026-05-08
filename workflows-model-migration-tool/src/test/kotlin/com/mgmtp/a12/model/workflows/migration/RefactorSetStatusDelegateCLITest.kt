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
package com.mgmtp.a12.model.workflows.migration

import com.mgmtp.a12.model.workflows.migration.WorkflowsMigratorCLI.Companion.FAILED
import com.mgmtp.a12.model.workflows.migration.WorkflowsMigratorCLI.Companion.SUCCESS
import com.mgmtp.a12.model.workflows.migration.bpmn.steps.RefactorSetStatusDelegateStep.Companion.SKIPPED_MESSAGE
import com.mgmtp.a12.model.workflows.migration.utils.ConsoleOutputCapturer
import com.mgmtp.a12.model.workflows.migration.utils.Constants
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private class RefactorSetStatusDelegateCLITest {

    var consoleOutputCapturer: ConsoleOutputCapturer? = null

    companion object {
        const val DOCUMENT_USING_SET_DOCUMENT_FIELD_DELEGATE = "DocumentUsing_SetDocumentFieldDelegate.bpmn"
        val DOCUMENT_USING_SET_DOCUMENT_FIELD_DELEGATE_PATH = Paths.get(
            Constants.RESOURCE_PATH,
            Constants.SET_STATUS_DELEGATE_MIGRATION_FOLDER_NAME,
            DOCUMENT_USING_SET_DOCUMENT_FIELD_DELEGATE,
        )
        const val DOCUMENT_NOT_USING_SET_DOCUMENT_FIELD_DELEGATE =
            "DocumentNotUsing_SetDocumentFieldDelegate.bpmn"
        val DOCUMENT_NOT_USING_SET_DOCUMENT_FIELD_DELEGATE_PATH = Paths.get(
            Constants.RESOURCE_PATH,
            Constants.SET_STATUS_DELEGATE_MIGRATION_FOLDER_NAME,
            DOCUMENT_NOT_USING_SET_DOCUMENT_FIELD_DELEGATE,
        )
        var tempDir: Path? = null
    }

    @BeforeEach
    fun setup(@TempDir(cleanup = CleanupMode.ALWAYS) tempDir: Path) {
        consoleOutputCapturer = ConsoleOutputCapturer()
        consoleOutputCapturer?.start()
        Companion.tempDir = tempDir
        val fileNeedMigration = Files.newInputStream(DOCUMENT_USING_SET_DOCUMENT_FIELD_DELEGATE_PATH)
        Files.copy(fileNeedMigration, tempDir.resolve(DOCUMENT_USING_SET_DOCUMENT_FIELD_DELEGATE))
        val model = Files.newInputStream(DOCUMENT_NOT_USING_SET_DOCUMENT_FIELD_DELEGATE_PATH)
        Files.copy(model, tempDir.resolve(DOCUMENT_NOT_USING_SET_DOCUMENT_FIELD_DELEGATE))
    }

    @AfterEach
    fun cleanUp() {
        consoleOutputCapturer?.stop()
    }

    @Test
    fun `should migrate a document that use SetDocumentFieldDelegate`() {
        // given
        val documentPath = Paths.get(tempDir.toString(), DOCUMENT_USING_SET_DOCUMENT_FIELD_DELEGATE).toString()
        val migrationResult = WorkflowsMigratorCLI().migrate(arrayOf(documentPath, "-smv", "10.0.0"))

        // when
        System.out.flush()
        val consoleOutput = consoleOutputCapturer?.stop()

        // then
        migrationResult shouldBe SUCCESS
        consoleOutput shouldContain "Successfully migrated file"
    }

    @Test
    fun `should not migrate a document that does not use SetDocumentFieldDelegate`() {
        // given
        val documentPath = Paths.get(tempDir.toString(), DOCUMENT_NOT_USING_SET_DOCUMENT_FIELD_DELEGATE).toString()
        val migrationResult = WorkflowsMigratorCLI().migrate(arrayOf(documentPath, "-smv", "10.0.0"))

        // when
        System.out.flush()
        val consoleOutput: String = consoleOutputCapturer?.stop().toString()

        // then
        migrationResult shouldBe SUCCESS
        consoleOutput shouldContain SKIPPED_MESSAGE
    }

    @Test
    fun `should not migrate a document with an incompatible version`() {
        // given
        val documentPath = Paths.get(tempDir.toString(), DOCUMENT_NOT_USING_SET_DOCUMENT_FIELD_DELEGATE).toString()
        val migrationResult = WorkflowsMigratorCLI().migrate(arrayOf(documentPath, "-smv", "12.0.0"))

        // when
        System.out.flush()
        val consoleOutput: String = consoleOutputCapturer?.stop().toString()

        // then
        migrationResult shouldBe FAILED
        consoleOutput shouldNotContain SKIPPED_MESSAGE
    }
}
