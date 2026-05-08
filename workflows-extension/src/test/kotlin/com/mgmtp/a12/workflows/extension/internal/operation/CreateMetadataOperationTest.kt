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
package com.mgmtp.a12.workflows.extension.internal.operation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mgmtp.a12.dataservices.document.DataServicesDocument
import com.mgmtp.a12.dataservices.document.DataServicesDocumentFactory
import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.dataservices.document.DocumentService
import com.mgmtp.a12.dataservices.document.internal.DefaultDataServicesDocument
import com.mgmtp.a12.dataservices.document.internal.DefaultDocumentMetadata
import com.mgmtp.a12.dataservices.document.internal.MetadataUtils
import com.mgmtp.a12.dataservices.document.persistence.DocumentComputationStrategy
import com.mgmtp.a12.dataservices.document.persistence.DocumentValidationStrategy
import com.mgmtp.a12.dataservices.document.persistence.IDocumentRepository
import com.mgmtp.a12.dataservices.model.ModelService
import com.mgmtp.a12.dataservices.model.document.persistence.internal.DocumentModelLoader
import com.mgmtp.a12.dataservices.rpc.RpcException
import com.mgmtp.a12.dataservices.utils.internal.DocumentModelUtils
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.DocumentV2
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.GroupInstanceV2
import com.mgmtp.a12.kernel.md.facade.DocumentModelServiceFactory
import com.mgmtp.a12.model.header.DefaultHeaderParser
import com.mgmtp.a12.model.header.HeaderParser
import com.mgmtp.a12.uaa.authorization.DecisionContext
import com.mgmtp.a12.workflows.extension.internal.exception.WorkflowsRpcExceptionHandler
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithCDM.CDM_FORM_MODEL_NAME
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithCDM.COMPOSED_DOCUMENT_MODEL_NAME
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithCDM.ROOT_DOCUMENT_MODEL_DOC_REF_STRING
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithCDM.ROOT_DOCUMENT_MODEL_NAME
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithCDM.cdmFormModelString
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithCDM.composedDocumentModel
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithCDM.metadataForCdm
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithCDM.rootDocumentModel
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithCDM.rootDocumentWithoutMetadata
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.DOCUMENT_MODEL_NAME_WITHOUT_METADATA
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.DOC_REF_STRING
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.DOC_REF_WITHOUT_METADATA_STRING
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.DOC_REF_WITH_DEEPLY_NESTED_METADATA_STRING
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.DOC_REF_WITH_NESTED_METADATA_STRING
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.FORM_MODEL_NAME
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.FORM_MODEL_NAME_FOR_NESTED_METADATA
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.documentModel
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.documentModelWithDeeplyNestedMetadata
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.documentModelWithNestedMetadata
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.documentModelWithoutMetadata
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.documentWithMetadata
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.documentWithNestedMetadata
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.documentWithoutMetadata
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.formModelForNestedMetadataString
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.formModelString
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.metadata
import com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest.CreateMetadataOperationWithRegularDM.timeZone
import com.mgmtp.a12.workflows.extension.internal.operation.constants.InternalDocumentStatus
import com.mgmtp.a12.workflows.extension.internal.testUtils.buildGenericModel
import com.mgmtp.a12.workflows.extension.internal.utils.WorkflowsDocumentService
import com.mgmtp.a12.workflows.extension.testFixtures.TestResources.Names.DOCUMENT_MODEL_NAME_WITH_DEEPLY_NESTED_METADATA
import com.mgmtp.a12.workflows.extension.testFixtures.TestResources.Names.DOCUMENT_MODEL_NAME_WITH_METADATA
import com.mgmtp.a12.workflows.extension.testFixtures.TestResources.Names.DOCUMENT_MODEL_NAME_WITH_NESTED_METADATA
import com.mgmtp.a12.workflows.extension.testFixtures.testUtils.TestDocumentMetadataMetaModelProvider
import com.mgmtp.a12.workflows.extension.testFixtures.testUtils.deserializeDocumentModel
import com.mgmtp.a12.workflows.extension.testFixtures.testUtils.deserializeDocumentV2
import com.mgmtp.a12.workflows.shared.dtos.ProcessMetadata
import com.mgmtp.a12.workflows.shared.dtos.TaskDefinition
import com.mgmtp.a12.workflows.shared.functions.testUtils.readResource
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.A12WF
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.ASSIGNEE
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.BUSINESS_KEY
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.CREATION_DATE
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.DUE_DATE
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.FORM_KEY
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.ID
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.INITIATOR
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.KEY
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.NAME
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.PROCESS
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.PROCESS_DEFINITION
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.TASK
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.TASK_DEFINITION
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.WORKFLOWS_INTERNAL_DOCUMENT_STATUS
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.beInstanceOf
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.io.InputStreamReader
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.function.BiConsumer
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.valueParameters

private class CreateMetadataOperationTest {

    val mockWorkflowsDocumentService: WorkflowsDocumentService = mockk(relaxed = true)

    val headerParser: HeaderParser = DefaultHeaderParser()
    val documentModelServiceFactory = DocumentModelServiceFactory()
    val documentModelSerializer = documentModelServiceFactory.createDocumentModelSerializer()
    val documentModelUtils = DocumentModelUtils(documentModelServiceFactory, documentModelSerializer, headerParser)
    val documentMetadataMetaModelProvider = spyk(TestDocumentMetadataMetaModelProvider(documentModelUtils))
    val mockModelService: ModelService = mockk(relaxed = true)
    val spyObjectMapper: ObjectMapper = spyk(jacksonObjectMapper())
    val mockDocumentRepository: IDocumentRepository = mockk(relaxed = true)
    val mockDocumentModelLoader: DocumentModelLoader = mockk(relaxed = true)

    val mockDocumentV2: DocumentV2 = mockk(relaxed = true)
    val mockDataServicesDocument: DataServicesDocument = mockk(relaxed = true)
    val mockDataServicesDocumentFactory: DataServicesDocumentFactory = mockk(relaxed = true)
    val spyDocumentService: DocumentService = spyk(recordPrivateCalls = true)

    val createMetadataOperation = CreateMetadataOperation(
        mockWorkflowsDocumentService,
        mockModelService,
        mockDocumentModelLoader,
        spyDocumentService,
        WorkflowsRpcExceptionHandler(spyObjectMapper),
    )

    object CreateMetadataOperationWithRegularDM {
        const val DOCUMENT_MODEL_NAME_WITHOUT_METADATA = "SimplePersonWithoutMetadata_DM"
        const val FORM_MODEL_NAME = "SimpleFillApplicant_FM"
        const val FORM_MODEL_NAME_FOR_NESTED_METADATA = "SimpleFillApplicantForDocumentModelWithNestedMetadata_FM"

        const val DOC_REF_STRING = "$DOCUMENT_MODEL_NAME_WITH_METADATA/1"
        const val DOC_REF_WITH_NESTED_METADATA_STRING = "$DOCUMENT_MODEL_NAME_WITH_NESTED_METADATA/1"
        const val DOC_REF_WITH_DEEPLY_NESTED_METADATA_STRING = "$DOCUMENT_MODEL_NAME_WITH_DEEPLY_NESTED_METADATA/1"
        const val DOC_REF_WITHOUT_METADATA_STRING = "$DOCUMENT_MODEL_NAME_WITHOUT_METADATA/1"

        val documentModelString = readResource<String>(this::class, "/models/documentModels/$DOCUMENT_MODEL_NAME_WITH_METADATA.json")
        val documentModelWithNestedMetadataString =
            readResource<String>(this::class, "/models/documentModels/$DOCUMENT_MODEL_NAME_WITH_NESTED_METADATA.json")
        val documentModelWithDeeplyNestedMetadataString = readResource<String>(
            this::class,
            "/models/documentModels/$DOCUMENT_MODEL_NAME_WITH_DEEPLY_NESTED_METADATA.json",
        )
        val documentModelWithoutMetadataString =
            readResource<String>(this::class, "/models/documentModels/$DOCUMENT_MODEL_NAME_WITHOUT_METADATA.json")

        val documentModel = deserializeDocumentModel(documentModelString)
        val documentModelWithNestedMetadata = deserializeDocumentModel(documentModelWithNestedMetadataString)
        val documentModelWithDeeplyNestedMetadata = deserializeDocumentModel(documentModelWithDeeplyNestedMetadataString)
        val documentModelWithoutMetadata = deserializeDocumentModel(documentModelWithoutMetadataString)

        val formModelString = readResource<String>(this::class, "/models/formModels/$FORM_MODEL_NAME.json")
        val formModelForNestedMetadataString =
            readResource<String>(this::class, "/models/formModels/$FORM_MODEL_NAME_FOR_NESTED_METADATA.json")

        val documentWithMetadataString = readResource<String>(this::class, "/documents/validDocument.json")
        val documentWithNestedMetadataString =
            readResource<String>(this::class, "/documents/validDocumentWithNestedMetadata.json")
        val documentWithDeeplyNestedMetadataString = readResource<String>(this::class, "/documents/validDocumentWithDeeplyNestedMetadata.json")
        val documentWithoutMetadataString =
            readResource<String>(this::class, "/documents/validDocumentWithoutMetadata.json")

        val documentWithMetadata = deserializeDocumentV2(
            documentWithMetadataString,
            documentModelString,
            DOCUMENT_MODEL_NAME_WITH_METADATA,
        )
        val documentWithNestedMetadata = deserializeDocumentV2(
            documentWithNestedMetadataString,
            documentModelWithNestedMetadataString,
            DOCUMENT_MODEL_NAME_WITH_NESTED_METADATA,
        )
        val documentWithDeeplyNestedMetadata = deserializeDocumentV2(
            documentWithDeeplyNestedMetadataString,
            documentModelWithDeeplyNestedMetadataString,
            DOCUMENT_MODEL_NAME_WITH_DEEPLY_NESTED_METADATA,
        )
        val documentWithoutMetadata = deserializeDocumentV2(
            documentWithoutMetadataString,
            documentModelString,
            DOCUMENT_MODEL_NAME_WITH_METADATA,
        )

        val metadata = readResource<JsonNode>(this::class, "/documents/a12wf-metadata.json")

        // timeZone is only determined once, since it is the same defined for every DM used in the tests.
        val timeZone = deserializeDocumentModel(documentModelString).content.documentModelConfig.timeZone
    }

    object CreateMetadataOperationWithCDM {
        const val COMPOSED_DOCUMENT_MODEL_NAME = "TeamEmployee_CDM"
        const val ROOT_DOCUMENT_MODEL_NAME = "Team_DM"
        const val CDM_FORM_MODEL_NAME = "TeamEmployee_CFM"

        const val ROOT_DOCUMENT_MODEL_DOC_REF_STRING = "$ROOT_DOCUMENT_MODEL_NAME/1"

        val composedDocumentModelString =
            readResource<String>(this::class, "/models/cdm/$COMPOSED_DOCUMENT_MODEL_NAME.json")
        val rootDocumentModelString =
            readResource<String>(this::class, "/models/cdm/$ROOT_DOCUMENT_MODEL_NAME.json")

        val composedDocumentModel = deserializeDocumentModel(composedDocumentModelString)
        val rootDocumentModel = deserializeDocumentModel(rootDocumentModelString)

        val cdmFormModelString = readResource<String>(this::class, "/models/formModels/$CDM_FORM_MODEL_NAME.json")

        val rootDocumentWithoutMetadataString = readResource<String>(this::class, "/documents/cdd/validRootDocumentWithoutMetadata.json")
        val rootDocumentWithoutMetadata = deserializeDocumentV2(rootDocumentWithoutMetadataString, rootDocumentModelString, ROOT_DOCUMENT_MODEL_NAME)

        val metadataForCdm =
            readResource<JsonNode>(this::class, "/documents/cdd/a12wf-metadata-cdm.json")
    }

    companion object {
        lateinit var expectedMetadataNode: JsonNode
        lateinit var expectedMetadata: ProcessMetadata
        lateinit var personName: String
        lateinit var teamName: String

        @JvmStatic
        fun documentsToTest(): List<Arguments> = listOf(
            Arguments.of(DOC_REF_STRING, documentWithMetadata),
            Arguments.of(DOC_REF_WITH_NESTED_METADATA_STRING, documentWithNestedMetadata),
        )

        @JvmStatic
        fun documentsToTestWithAssertion(): List<Arguments> = listOf(
            Arguments.of(DOC_REF_STRING, BiConsumer<DocumentV2, JsonNode> { result, metadataNode -> assertGroupEqualToJson(result.group(A12WF), metadataNode) }),
            Arguments.of(DOC_REF_WITH_NESTED_METADATA_STRING, BiConsumer<DocumentV2, JsonNode> { result, metadataNode -> assertGroupEqualToJson(result.group("/person/$A12WF"), metadataNode) }),
            Arguments.of(DOC_REF_WITH_DEEPLY_NESTED_METADATA_STRING, BiConsumer<DocumentV2, JsonNode> { result, metadataNode -> assertGroupEqualToJson(result.group("/person/level2/level3/$A12WF"), metadataNode) }),
        )

        fun assertGroupEqualToJson(group: GroupInstanceV2, other: JsonNode) {
            group.fieldValue("$PROCESS_DEFINITION/$KEY") shouldBe other.at("/$PROCESS_DEFINITION").findValue(KEY).asText()
            group.fieldValue("$PROCESS_DEFINITION/$NAME") shouldBe other.at("/$PROCESS_DEFINITION").findValue(NAME).asText()

            group.fieldValue("$TASK_DEFINITION/$KEY") shouldBe other.at("/$TASK_DEFINITION").findValue(KEY).asText()
            group.fieldValue("$TASK_DEFINITION/$NAME") shouldBe other.at("/$TASK_DEFINITION").findValue(NAME).asText()
            group.fieldValue("$TASK_DEFINITION/$FORM_KEY") shouldBe other.at("/$TASK_DEFINITION").findValue(FORM_KEY).asText()

            group.fieldValue("$PROCESS/$ID") shouldBe other.at("/$PROCESS").findValue(ID).asText()
            group.fieldValue("$PROCESS/$BUSINESS_KEY") shouldBe other.at("/$PROCESS").findValue(BUSINESS_KEY).asText()
            group.fieldValue("$PROCESS/$INITIATOR") shouldBe other.at("/$PROCESS").findValue(INITIATOR).asText()

            group.fieldValue("$TASK/$ID") shouldBe other.at("/$TASK").findValue(ID).asText()
            group.fieldValue("$TASK/$CREATION_DATE") shouldBe LocalDateTime.parse(other.at("/$TASK").findValue(CREATION_DATE).asText()).atZone(ZoneId.of(timeZone)).toInstant()
            group.fieldValue("$TASK/$DUE_DATE") shouldBe null
            group.fieldValue("$TASK/$ASSIGNEE") shouldBe other.at("/$TASK").findValue(ASSIGNEE).asText()

            group.fieldValue(WORKFLOWS_INTERNAL_DOCUMENT_STATUS) shouldBe other.findValue(WORKFLOWS_INTERNAL_DOCUMENT_STATUS).asText()
        }
    }

    @BeforeEach
    fun setup() {
        every { mockDocumentRepository.findByDocumentReference(any()) } returns
            Optional.of(mockDataServicesDocument)
        every { mockDataServicesDocumentFactory.newDataServicesDocument(mockDocumentV2) } returns mockDataServicesDocument
        expectedMetadataNode = metadata.findValue("A12WF")
        expectedMetadata = spyObjectMapper.treeToValue(expectedMetadataNode, ProcessMetadata::class.java)
        personName = documentWithoutMetadata.fieldValue("/person/name").toString()

        every { documentMetadataMetaModelProvider.model } returns documentModelUtils.deserializeDocumentModel(InputStreamReader(this.javaClass.getResourceAsStream(documentMetadataMetaModelProvider.documentMetadataPath)))

        every { spyDocumentService.create(any(), any(), any(), any()) } answers {
            var document = firstArg<DocumentV2>()
            document = MetadataUtils(documentMetadataMetaModelProvider, documentModelServiceFactory).createDocumentMetadata(document, DocumentReference(DOC_REF_STRING), "admin", Instant.now(), "1.0.0")
            DefaultDataServicesDocument(document, DefaultDocumentMetadata(document))
        }
        every { spyDocumentService.update(any(), any<DocumentV2>(), any(), any(), any()) } answers {
            var document = secondArg<DocumentV2>()
            document = MetadataUtils(documentMetadataMetaModelProvider, documentModelServiceFactory).createDocumentMetadata(document, firstArg(), "admin", Instant.now(), "1.0.0")
            DefaultDataServicesDocument(document, DefaultDocumentMetadata(document))
        }
        configureMocksForRegularDocuments()
    }

    @Nested
    @DisplayName("Given document is a regular DM")
    inner class GivenDocumentIsRegularDM {

        @AfterEach
        fun clear() {
            clearAllMocks(currentThreadOnly = true)
        }

        @Nested
        @DisplayName("Given document is already referenced (has metadata)...")
        inner class GivenDocumentAlreadyReferenced {

            @ParameterizedTest
            @MethodSource("com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest#documentsToTest")
            fun `should throw DocumentAlreadyReferencedException because document already contains task ID`(docRef: DocumentReference, document: DocumentV2) {
                // given
                every { mockWorkflowsDocumentService.loadDocument(any()) } returns document

                // when/then
                val exception =
                    shouldThrow<ResponseStatusException> { createMetadataOperation.rpc(docRef, expectedMetadata) }
                exception should beInstanceOf<ResponseStatusException>()
                exception.statusCode shouldBe HttpStatus.BAD_REQUEST
                exception.message shouldContain "Metadata for document [$docRef] is not empty"
            }
        }

        @Nested
        @DisplayName("Given document is not yet referenced by any user task (has no metadata)...")
        inner class GivenDocumentIsUnreferenced {

            @Nested
            @DisplayName("When user task has docRef to reference existing document...")
            inner class WhenUserTaskReferencesExistingDocument {

                @ParameterizedTest
                @MethodSource("com.mgmtp.a12.workflows.extension.internal.operation.CreateMetadataOperationTest#documentsToTestWithAssertion")
                fun `should fill in metadata and update existing root`(docRefString: String, assertion: BiConsumer<DocumentV2, JsonNode>) {
                    // given
                    val docRef = DocumentReference(docRefString)
                    val documentSlot = slot<DocumentV2>()

                    // when
                    createMetadataOperation.rpc(docRef, expectedMetadata)

                    // then
                    verify(exactly = 1) { spyDocumentService.update(docRef, capture(documentSlot), any(), any(), any()) }
                    val metadataNode = addWorkflowsInternalDocumentStatusToExpectedMetadataForAssertion(InternalDocumentStatus.NON_EMPTY.toString())
                    val updatedDocument = documentSlot.captured
                    assertion.accept(updatedDocument, metadataNode)
                    updatedDocument.fieldValue("/person/name") shouldBe personName
                }

                @Test
                fun `should throw NoSuchElementException if document model does not contain A12WF metadata`() {
                    // given
                    val docRef = DocumentReference(DOC_REF_WITHOUT_METADATA_STRING)

                    // when/then
                    val exception = shouldThrow<ResponseStatusException> {
                        createMetadataOperation.rpc(docRef, expectedMetadata)
                    }
                    exception should beInstanceOf<ResponseStatusException>()
                    exception.statusCode shouldBe HttpStatus.BAD_REQUEST
                    exception.message shouldContain "Could not find A12WF group in model ${docRef.documentModelName}"
                }

                @Test
                fun `should update document using document service without validation`() {
                    // given
                    val docRef = DocumentReference(DOC_REF_STRING)

                    // when
                    createMetadataOperation.rpc(docRef, expectedMetadata)

                    // then
                    verify(exactly = 1) {
                        spyDocumentService.update(
                            docRef,
                            any<DocumentV2>(),
                            null,
                            DocumentValidationStrategy.NO_VALIDATION,
                            DocumentComputationStrategy.DEFAULT_CONFIGURATION,
                        )
                    }
                }
            }

            @Nested
            @DisplayName("When user task initializes new document based on form key...")
            inner class WhenUserTaskCreatesNewDocument {

                @Test
                fun `should use document model name referenced by form model given by form key`() {
                    // given
                    val expectedDocumentModelReferencedByFormModel = DOCUMENT_MODEL_NAME_WITH_METADATA
                    val documentSlot = slot<DocumentV2>()

                    // when
                    createMetadataOperation.rpc(null, expectedMetadata)

                    // then
                    verify(exactly = 1) { spyDocumentService.create(capture(documentSlot), any(), any(), any()) }
                    documentSlot.captured.documentModelId shouldBe expectedDocumentModelReferencedByFormModel
                }

                @Test
                fun `should fill in metadata and create new document`() {
                    // given
                    val documentSlot = slot<DocumentV2>()

                    // when
                    createMetadataOperation.rpc(null, expectedMetadata)

                    // then
                    verify(exactly = 1) { spyDocumentService.create(capture(documentSlot), any(), any(), any()) }
                    val metadataNode = addWorkflowsInternalDocumentStatusToExpectedMetadataForAssertion(InternalDocumentStatus.EMPTY.toString())
                    assertGroupEqualToJson(documentSlot.captured.group(A12WF), metadataNode)
                }

                @Test
                fun `should fill in nested metadata and create new document`() {
                    // given
                    val documentSlot = slot<DocumentV2>()
                    // Manipulate metadata so the right Form model is loaded.
                    val (taskDefinitionKey, taskDefinitionName, _) = expectedMetadata.taskDefinition
                    val taskDefinitionWithDefinedFormKey = TaskDefinition(taskDefinitionKey, taskDefinitionName, FORM_MODEL_NAME_FOR_NESTED_METADATA)
                    expectedMetadata = expectedMetadata.copy(
                        taskDefinition = taskDefinitionWithDefinedFormKey,
                    )

                    // when
                    createMetadataOperation.rpc(null, expectedMetadata)

                    // then
                    verify(exactly = 1) { spyDocumentService.create(capture(documentSlot), any(), any(), any()) }
                    val metadataNode = addWorkflowsInternalDocumentStatusToExpectedMetadataForAssertion(InternalDocumentStatus.EMPTY.toString())
                    assertGroupEqualToJson(documentSlot.captured.group(("/person/$A12WF")), metadataNode)
                }

                @Test
                fun `should create document using document service without validation`() {
                    // when
                    createMetadataOperation.rpc(null, expectedMetadata)

                    // then
                    verify { spyDocumentService.create(any(), null, DocumentValidationStrategy.NO_VALIDATION, DocumentComputationStrategy.DEFAULT_CONFIGURATION) }
                }
            }
        }
    }

    @Nested
    @DisplayName("Given document is a cdd and has not yet been referenced by any user task (it lacks metadata).")
    inner class GivenDocumentIsCdm {

        @BeforeEach
        fun setup() {
            expectedMetadataNode = metadataForCdm.findValue("A12WF")
            expectedMetadata = spyObjectMapper.treeToValue(expectedMetadataNode, ProcessMetadata::class.java)
            teamName = rootDocumentWithoutMetadata.fieldValue("/Team/TeamName") as String
            configureMocksForCdmCase()
        }

        @AfterEach
        fun clear() {
            clearAllMocks(currentThreadOnly = true)
        }

        @Nested
        @DisplayName("When user task initializes new root document based on form key...")
        inner class WhenUserTaskCreatesNewRootDocument {

            @Test
            fun `should use the root document model name of CDM, which is referenced by the CDM Form model given by form key`() {
                // given
                val documentSlot = slot<DocumentV2>()

                // when
                createMetadataOperation.rpc(null, expectedMetadata)

                // then
                verify(exactly = 1) { spyDocumentService.create(capture(documentSlot), any(), any(), any()) }
                documentSlot.captured.documentModelId shouldBe ROOT_DOCUMENT_MODEL_NAME
            }

            @Test
            fun `should fill in metadata and create new root document`() {
                // given
                val documentSlot = slot<DocumentV2>()

                // when
                createMetadataOperation.rpc(null, expectedMetadata)

                // then
                verify(exactly = 1) { spyDocumentService.create(capture(documentSlot), any(), any(), any()) }
                val metadataNode = addWorkflowsInternalDocumentStatusToExpectedMetadataForAssertion(InternalDocumentStatus.EMPTY.toString())
                assertGroupEqualToJson(documentSlot.captured.group("/Team/$A12WF"), metadataNode)
            }
        }

        @Nested
        @DisplayName("When user task has docRef to reference existing root document...")
        inner class WhenUserTaskReferencesExistingRootDocument {

            @Test
            fun `should fill in metadata and update existing root document`() {
                // given
                val docRef = DocumentReference(ROOT_DOCUMENT_MODEL_DOC_REF_STRING)
                val documentSlot = slot<DocumentV2>()

                // when
                createMetadataOperation.rpc(docRef, expectedMetadata)

                // then
                verify(exactly = 1) { spyDocumentService.update(docRef, capture(documentSlot), any(), any(), any()) }
                val metadataNode =
                    addWorkflowsInternalDocumentStatusToExpectedMetadataForAssertion(InternalDocumentStatus.NON_EMPTY.toString())
                assertGroupEqualToJson(documentSlot.captured.group(("/Team/$A12WF")), metadataNode)
                documentSlot.captured.fieldValue("/Team/TeamName") shouldBe teamName
            }
        }

        fun configureMocksForCdmCase() {
            every { mockDocumentModelLoader.loadModel(ROOT_DOCUMENT_MODEL_NAME) } returns rootDocumentModel
            every { mockDocumentModelLoader.loadModel(COMPOSED_DOCUMENT_MODEL_NAME) } returns composedDocumentModel
            every { mockModelService.load(CDM_FORM_MODEL_NAME) } returns buildGenericModel(cdmFormModelString)

            every { mockWorkflowsDocumentService.loadDocument(DocumentReference(ROOT_DOCUMENT_MODEL_DOC_REF_STRING)) } returns rootDocumentWithoutMetadata
        }
    }

    @Nested
    @DisplayName("Given exception in repository")
    inner class GivenExceptionInRepository {

        val commonException = Exception("some error message")

        @Test
        fun `should throw RpcException if common exception is thrown`() {
            // given
            every { spyDocumentService.create(any(), any(), any(), any()) } throws commonException

            // when/then
            shouldThrow<RpcException> { createMetadataOperation.rpc(null, expectedMetadata) }
        }
    }

    /**
     * Testing the EvaluationContext for this operation would take too much additional setup effort
     * so instead we just assume @DecisionContext works correctly and check if the annotations are present
     */
    @Test
    fun `should have correct @DecisionContext annotations`() {
        // given
        val opClass = CreateMetadataOperation::class
        val annotatedMethod = opClass.memberFunctions.find { it.name == "rpc" }
        val methodParameters = annotatedMethod!!.valueParameters
        val annotatedParameters = methodParameters.filter { it.findAnnotation<DecisionContext>() != null }
        val unAnnotatedParameters = methodParameters.minus(annotatedParameters)

        // Add parameter names here if they are intentionally unannotated
        val excludedParameters = listOf<String>()

        // then
        // With this check we can make sure that there are no unintentionally unannotated parameters
        unAnnotatedParameters.map { it.name } shouldContainOnly excludedParameters
        annotatedParameters.map { it.name } shouldContainOnly listOf("docRef", "metadata")
        annotatedParameters.forEach { it.name shouldBe it.findAnnotation<DecisionContext>()!!.value }
    }

    fun configureMocksForRegularDocuments() {
        every { mockDocumentModelLoader.loadModel(DOCUMENT_MODEL_NAME_WITH_METADATA) } returns documentModel
        every { mockDocumentModelLoader.loadModel(DOCUMENT_MODEL_NAME_WITH_NESTED_METADATA) } returns documentModelWithNestedMetadata
        every { mockDocumentModelLoader.loadModel(DOCUMENT_MODEL_NAME_WITH_DEEPLY_NESTED_METADATA) } returns documentModelWithDeeplyNestedMetadata
        every { mockDocumentModelLoader.loadModel(DOCUMENT_MODEL_NAME_WITHOUT_METADATA) } returns documentModelWithoutMetadata
        every { mockModelService.load(FORM_MODEL_NAME) } returns buildGenericModel(formModelString)
        every { mockModelService.load(FORM_MODEL_NAME_FOR_NESTED_METADATA) } returns buildGenericModel(
            formModelForNestedMetadataString,
        )
        every { mockWorkflowsDocumentService.loadDocument(DocumentReference(DOC_REF_STRING)) } returns documentWithoutMetadata
        every { mockWorkflowsDocumentService.loadDocument(DocumentReference(DOC_REF_WITH_NESTED_METADATA_STRING)) } returns documentWithoutMetadata
        every { mockWorkflowsDocumentService.loadDocument(DocumentReference(DOC_REF_WITH_DEEPLY_NESTED_METADATA_STRING)) } returns documentWithoutMetadata
        every { mockWorkflowsDocumentService.loadDocument(DocumentReference(DOC_REF_WITHOUT_METADATA_STRING)) } returns documentWithoutMetadata
    }

    fun addWorkflowsInternalDocumentStatusToExpectedMetadataForAssertion(internalDocumentStatus: String): JsonNode {
        val metadataNode = spyObjectMapper.convertValue<ObjectNode>(expectedMetadata)
        metadataNode.put(WORKFLOWS_INTERNAL_DOCUMENT_STATUS, internalDocumentStatus)
        return metadataNode
    }
}
