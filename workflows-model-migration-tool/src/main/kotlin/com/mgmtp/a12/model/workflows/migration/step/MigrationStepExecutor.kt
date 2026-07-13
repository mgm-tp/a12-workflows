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

package com.mgmtp.a12.model.workflows.migration.step

import com.mgmtp.a12.model.workflows.migration.accessor.VersionAccessor
import com.mgmtp.a12.model.workflows.migration.config.types.MigrationConfig
import com.mgmtp.a12.model.workflows.migration.config.types.MigrationStepConfigType
import com.mgmtp.a12.model.workflows.migration.exception.MigrationException
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationError
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationErrorType
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationResult
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationResultType
import org.apache.commons.lang3.StringUtils
import org.jdom2.Document
import java.util.*

/**
 * Executes migration steps in right order, the list of steps will be calculated based on source model version
 */
class MigrationStepExecutor(
    versionAccessor: VersionAccessor,
    migrationConfig: MigrationConfig,
    stepFactory: Optional<MigrationStepFactory>,
    optionalSteps: Optional<List<String>>,
) {
    private val versionAccessor: VersionAccessor
    private val migrationConfig: MigrationConfig
    private val stepFactory: MigrationStepFactory
    private val optionalSteps: List<String>

    init {
        this.versionAccessor = versionAccessor
        this.migrationConfig = migrationConfig
        this.stepFactory = stepFactory.orElseGet { DefaultMigrationStepFactory() }
        this.optionalSteps = optionalSteps.orElseGet { ArrayList() }
    }

    /**
     * Execute all migration steps, that are configured for given source model version.
     *
     * @param doc             the document to migrate
     * @param migrationResult the migration result
     */
    fun execute(doc: Document, migrationResult: MigrationResult) {
        // set source model version of result
        migrationResult.sourceModelVersion = versionAccessor.sourceModelVersion
        if (StringUtils.equals(migrationConfig.currentModelVersion, versionAccessor.sourceModelVersion)) {
            // if model is up-do-date ==> don't migrate ----------------------------------------------------------------
            migrationResult.targetModelVersion = versionAccessor.sourceModelVersion
            migrationResult.type = MigrationResultType.NOT_REQUIRED
        } else {
            // if model is obsolete ==> execute migration --------------------------------------------------------------
            val migrationSteps = getStepsFor(versionAccessor.sourceModelVersion)
            for (migrationStep: MigrationStep in migrationSteps) {
                // 1.) Validate preconditions
                if (!StringUtils.equals(versionAccessor.sourceModelVersion, migrationStep.sourceModelVersion)) {
                    throw MigrationException(
                        MigrationError(
                            MigrationErrorType.MODEL_CONTENT_UNEXPECTED_SOURCE_VERSION,
                            message = "Precondition of step '${migrationStep.javaClass.getSimpleName()}' violated: Expected source model version = '${migrationStep.sourceModelVersion}' is not equal to document model version '${versionAccessor.sourceModelVersion}'",
                        ),
                    )
                }

                // 2.) Migrate document
                migrationStep.execute(doc, migrationResult)

                // 3.) Update current sourceModelVersion after migration step
                versionAccessor.sourceModelVersion = migrationStep.targetModelVersion

                // 4.) Update target model version of result
                migrationResult.targetModelVersion = migrationStep.targetModelVersion
            }
        }
    }

    private fun getStepsFor(sourceModelVersion: String?): List<MigrationStep> {
        val steps = arrayListOf<MigrationStep>()
        if (migrationConfig.getMigrationSteps().size == 0) {
            // in case of empty config ==> do nothing
            return steps
        }
        val visitedOptionalSteps = mutableListOf<MigrationStepConfigType?>()
        var stepConfigType: MigrationStepConfigType? = getStepConfigsFor(sourceModelVersion, visitedOptionalSteps)
            ?: throw MigrationException(
                MigrationError(
                    MigrationErrorType.UNSUPPORTED_SOURCE_MODEL_VERSION,
                    message = "Source model version: '$sourceModelVersion' could not be found in the given migration configuration",
                ),
            )
        do {
            if (isOptionalStep(stepConfigType)) {
                visitedOptionalSteps.add(stepConfigType)
            }
            if ((
                !isOptionalStep(stepConfigType) ||
                    optionalSteps.contains(getStepName(stepConfigType!!.bean.clazz))
                )
            ) {
                steps.add(stepFactory.create(stepConfigType)!!)
            }
            // Get next migration step based on targetModelVersion of previous migration step
            stepConfigType = getStepConfigsFor(stepConfigType!!.targetModelVersion, visitedOptionalSteps)
        } while (stepConfigType != null)
        return steps
    }

    private fun getStepConfigsFor(
        modelVersion: String?,
        visitedOptionalSteps: List<MigrationStepConfigType?>,
    ): MigrationStepConfigType? {
        for (stepConfigType: MigrationStepConfigType in migrationConfig.getMigrationSteps()) {
            if ((
                StringUtils.equals(stepConfigType.sourceModelVersion, modelVersion) &&
                    !StringUtils.equals(modelVersion, versionAccessor.targetModelVersion) &&
                    !visitedOptionalSteps.contains((stepConfigType))
                )
            ) {
                return stepConfigType
            }
        }
        return null
    }

    private fun isOptionalStep(step: MigrationStepConfigType?): Boolean = step!!.isSetOptional && step.isOptional

    private fun getStepName(fullyQualifiedName: String): String {
        val indexOfLastPoint = fullyQualifiedName.lastIndexOf('.')
        return fullyQualifiedName.substring(indexOfLastPoint + 1)
    }
}
