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

import com.mgmtp.a12.model.workflows.migration.accessor.VersionAccessor
import com.mgmtp.a12.model.workflows.migration.config.MigratorConfigLoader
import com.mgmtp.a12.model.workflows.migration.config.MigratorConfigLocator
import com.mgmtp.a12.model.workflows.migration.config.types.MigrationConfig
import com.mgmtp.a12.model.workflows.migration.exception.MigrationException
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationError
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationErrorType
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationResult
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationResultType
import com.mgmtp.a12.model.workflows.migration.step.MigrationStepExecutor
import com.mgmtp.a12.model.workflows.migration.utils.XmlUtil
import com.mgmtp.a12.model.workflows.migration.utils.stream.StreamSource
import com.mgmtp.a12.model.workflows.migration.validation.ModelValidator
import com.mgmtp.a12.model.workflows.migration.validation.SchemaValidator
import org.jdom2.Document
import org.jdom2.JDOMException
import java.io.IOException
import java.util.*

/**
 * Instantiates a new Migrator.
 *
 * @param versionAccessor       the accessor for source and target model versions
 * @param migratorConfigLocator the migrator config locator
 * @param schemaValidator       the schema validator
 * @param modelValidator        the model validator
 */
class WorkflowsMigrator(
    private val versionAccessor: VersionAccessor,
    private val migratorConfigLocator: MigratorConfigLocator,
    private val schemaValidator: SchemaValidator,
    private val modelValidator: ModelValidator,
    private val optionalSteps: List<String>,
    private var skipModelValidation: Boolean = false,
) {

    /**
     * Conduct the actual migration and write the migrated model to modelSource
     *
     * @param modelSource Content of model as writable stream source
     * @return Result of migration
     */
    fun migrate(modelSource: StreamSource): MigrationResult = migrate(modelSource, buildMigrationStepExecutor())

    /**
     * Conduct the actual migration. Migrates the model document in-place.
     *
     * @param document           the model as a jdom2 Document
     * @return Result of migration
     */
    fun migrate(document: Document): MigrationResult = migrate(document, buildMigrationStepExecutor())

    /**
     * Batch execution of migration. Writes each migrated model to modelSource
     *
     * @param modelSources List of models to migrate
     * @return List of migration results, that is in the same order as modelsSources.
     */
    fun migrate(modelSources: List<StreamSource>): List<MigrationResult> {
        val stepExecutor = buildMigrationStepExecutor()
        val results: MutableList<MigrationResult> = mutableListOf()
        modelSources.forEach { results.add(migrate(it, stepExecutor)) }
        return results
    }

    fun skipModelValidation(skipModelValidation: Boolean) {
        this.skipModelValidation = skipModelValidation
    }

    // Privates
    private fun migrate(streamSource: StreamSource, stepExecutor: MigrationStepExecutor): MigrationResult {
        try {
            val document = parseSource(streamSource)

            val migrationResult = migrate(document, stepExecutor)

            if (migrationResult.type == MigrationResultType.SUCCESSFUL) {
                writeMigratedDocumentContent(streamSource, document)
            }
            return migrationResult
        } catch (migExcp: MigrationException) {
            return MigrationResult.createWithError(migExcp.error)
        } catch (e: Exception) {
            return MigrationResult.createWithError(
                MigrationError(MigrationErrorType.UNKNOWN_ERROR, message = e.message!!),
            )
        }
    }

    private fun migrate(doc: Document, stepExecutor: MigrationStepExecutor): MigrationResult {
        try {
            modelValidator.validate(doc, versionAccessor.sourceModelVersion)
            val migrationResult = MigrationResult(MigrationResultType.SUCCESSFUL)

            stepExecutor.execute(doc, migrationResult)

            if (!skipModelValidation) {
                schemaValidator.validate(doc, migrationResult)
            }
            return migrationResult
        } catch (migExcp: MigrationException) {
            return MigrationResult.createWithError(migExcp.error)
        } catch (e: Exception) {
            e.printStackTrace()
            return MigrationResult.createWithError(
                MigrationError(MigrationErrorType.UNKNOWN_ERROR, message = e.message!!),
            )
        }
    }

    @Throws(MigrationException::class)
    private fun writeMigratedDocumentContent(streamSource: StreamSource, document: Document) {
        try {
            streamSource.getOutput().use { outputStream ->
                XmlUtil.serializeModel(
                    outputStream,
                    document,
                )
            }
        } catch (e: IOException) {
            throw MigrationException(
                MigrationError(
                    MigrationErrorType.MODEL_CONTENT_RESULT_NOT_WRITABLE,
                    message = "Writing migrated model failed: ${e.message}",
                ),
            )
        }
    }

    @Throws(MigrationException::class)
    private fun parseSource(streamSource: StreamSource): Document {
        var document: Document
        try {
            streamSource.getInput().use {
                document = XmlUtil.parseModel(it)
                val path = streamSource.getModelFilePath()
                document.setProperty(
                    WorkflowsMigratorCLI.FILE_PATH_KEY,
                    path?.toAbsolutePath().toString(),
                )
            }
            return document
        } catch (exception: JDOMException) {
            throw MigrationException(
                MigrationError(
                    MigrationErrorType.MODEL_CONTENT_INVALID_XML,
                    message = "parsing source failed: ${exception.message}",
                ),
            )
        }
    }

    private fun buildMigrationStepExecutor(): MigrationStepExecutor = MigrationStepExecutor(versionAccessor, loadConfiguration(), Optional.empty(), Optional.of(optionalSteps))

    private fun loadConfiguration(): MigrationConfig {
        val config = migratorConfigLocator.locate()
            ?: throw MigrationException(
                MigrationError(
                    MigrationErrorType.CONFIG_FILE_NOT_FOUND,
                    message = "Input stream of configuration file must not be NULL!",
                ),
            )
        return MigratorConfigLoader().load(config)
    }

    open class Builder {
        private lateinit var versionAccessor: VersionAccessor
        private lateinit var configLocator: MigratorConfigLocator
        private lateinit var schemaValidator: SchemaValidator
        private lateinit var modelValidator: ModelValidator
        private var optionalSteps: List<String> = emptyList()

        fun withVersionAccessor(versionAccessor: VersionAccessor) {
            this.versionAccessor = versionAccessor
        }

        fun withConfigLocator(configLocator: MigratorConfigLocator): Builder = apply {
            this.configLocator = configLocator
        }

        fun withSchemaValidator(schemaValidator: SchemaValidator): Builder = apply {
            this.schemaValidator = schemaValidator
        }

        fun withOptionalSteps(optionalSteps: List<String>): Builder = apply {
            this.optionalSteps = optionalSteps
        }

        fun withModelValidator(modelValidator: ModelValidator): Builder = apply {
            this.modelValidator = modelValidator
        }

        fun build(): WorkflowsMigrator = WorkflowsMigrator(
            versionAccessor,
            configLocator,
            schemaValidator,
            modelValidator,
            optionalSteps,
        )
    }
}
