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

package com.mgmtp.a12.workflows.extension.internal.utils

import com.mgmtp.a12.kernel.md.document.apiV2.DocumentPointer
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.DocumentV2
import com.mgmtp.a12.workflows.extension.testFixtures.TestResources.Names.DOCUMENT_MODEL_NAME_WITH_DEEPLY_NESTED_METADATA
import com.mgmtp.a12.workflows.extension.testFixtures.TestResources.Names.DOCUMENT_MODEL_NAME_WITH_METADATA
import com.mgmtp.a12.workflows.extension.testFixtures.TestResources.Names.DOCUMENT_MODEL_NAME_WITH_NESTED_METADATA
import com.mgmtp.a12.workflows.extension.testFixtures.testUtils.deserializeDocumentV2
import com.mgmtp.a12.workflows.shared.functions.testUtils.readResource
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPath.RELATIVE_TASK_ID_PATH
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.A12WF
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

private class DocumentV2ExtensionsTest {

    companion object {
        const val TASK_ID = "530d14f2-107f-11ec-8308-803253632adb"

        val documentModelString = readResource<String>(this::class, "/models/documentModels/$DOCUMENT_MODEL_NAME_WITH_METADATA.json")
        val documentModelWithNestedMetadataString =
            readResource<String>(this::class, "/models/documentModels/$DOCUMENT_MODEL_NAME_WITH_NESTED_METADATA.json")
        val documentModelWithDeeplyNestedMetadataString = readResource<String>(
            this::class,
            "/models/documentModels/$DOCUMENT_MODEL_NAME_WITH_DEEPLY_NESTED_METADATA.json",
        )

        val documentWithMetadataString = readResource<String>(this::class, "/documents/validDocument.json")
        val documentWithNestedMetadataString = readResource<String>(this::class, "/documents/validDocumentWithNestedMetadata.json")
        val documentWithDeeplyNestedMetadataString = readResource<String>(this::class, "/documents/validDocumentWithDeeplyNestedMetadata.json")
        val documentWithoutMetadataString = readResource<String>(this::class, "/documents/validDocumentWithoutMetadata.json")

        val documentWithMetadata = deserializeDocumentV2(documentWithMetadataString, documentModelString, DOCUMENT_MODEL_NAME_WITH_METADATA)
        val documentWithNestedMetadata = deserializeDocumentV2(documentWithNestedMetadataString, documentModelWithNestedMetadataString, DOCUMENT_MODEL_NAME_WITH_NESTED_METADATA)
        val documentWithDeeplyNestedMetadata = deserializeDocumentV2(documentWithDeeplyNestedMetadataString, documentModelWithDeeplyNestedMetadataString, DOCUMENT_MODEL_NAME_WITH_DEEPLY_NESTED_METADATA)
        val documentWithoutMetadata = deserializeDocumentV2(documentWithoutMetadataString, documentModelString, DOCUMENT_MODEL_NAME_WITH_METADATA)

        @JvmStatic
        fun documentsUnderTestProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(documentWithMetadata, DocumentPointer.of(A12WF)),
            Arguments.of(documentWithNestedMetadata, DocumentPointer.of("person/$A12WF")),
            Arguments.of(documentWithDeeplyNestedMetadata, DocumentPointer.of("person/level2/level3/$A12WF")),
        )
    }

    @ParameterizedTest
    @MethodSource("documentsUnderTestProvider")
    fun `should find correct DocumentPointer to A12WF metadata`(document: DocumentV2, expectedDocumentPointer: DocumentPointer) {
        // when
        val a12wfPointer = document.getDocumentPointerToA12wfMetadata()

        // then
        a12wfPointer shouldBe expectedDocumentPointer
    }

    @Test
    fun `should return null if A12WF metadata does not exist in document`() {
        // when
        val a12wfPointer = documentWithoutMetadata.getDocumentPointerToA12wfMetadata()

        // then
        a12wfPointer shouldBe null
    }

    @Test
    fun `should return value of Metadata field`() {
        // when
        val foundTaskID = documentWithNestedMetadata.getTextValueFromA12wfMetadata(RELATIVE_TASK_ID_PATH)

        // then
        foundTaskID shouldBe TASK_ID
    }

    @Test
    fun `should return null if Metadata field does not exist`() {
        // when
        val foundTaskID = documentWithoutMetadata.getTextValueFromA12wfMetadata(RELATIVE_TASK_ID_PATH)

        // then
        foundTaskID shouldBe null
    }

    @Test
    fun `should return null if Metadata exists but field does not exist`() {
        // when
        val foundTaskID = documentWithMetadata.getTextValueFromA12wfMetadata("/nonexistent/field")

        // then
        foundTaskID shouldBe null
    }
}
