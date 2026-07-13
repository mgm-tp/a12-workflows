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

import com.mgmtp.a12.model.workflows.migration.accessor.ElementQuery.Companion.createForBPMN
import com.mgmtp.a12.model.workflows.migration.bpmn.steps.RefactorSetStatusDelegateStep.Companion.ATTRIBUTS_NAMESPACE
import com.mgmtp.a12.model.workflows.migration.bpmn.steps.RefactorSetStatusDelegateStep.Companion.ENABLED_INPUT_PARAMETER
import com.mgmtp.a12.model.workflows.migration.bpmn.steps.RefactorSetStatusDelegateStep.Companion.OLD_VALUE_BEAN_NAME
import com.mgmtp.a12.model.workflows.migration.config.types.MigrationStepConfigType
import com.mgmtp.a12.model.workflows.migration.utils.Constants.RESOURCE_PATH
import com.mgmtp.a12.model.workflows.migration.utils.Constants.SET_STATUS_DELEGATE_MIGRATION_FOLDER_NAME
import com.mgmtp.a12.model.workflows.migration.utils.FileSystemUtil.getDocument
import com.mgmtp.a12.model.workflows.migration.utils.TestMigrationLogger
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.jdom2.Attribute
import org.jdom2.Document
import org.jdom2.Element
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Paths
import java.util.*
import java.util.stream.Stream

private class RefactorSetStatusDelegateStepTest {

    companion object {
        val SET_DOCUMENT_FIELD_DELEGATE_PATH = Paths.get(
            RESOURCE_PATH,
            SET_STATUS_DELEGATE_MIGRATION_FOLDER_NAME,
            "DocumentUsing_SetDocumentFieldDelegate.bpmn",
        )
        val DOCUMENT_USING_SET_DOCUMENT_FIELD_DELEGATE = Paths.get(
            RESOURCE_PATH,
            SET_STATUS_DELEGATE_MIGRATION_FOLDER_NAME,
            "DocumentUsing_SetDocumentFieldDelegate.bpmn",
        )
        val DOCUMENT_NOT_USING_SET_DOCUMENT_FIELD_DELEGATE = Paths.get(
            RESOURCE_PATH,
            SET_STATUS_DELEGATE_MIGRATION_FOLDER_NAME,
            "DocumentNotUsing_SetDocumentFieldDelegate.bpmn",
        )

        @JvmStatic
        fun generateVersions(): Stream<Arguments?> {
            val versions: MutableList<Arguments?> = ArrayList()
            val maxFirstVersionRange = 5
            val maxSecondVersionRange = 5
            val validVersionPayload = "10.%d.%d"
            val invalidVersionPayload = "9.%d.%d"
            for (i in 0..maxFirstVersionRange) {
                for (j in 0..maxSecondVersionRange) {
                    versions.add(Arguments.of(String.format(validVersionPayload, i, j), true))
                    versions.add(Arguments.of(String.format(invalidVersionPayload, i, j), false))
                }
            }
            versions.shuffle()
            return versions.stream()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [RefactorSetStatusDelegateStep.ATTRIBUT_DELEGATE_EXPRESSION, RefactorSetStatusDelegateStep.ATTRIBUT_MODEL_TEMPLATE])
    fun `should migrate uses of SetDocumentFieldDelegate in delegateExpression and modelTemplate`(attributeString: String) {
        // given
        val document: Document = getDocument(SET_DOCUMENT_FIELD_DELEGATE_PATH)
        val stepConfigType = MigrationStepConfigType()
        stepConfigType.sourceModelVersion = "10.0.0"
        val migration = RefactorSetStatusDelegateStep(stepConfigType)

        // when
        migration.execute(document, TestMigrationLogger())
        val serviceTaskList = createForBPMN(document.getRootElement())
            .byXpath(RefactorSetStatusDelegateStep.SERVICE_XPATH_LOCATION)

        // then
        serviceTaskList.stream()
            .map { it.getAttribute(attributeString, ATTRIBUTS_NAMESPACE) }
            .filter { Objects.nonNull(it) }
            .forEach { attribute: Attribute -> attribute.value shouldNotContain OLD_VALUE_BEAN_NAME }
    }

    @Test
    fun `should migrate uses of IsValidationEnabled parameter in SetDocumentFieldDelegate service task`() {
        val document: Document = getDocument(SET_DOCUMENT_FIELD_DELEGATE_PATH)
        val stepConfigType = MigrationStepConfigType()
        stepConfigType.sourceModelVersion = "10.0.0"
        val migration = RefactorSetStatusDelegateStep(stepConfigType)

        // when
        migration.execute(document, TestMigrationLogger())
        val serviceTaskList = createForBPMN(document.getRootElement())
            .byXpath(RefactorSetStatusDelegateStep.SERVICE_XPATH_LOCATION)

        // then
        serviceTaskList.stream()
            .flatMap {
                createForBPMN(it).byXpath(RefactorSetStatusDelegateStep.INPUT_PARAMETERS_XPATH_LOCATION).stream()
            }
            .map { parameter: Element -> parameter.getAttribute("name") }
            .filter { obj: Attribute? -> Objects.nonNull(obj) }
            .forEach { attribute: Attribute -> attribute shouldNotBeEqual ENABLED_INPUT_PARAMETER }
    }

    @ParameterizedTest
    @MethodSource("generateVersions")
    fun `should check the version validation functionality`(version: String, expectedVersion: Boolean) {
        // given
        val migration = RefactorSetStatusDelegateStep(MigrationStepConfigType())

        // when
        val check = migration.checkValidVersion(version)

        // then
        check shouldBe expectedVersion
    }

    @Test
    fun `should get services that use SetDocumentFieldDelegate`() {
        // given
        val document: Document = getDocument(DOCUMENT_USING_SET_DOCUMENT_FIELD_DELEGATE)
        val migration = RefactorSetStatusDelegateStep(MigrationStepConfigType())

        // when
        val services = migration.getServicesWhereSetDocumentFieldDelegateIsUsed(document)

        // then
        services.shouldNotBeEmpty()
    }

    @Test
    fun `should get an empty list of services that use SetDocumentFieldDelegate`() {
        // given
        val document: Document = getDocument(DOCUMENT_NOT_USING_SET_DOCUMENT_FIELD_DELEGATE)
        val migration = RefactorSetStatusDelegateStep(MigrationStepConfigType())

        // when
        val services = migration.getServicesWhereSetDocumentFieldDelegateIsUsed(document)

        // then
        services.shouldBeEmpty()
    }
}
