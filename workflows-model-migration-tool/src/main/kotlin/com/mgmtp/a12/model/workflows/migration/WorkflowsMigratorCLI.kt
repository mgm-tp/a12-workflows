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

import com.mgmtp.a12.model.workflows.migration.bpmn.BPMNMigratorBuilder
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationError
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationInfo
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationResultType
import com.mgmtp.a12.model.workflows.migration.utils.XmlUtil
import org.apache.commons.cli.*
import org.jdom2.Document
import org.jdom2.JDOMException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.system.exitProcess

/**
 * Main class of model migration, this coordinates the actual migration process and prepares the necessary configuration.
 */
class WorkflowsMigratorCLI {

    companion object {
        const val FILE_PATH_KEY = "http://www.mgm-tp.com/a12/model/file/path"
        const val SUCCESS = 0
        const val FAILED = 1
        private val OPTIONS = Options()

        init {
            OPTIONS.addOption("b", "backup", false, "create a backup before migrating a model")
            OPTIONS.addOption(
                "n",
                "no-validation",
                false,
                "do not validate the model after migration (not recommended)",
            )
            OPTIONS.addOption("d", "debug", false, "enable debug mode")
            OPTIONS.addOption("t", "test", false, "do not write migrated model to file, but to stdout")
            OPTIONS.addOption("h", "help", false, "display this help")
            OPTIONS.addOption(
                "o",
                "optional",
                true,
                "enable optional migration steps, enable by listing the names of the steps, e.g. SampleMigrationStepFive",
            )
            OPTIONS.addOption("smv", "source-model-version", true, "source model version of the bpmn model [mandatory]")
            OPTIONS.addOption(
                "tmv",
                "target-model-version",
                true,
                "target model version of the bpmn model, until migration steps shall be executed [optional]",
            )
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val migrationResult = WorkflowsMigratorCLI().migrate(args)
            exitProcess(migrationResult)
        }

        private fun showHelp(error: String? = null) {
            val helpFormatter = HelpFormatter()
            val header = StringBuilder()
            if (error != null) {
                header.append("\terror: ")
                header.append(error)
            }
            header.append("\noptions:")
            helpFormatter.printHelp(
                120,
                ("java -jar BpmnMigrationTool-VERSION-jar-with-dependencies.jar [OPTIONS] MODEL_FILE/DIRECTORY"),
                header.toString(),
                OPTIONS,
                null,
            )
        }

        private fun printMigrationInfos(infos: Collection<MigrationInfo>) {
            if (!infos.isEmpty()) {
                println("Infos:")
                infos.forEach { println("\t$it") }
                println()
            }
        }

        private fun printMigrationErrors(errors: Collection<MigrationError>) {
            if (!errors.isEmpty()) {
                println("Errors:")
                errors.forEach { println("\t$it") }
                println("")
            }
        }

        private fun printSuccess(size: Int) {
            println("\n$size bpmn models migrated successfully.")
        }

        private fun printProblems(resultMap: Map<Path, Int>) {
            resultMap.entries
                .filter { it.value == FAILED }
                .forEach { println("Migration of '${it.key}' failed!") }
        }
    }

    /**
     * Initial method after migration started.
     *
     * @param args contains directory/files as well as the options
     * @return status of migration
     */
    fun migrate(args: Array<String>?): Int {
        val parser = DefaultParser()
        val commandLine = runCatching {
            parser.parse(OPTIONS, args, false)
        }.getOrElse {
            showHelp(it.message)
            return FAILED
        }

        if (commandLine.hasOption('h')) {
            showHelp()
            return SUCCESS
        }
        if (commandLine.args.isEmpty()) {
            showHelp("Please specify a model file!")
            return FAILED
        }
        if (!commandLine.hasOption("smv")) {
            showHelp("Please specify a source-model-version!")
            return FAILED
        }

        val inputPath = runCatching {
            Paths.get(commandLine.args[0])
        }.getOrElse {
            showHelp("Invalid path: " + commandLine.args[0])
            return FAILED
        }

        if (Files.isRegularFile(inputPath)) {
            return migrateSingleModel(commandLine, inputPath)
        } else if (Files.isDirectory(inputPath)) {
            return migrateDirectoryRecursively(commandLine, inputPath)
        } else {
            showHelp("Not a file or directory: " + commandLine.args[0])
            return FAILED
        }
    }

    private fun migrateDirectoryRecursively(commandLine: CommandLine, inputPath: Path): Int {
        val resultMap: MutableMap<Path, Int> = HashMap()
        try {
            Files.walk(inputPath)
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".bpmn") }
                .forEach { resultMap[it] = migrateSingleModel(commandLine, it) }
            if ((resultMap.values.stream().anyMatch { it == FAILED })) {
                printProblems(resultMap)
                return FAILED
            } else {
                printSuccess(resultMap.size)
                return SUCCESS
            }
        } catch (e: IOException) {
            println("Failed to traverse directory + '$inputPath'.\nReason: ${e.message}")
            return FAILED
        }
    }

    private fun migrateSingleModel(commandLine: CommandLine, inputPath: Path): Int {
        if (commandLine.hasOption('b')) {
            try {
                Files.copy(inputPath, Paths.get("$inputPath.bak"))
            } catch (exception: IOException) {
                println("Could not create backup file: ${exception.message}")
                return FAILED
            }
        }
        var document: Document
        try {
            Files.newInputStream(inputPath).use {
                document = XmlUtil.parseModel(it)
                document.setProperty(FILE_PATH_KEY, inputPath.toFile().getCanonicalPath())
            }
        } catch (ex: IOException) {
            println("Could not read model file: ${ex.message}")
            return FAILED
        } catch (ex: JDOMException) {
            println("Could not parse model: ${ex.message}")
            return FAILED
        }

        val builder = runCatching {
            val sourceModelVersion = commandLine.getOptionValue("smv")
            val targetModelVersion = commandLine.getOptionValue("tmv")
            BPMNMigratorBuilder(sourceModelVersion, targetModelVersion)
        }.onFailure {
            println("Severe system error: ${it.message}")
            return FAILED
        }.getOrThrow()

        if (commandLine.hasOption('o')) {
            val optionValues = commandLine.getOptionValues('o')
            val optionalSteps: List<String> = listOf(*optionValues ?: arrayOfNulls(0)).map { it.toString() }
            builder.withOptionalSteps(optionalSteps)
        }

        val migrator = builder.build()
        migrator.skipModelValidation(commandLine.hasOption('n'))
        val migrationResult = migrator.migrate(document)
        when (migrationResult.type) {
            MigrationResultType.SUCCESSFUL -> {
                println("Successfully migrated file '$inputPath' from version ${migrationResult.sourceModelVersion} to ${migrationResult.targetModelVersion}.")

                if (commandLine.hasOption('d')) {
                    printMigrationInfos(migrationResult.infos)
                }

                try {
                    val outputStream = if (commandLine.hasOption('t')) System.out else Files.newOutputStream(inputPath)
                    outputStream.use { XmlUtil.serializeModel(outputStream, document) }
                } catch (exception: IOException) {
                    println("Unable to write model to disk: ${exception.message}")
                    return FAILED
                }
            }

            MigrationResultType.FAILED -> {
                println("Migration for '$inputPath' failed!")
                printMigrationErrors(migrationResult.errors)
                printMigrationInfos(migrationResult.infos)
                return FAILED
            }

            MigrationResultType.NOT_REQUIRED -> {
                println("Migration for '$inputPath' not required.\n")
            }
        }
        return SUCCESS
    }
}
