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

package com.mgmtp.a12.model.workflows.migration.bpmn.validation

import com.mgmtp.a12.model.workflows.migration.exception.MigrationException
import com.mgmtp.a12.model.workflows.migration.utils.Constants
import com.mgmtp.a12.model.workflows.migration.utils.FileSystemUtil
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.equals.shouldBeEqual
import org.jdom2.Document
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

private class BPMNModelValidatorTest {

    companion object {
        var bpmnModelValidator: BPMNModelValidator = BPMNModelValidator()
        val BPMN_MODEL_PATH: Path = Paths.get(Constants.RESOURCE_PATH + "ParallelGatewayProcess.bpmn")
    }

    @Test
    fun `validate a valid BPMNModel`() {
        // given
        val document: Document = FileSystemUtil.getDocument(BPMN_MODEL_PATH)

        // when, then
        shouldNotThrowAny { bpmnModelValidator.validate(document, "8.0.0") }
    }

    @Test
    fun `should give an error because of an invalid model`() {
        // given
        val document: Document = FileSystemUtil.getDocument(BPMN_MODEL_PATH)

        // when, then
        val exception: Exception = shouldThrow<MigrationException> {
            bpmnModelValidator.validate(document, "9.0.0")
        }
        exception.message?.shouldBeEqual("The model is not a valid A12 bpmn model. Model Version 9.0.0 requires that formKey is set for every user task.")
    }
}
