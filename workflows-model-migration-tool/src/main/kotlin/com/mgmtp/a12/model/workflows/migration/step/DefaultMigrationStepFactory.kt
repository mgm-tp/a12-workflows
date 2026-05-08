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
package com.mgmtp.a12.model.workflows.migration.step

import com.mgmtp.a12.model.workflows.migration.config.types.MigrationStepConfigType
import com.mgmtp.a12.model.workflows.migration.exception.MigrationException
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationError
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationErrorType
import java.lang.reflect.InvocationTargetException

/**
 * Creates new MigrationStep instance from configured class name of bean.
 */
internal class DefaultMigrationStepFactory : MigrationStepFactory {
    /**
     * Creates a migration step instance configured in BPMNMigratorConfig.xml.
     *
     * @param stepConfig the step config
     * @return the migration step
     */
    override fun create(stepConfig: MigrationStepConfigType?): MigrationStep {
        val clazzName = stepConfig?.bean?.clazz
        try {
            val stepClazz = Class.forName(clazzName)
            // noinspection unchecked
            val stepConstructor = stepClazz.getConstructor(MigrationStepConfigType::class.java)
            val step = stepConstructor.newInstance(stepConfig)
            if (step is MigrationStep) {
                return step
            }
            throw MigrationException(
                MigrationError(
                    MigrationErrorType.STEP_UNSUPPORTED_TYPE,
                    message = "Current migration step '$clazzName' is not a sub class of '${MigrationStep::class.java.getName()}'",
                ),
            )
        } catch (e: ClassNotFoundException) {
            throw buildMigrationExceptionStepUnsupportedType(stepConfig.toString(), e.message)
        } catch (e: InvocationTargetException) {
            throw buildMigrationExceptionStepUnsupportedType(stepConfig.toString(), e.message)
        } catch (e: IllegalAccessException) {
            throw buildMigrationExceptionStepUnsupportedType(stepConfig.toString(), e.message)
        } catch (e: InstantiationException) {
            throw buildMigrationExceptionStepUnsupportedType(stepConfig.toString(), e.message)
        } catch (e: NoSuchMethodException) {
            throw buildMigrationExceptionStepUnsupportedType(stepConfig.toString(), e.message)
        }
    }

    private fun buildMigrationExceptionStepUnsupportedType(
        clazzName: String,
        migrationStepClazzName: String?,
    ): MigrationException = MigrationException(
        MigrationError(
            MigrationErrorType.STEP_UNSUPPORTED_TYPE,
            message = "Current migration step '$clazzName' is not a sub class of '$migrationStepClazzName'",
        ),
    )
}
