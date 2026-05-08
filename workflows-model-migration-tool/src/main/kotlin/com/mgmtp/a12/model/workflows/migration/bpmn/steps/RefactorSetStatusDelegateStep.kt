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

import com.mgmtp.a12.model.workflows.migration.accessor.ElementQuery
import com.mgmtp.a12.model.workflows.migration.config.types.MigrationStepConfigType
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationLogger
import com.mgmtp.a12.model.workflows.migration.step.BaseMigrationStep
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace

class RefactorSetStatusDelegateStep(stepConfigType: MigrationStepConfigType) : BaseMigrationStep(stepConfigType) {

    companion object {
        const val OLD_VALUE_BEAN_NAME = "setDocumentFieldDelegate"
        const val NEW_VALUE_BEAN_NAME = "setStatusDelegate"
        const val ENABLED_INPUT_PARAMETER = "isValidationEnabled"
        const val SKIPPED_MESSAGE =
            "Skipped : The model doesn't use the service SetDocumentFieldDelegate. No need for a migration!"
        const val ATTRIBUT_MODEL_TEMPLATE = "modelerTemplate"
        const val ATTRIBUT_DELEGATE_EXPRESSION = "delegateExpression"
        const val SERVICE_XPATH_LOCATION = "//bpmn:serviceTask"
        const val INPUT_PARAMETERS_XPATH_LOCATION =
            "//bpmn:extensionElements/camunda:inputOutput/camunda:inputParameter"
        val ATTRIBUTS_NAMESPACE: Namespace = Namespace.getNamespace("camunda", "http://camunda.org/schema/1.0/bpmn")
    }

    override fun execute(document: Document, migrationLogger: MigrationLogger) {
        val servicesWithSetDocumentFieldDelegate = getServicesWhereSetDocumentFieldDelegateIsUsed(document)
        if (checkValidVersion(sourceModelVersion) && servicesWithSetDocumentFieldDelegate.isNotEmpty()) {
            ElementQuery.createForBPMN(document.getRootElement())
                .byXpath(SERVICE_XPATH_LOCATION)
                .forEach { serviceDelegate: Element ->
                    val modelTemplate = serviceDelegate.getAttribute(
                        ATTRIBUT_MODEL_TEMPLATE,
                        ATTRIBUTS_NAMESPACE,
                    )
                    if (modelTemplate != null && modelTemplate.value.contains(OLD_VALUE_BEAN_NAME)) {
                        modelTemplate.setValue(replaceSetDocumentFieldDelegateString(modelTemplate.value))
                    }
                    val delegateExpression =
                        serviceDelegate.getAttribute(ATTRIBUT_DELEGATE_EXPRESSION, ATTRIBUTS_NAMESPACE)
                    if (delegateExpression != null && delegateExpression.value.contains(OLD_VALUE_BEAN_NAME)) {
                        delegateExpression.setValue(replaceSetDocumentFieldDelegateString(delegateExpression.value))
                    }
                    removeEnabledParameterIfExists(serviceDelegate)
                }
        } else {
            println(String.format(SKIPPED_MESSAGE))
        }
    }

    fun checkValidVersion(modelVersion: String): Boolean = modelVersion.matches("10\\.[0-9]+\\.[0-9]+".toRegex())

    fun getServicesWhereSetDocumentFieldDelegateIsUsed(document: Document): List<Element> = ElementQuery.createForBPMN(document.getRootElement())
        .byXpath(SERVICE_XPATH_LOCATION)
        .stream().filter { serviceDelegate: Element ->
            val modelTemplate = serviceDelegate.getAttribute(ATTRIBUT_MODEL_TEMPLATE, ATTRIBUTS_NAMESPACE)
            val delegateExpression = serviceDelegate.getAttribute(ATTRIBUT_DELEGATE_EXPRESSION, ATTRIBUTS_NAMESPACE)
            val modelTemplateExist = modelTemplate != null && modelTemplate.value.contains(OLD_VALUE_BEAN_NAME)
            val delegateExpressionExist =
                delegateExpression != null && delegateExpression.value.contains(OLD_VALUE_BEAN_NAME)
            modelTemplateExist || delegateExpressionExist
        }.toList()

    private fun removeEnabledParameterIfExists(element: Element) {
        val query = ElementQuery.createForBPMN(element)
        val inputParameters = query.byXpath(INPUT_PARAMETERS_XPATH_LOCATION)
        inputParameters.forEach {
            val attribute = it.getAttribute("name")
            if (attribute != null && attribute.value == ENABLED_INPUT_PARAMETER) {
                it.parent.removeContent(it)
            }
        }
    }

    private fun replaceSetDocumentFieldDelegateString(value: String): String = value.replace(OLD_VALUE_BEAN_NAME, NEW_VALUE_BEAN_NAME)
}
