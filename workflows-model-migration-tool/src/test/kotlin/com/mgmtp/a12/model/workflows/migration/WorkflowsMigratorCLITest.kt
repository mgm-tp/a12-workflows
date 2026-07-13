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

package com.mgmtp.a12.model.workflows.migration

import com.mgmtp.a12.model.workflows.migration.WorkflowsMigratorCLI.Companion.FAILED
import com.mgmtp.a12.model.workflows.migration.WorkflowsMigratorCLI.Companion.SUCCESS
import com.mgmtp.a12.model.workflows.migration.utils.ConsoleOutputCapturer
import com.mgmtp.a12.model.workflows.migration.utils.Constants
import com.mgmtp.a12.model.workflows.migration.utils.FileSystemUtil
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

private class WorkflowsMigratorCLITest {

    var consoleOutputCapturer: ConsoleOutputCapturer? = null

    companion object {
        var tempDir: Path? = null
        const val BPMN_MODELS_FOLDER_PATH = Constants.RESOURCE_PATH
        val VALID_MODEL_NAMES = arrayOf(
            "FillTravelExpenses.bpmn",
            "ParallelGatewayProcess.bpmn",
            "PlanBusinessTrip.bpmn",
        )
        val INVALID_MODEL_NAMES = arrayOf(
            "FillTravelExpensesInvalid.bpmn",
            "ParallelGatewayProcessInvalid.bpmn",
        )
        const val EXPECTED_SUCCESS = "Successfully migrated file '%s' from version"
        const val EXPECTED_ERROR = "Migration of '%s' failed!"

        @BeforeAll
        @Throws(Exception::class)
        @JvmStatic
        fun setup() {
            tempDir = Files.createTempDirectory("WorkflowsMigratorCLITest")
            val validModelsDirectory = createTestModelsTempDirectory("valid")
            copyTestFileInDirectory(validModelsDirectory, VALID_MODEL_NAMES)
            val invalidModelsDirectory = createTestModelsTempDirectory("invalid")
            copyTestFileInDirectory(invalidModelsDirectory, INVALID_MODEL_NAMES)
        }

        @JvmStatic
        fun createTestModelsTempDirectory(startName: String): Path? {
            val validModelsDirectory: Path? = tempDir?.resolve("$startName/$BPMN_MODELS_FOLDER_PATH")
            Files.createDirectories(validModelsDirectory)
            return validModelsDirectory
        }

        @JvmStatic
        fun copyTestFileInDirectory(directory: Path?, testFiles: Array<String>) {
            for (modelName: String in testFiles) {
                val model = Files.newInputStream(Paths.get("$BPMN_MODELS_FOLDER_PATH/$modelName"))
                Files.copy(model, directory?.resolve(modelName))
            }
        }

        @JvmStatic
        fun generateTestModelsAndExpectedMessagesParameter(): Stream<Arguments> = Stream.of(
            Arguments.of(VALID_MODEL_NAMES, "valid/$BPMN_MODELS_FOLDER_PATH", EXPECTED_SUCCESS, SUCCESS),
            Arguments.of(INVALID_MODEL_NAMES, "invalid/$BPMN_MODELS_FOLDER_PATH", EXPECTED_ERROR, FAILED),
        )

        @AfterAll
        @JvmStatic
        fun cleanUp() {
            FileSystemUtil.removeDirectoryWithContents(tempDir)
        }
    }

    @BeforeEach
    fun setupConsoleOutputCapturer() {
        consoleOutputCapturer = ConsoleOutputCapturer()
        consoleOutputCapturer?.start()
    }

    @ParameterizedTest
    @MethodSource("generateTestModelsAndExpectedMessagesParameter")
    fun `should migrate a valid and invalid models`(
        modelNames: Array<String>,
        modelDirectoryPath: String,
        expectedMessage: String,
        expectedResultCode: Int,
    ) {
        // given
        val migrationResult = WorkflowsMigratorCLI().migrate(
            arrayOf(tempDir?.resolve(modelDirectoryPath).toString(), "-d", "-smv", "8.1.0"),
        )
        // when
        System.out.flush()
        val consoleOutput = consoleOutputCapturer?.stop()

        // then
        migrationResult shouldBe expectedResultCode
        for (modelName: String in modelNames) {
            val modelPath = tempDir?.resolve("$modelDirectoryPath/$modelName")
            val formattedExpectedMessage = String.format(expectedMessage, modelPath)
            consoleOutput shouldContain formattedExpectedMessage
        }
    }

    @Test
    fun `should show help`() {
        // given
        val migrationResult = WorkflowsMigratorCLI().migrate(arrayOf(tempDir.toString(), "-h"))

        // when
        System.out.flush()
        val consoleOutput = consoleOutputCapturer?.stop()

        // then
        val lineSeparator = System.lineSeparator()
        migrationResult shouldBe SUCCESS
        consoleOutput shouldContain "options:" + lineSeparator +
            " -b,--backup                         create a backup before migrating a model" + lineSeparator +
            " -d,--debug                          enable debug mode" + lineSeparator +
            " -h,--help                           display this help" + lineSeparator +
            " -n,--no-validation                  do not validate the model after migration (not recommended)" + lineSeparator +
            " -o,--optional <arg>                 enable optional migration steps, enable by listing the names of the steps, e.g."
    }

    @Test
    fun `should fail if source model version was not given`() {
        // given
        val migrationResult = WorkflowsMigratorCLI().migrate(arrayOf(tempDir.toString()))

        // when
        System.out.flush()
        val consoleOutput = consoleOutputCapturer?.stop()

        // then
        migrationResult shouldBe FAILED
        consoleOutput shouldContain "error: Please specify a source-model-version"
    }

    @Test
    fun `should fail if model is not specified`() {
        // given
        val migrationResult = WorkflowsMigratorCLI().migrate(arrayOf("-b"))

        // when
        System.out.flush()
        val consoleOutput = consoleOutputCapturer?.stop()

        // then
        migrationResult shouldBe FAILED
        consoleOutput shouldContain "error: Please specify a model file!"
    }

    @Test
    fun `should fail if invalid option was set`() {
        // given
        val migrationResult = WorkflowsMigratorCLI().migrate(arrayOf("-wo"))

        // when
        System.out.flush()
        val consoleOutput = consoleOutputCapturer?.stop()

        // then
        migrationResult shouldBe FAILED
        consoleOutput shouldContain "error: Unrecognized option: -wo"
    }

    @Test
    fun `should fail if path is invalid`() {
        // given
        val migrationResult = WorkflowsMigratorCLI().migrate(arrayOf("/asdasdca/asdllasd/", "-smv", "8.1.0"))

        // when
        System.out.flush()
        val consoleOutput = consoleOutputCapturer?.stop()

        // then
        migrationResult shouldBe FAILED
        consoleOutput shouldContain "error: Not a file or directory:"
    }
}
