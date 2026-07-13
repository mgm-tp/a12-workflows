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

package com.mgmtp.a12.model.workflows.migration.bpmn.steps

import com.mgmtp.a12.model.workflows.migration.accessor.ElementQuery
import com.mgmtp.a12.model.workflows.migration.bpmn.validation.BPMNModelValidator
import com.mgmtp.a12.model.workflows.migration.config.types.MigrationStepConfigType
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationError
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationErrorType
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationInfo
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationLogger
import com.mgmtp.a12.model.workflows.migration.step.BaseMigrationStep
import org.jdom2.Document

class RemoveAllExtensionPropertiesStep(migrationStepConfig: MigrationStepConfigType) : BaseMigrationStep(migrationStepConfig) {

    override fun execute(document: Document, migrationLogger: MigrationLogger) {
        val query = ElementQuery.createForBPMN(document.getRootElement())
        val camundaPropertiesList = query.byXpath("//bpmn:extensionElements/camunda:properties")
        for (camundaProperties in camundaPropertiesList) {
            val extensionProperties =
                camundaProperties.getChildren("property", BPMNModelValidator.NS_CAMUNDA).toList()
            for (extensionProperty in extensionProperties) {
                val propertyName = extensionProperty.getAttribute("name").value
                val removed = camundaProperties.removeContent(extensionProperty)
                if (removed) {
                    migrationLogger
                        .addInfo(
                            MigrationInfo(propertyName, "Successfully removed extension property"),
                        )
                } else {
                    migrationLogger
                        .addError(
                            MigrationError(
                                MigrationErrorType.STEP_MIGRATION_ERROR,
                                propertyName,
                                "Extension property could not be removed from the bpmn model\"",
                            ),
                        )
                }
            }
        }
    }
}
