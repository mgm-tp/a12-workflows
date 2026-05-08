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
package com.mgmtp.a12.model.workflows.migration.bpmn.validation

import com.mgmtp.a12.model.workflows.migration.accessor.ElementQuery
import com.mgmtp.a12.model.workflows.migration.exception.MigrationException
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationError
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationErrorType
import com.mgmtp.a12.model.workflows.migration.validation.ModelValidator
import org.jdom2.Document
import org.jdom2.Namespace

class BPMNModelValidator : ModelValidator {

    companion object {
        val NS_BPMN: Namespace = Namespace.getNamespace("http://www.omg.org/spec/BPMN/20100524/MODEL")
        val NS_CAMUNDA: Namespace = Namespace.getNamespace("http://camunda.org/schema/1.0/bpmn")
    }

    override fun validate(modelDocument: Document?, modelVersion: String?) {
        if (modelVersion == "9.0.0") {
            checkFormKeyExists(modelDocument)
        }
    }

    @Throws(MigrationException::class)
    private fun checkFormKeyExists(modelDocument: Document?) {
        val query = ElementQuery.createForBPMN(modelDocument?.rootElement!!)
        val userTasks = query.byXpath("//bpmn:userTask")
        val failingElements = mutableListOf<String>()
        userTasks.forEach {
            val formKey = it.getAttribute("formKey", NS_CAMUNDA)
            if (formKey == null) failingElements.add(it.getAttribute("id").value)
        }
        if (failingElements.isNotEmpty()) {
            throw MigrationException(
                MigrationError(
                    MigrationErrorType.MODEL_CONTENT_NOT_SUPPORTED,
                    failingElements.toString(),
                    "The model is not a valid A12 bpmn model. Model Version 9.0.0 requires that formKey is set for every user task.",
                ),
            )
        }
    }
}
