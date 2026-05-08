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
package com.mgmtp.a12.model.workflows.migration.bpmn.steps

import com.mgmtp.a12.model.workflows.migration.accessor.ElementQuery.Companion.createForBPMN
import com.mgmtp.a12.model.workflows.migration.config.types.MigrationStepConfigType
import com.mgmtp.a12.model.workflows.migration.utils.Constants
import com.mgmtp.a12.model.workflows.migration.utils.TestMigrationLogger
import com.mgmtp.a12.model.workflows.migration.utils.XmlUtil.parseModel
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

private class RemoveAllExtensionPropertiesStepTest {

    @Test
    fun `should remove extension properties`() {
        // given
        val stream = Files.newInputStream(Paths.get("${Constants.RESOURCE_PATH}ParallelGatewayProcess.bpmn"))
        val document = parseModel(stream)

        // when
        val migration = RemoveAllExtensionPropertiesStep(MigrationStepConfigType())
        migration.execute(document, TestMigrationLogger())
        val camundaPropertiesList = createForBPMN(document.getRootElement())
            .byXpath("//bpmn:extensionElements/camunda:properties/camunda:property")

        // then
        camundaPropertiesList.size shouldBe 0
    }
}
