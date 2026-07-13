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

import com.mgmtp.a12.model.workflows.migration.WorkflowsMigrator
import com.mgmtp.a12.model.workflows.migration.bpmn.accessor.BPMNVersionAccessor
import com.mgmtp.a12.model.workflows.migration.bpmn.validation.BPMNModelValidator
import com.mgmtp.a12.model.workflows.migration.config.ClasspathMigratorConfigLocator
import com.mgmtp.a12.model.workflows.migration.validation.JDomSchemaValidator

class BPMNMigratorBuilder(sourceModelVersion: String, targetModelVersion: String?) : WorkflowsMigrator.Builder() {

    companion object {
        private const val BPMN_SCHEMA_PREFIX = "bpmn/schema/"
    }

    init {
        withVersionAccessor(BPMNVersionAccessor(sourceModelVersion, targetModelVersion))
        withConfigLocator(ClasspathMigratorConfigLocator("xml/config/BPMNMigratorConfig.xml"))
        val classLoader = BPMNModelValidator::class.java.getClassLoader()
        classLoader.getResource("validation/quirks.regex").openStream().use { quirksStream ->
            withSchemaValidator(
                JDomSchemaValidator(
                    arrayOf(
                        classLoader.getResource(BPMN_SCHEMA_PREFIX + "BPMN20.xsd"),
                        classLoader.getResource(BPMN_SCHEMA_PREFIX + "BPMNDI.xsd"),
                        classLoader.getResource(BPMN_SCHEMA_PREFIX + "DC.xsd"),
                        classLoader.getResource(BPMN_SCHEMA_PREFIX + "DI.xsd"),
                        classLoader.getResource(BPMN_SCHEMA_PREFIX + "Semantic.xsd"),
                    ),
                    quirksStream,
                ),
            )
        }
        withModelValidator(BPMNModelValidator())
    }
}
