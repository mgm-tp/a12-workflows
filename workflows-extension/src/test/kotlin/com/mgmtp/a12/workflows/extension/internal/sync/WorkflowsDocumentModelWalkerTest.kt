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

package com.mgmtp.a12.workflows.extension.internal.sync

import com.mgmtp.a12.kernel.md.document.api.services.DocumentDeserializationConfig
import com.mgmtp.a12.kernel.md.document.api.services.DocumentSerializationConfig
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.DocumentV2
import com.mgmtp.a12.kernel.md.facade.DocumentModelServiceFactory
import com.mgmtp.a12.kernel.md.facade.DocumentServiceFactory
import com.mgmtp.a12.kernel.md.model.api.IDocumentModel
import com.mgmtp.a12.kernel.md.model.api.services.IDocumentModelResolver
import com.mgmtp.a12.kernel.md.model.api.services.IDocumentModelSearchService
import com.mgmtp.a12.workflows.shared.dtos.ProcessEngineVariable
import com.mgmtp.a12.workflows.shared.functions.testUtils.readResource
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldNotContainKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.StringReader
import java.util.*
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
private class WorkflowsDocumentModelWalkerTest {

    companion object {

        val mockDocumentModelResolver: IDocumentModelResolver = mockk(relaxed = true)

        // Subject under test
        val spyWorkflowsDocumentModelWalker = spyk(WorkflowsDocumentModelWalker(mockDocumentModelResolver))

        val documentResource = readResource<String>(this::class, "/documents/validTestDocument.json")
        val documentModelResource = readResource<String>(this::class, "/models/documentModels/Test_DM.json")

        val jsonDeserializationConfig: DocumentDeserializationConfig = DocumentDeserializationConfig
            .builder()
            .format(DocumentSerializationConfig.Format.JSON)
            .build()

        val documentModel: IDocumentModel = DocumentModelServiceFactory()
            .createDocumentModelSerializer()
            .deserialize(StringReader(documentModelResource))

        val documentModelSearchService: IDocumentModelSearchService = DocumentModelServiceFactory()
            .createDocumentModelSearchService(documentModel)
    }

    var modificationMap = mapOf<String, ProcessEngineVariable<*>>()

    @BeforeAll
    fun walkDocument() {
        every { mockDocumentModelResolver.getDocumentModelById(any()) } returns documentModel
        every { mockDocumentModelResolver.getDocumentModelSearchService(any()) } returns Optional.of(documentModelSearchService)

        val documentServiceFactory = DocumentServiceFactory(mockDocumentModelResolver)

        val documentV2: DocumentV2 = documentServiceFactory
            .createDocumentV2Serializer()
            .deserializeV2(
                StringReader(documentResource),
                documentModel.header.id,
                jsonDeserializationConfig,
            ) { }

        val results = spyWorkflowsDocumentModelWalker.collectSyncAnnotatedFields(
            documentModel,
            documentV2,
        )
        modificationMap = results.modificationMap
    }

    @Test
    fun `document walking should find last value of annotated repeat`() {
        modificationMap shouldContain Pair("repeatedAnnotatedString", ProcessEngineVariable("repeatedAnnotatedString3"))
    }

    @Test
    fun `document walking should not find unannotated fields`() {
        modificationMap shouldNotContainKey "simpleNormalField"
    }

    @Test
    fun `document walking should find removed annotated fields`() {
        modificationMap shouldContain Pair("simpleAnnotatedRemovedField", ProcessEngineVariable(null))
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GivenAnnotatedFields {

        @ParameterizedTest(name = "{0} with value {1}")
        @MethodSource("expectedProcessVariableUpdates")
        fun `document walking should find variable and return`(annotationName: String, expectedValue: String?) {
            modificationMap shouldContain Pair(annotationName, ProcessEngineVariable(expectedValue))
        }

        fun expectedProcessVariableUpdates(): Stream<Arguments> = Stream.of(
            Arguments.of("simpleAnnotatedString", "simpleAnnotatedString"),
            Arguments.of("simpleAnnotatedBoolean", "true"),
            Arguments.of("simpleAnnotatedNumber", "1"),
            Arguments.of("simpleAnnotatedDate", "2023-01-01"),
            Arguments.of("simpleAnnotatedDateTime", "2023-01-01T01:01:01"),
            Arguments.of("simpleAnnotatedTime", "01:01:01"),
            Arguments.of("simpleAnnotatedDateFragment", "2023-01"),
            Arguments.of("simpleAnnotatedDateRange", "2023/2024"),
            Arguments.of("simpleAnnotatedConfirm", "simpleAnnotatedConfirm"),
            Arguments.of("simpleAnnotatedEnumeration", "VALUE_1"),
            Arguments.of("simpleComputatedAnnotatedField", "simpleComputatedAnnotatedField"),
            Arguments.of("simpleAnnotatedRemovedField", null),
            Arguments.of("simpleAnnotatedEmptyField", ""),
            Arguments.of("simpleAnnotatedNullField", null),
        )
    }
}
