/**
 * (c) copyright 2012-2026 mgm technology partners GmbH.
 * This software, the underlying source code and other artifacts are protected by copyright.
 * All rights, in particular the right to use, reproduce, publish and edit are reserved.
 * A simple right of use (license) can be acquired for use, duplication, publication, editing etc..
 * Requests for this can be made at A12-license@mgm-tp.com or other official channels of the copyright holder.
 */
package com.mgmtp.a12.workflows.engine.internal.delegates.templates

import com.mgmtp.a12.workflows.engine.internal.delegates.api.DelegateBeanNames
import com.mgmtp.a12.workflows.engine.internal.delegates.document.CreateDocumentDelegateParameters
import com.mgmtp.a12.workflows.engine.internal.delegates.email.SendEmailDelegateParameters
import com.mgmtp.a12.workflows.engine.internal.delegates.relationship.DeleteRelationshipLinkDelegateParameters
import com.mgmtp.a12.workflows.engine.internal.delegates.relationship.RelationshipLinkDelegateParameters
import com.mgmtp.a12.workflows.engine.internal.delegates.relationship.RelinkDocumentDelegateParameters
import com.mgmtp.a12.workflows.engine.internal.delegates.setstatus.SetStatusDelegateParameters
import com.mgmtp.a12.workflows.engine.internal.delegates.shared.DelegateParameters
import com.mgmtp.a12.workflows.engine.internal.delegates.syncData.SyncAvailableFieldsDelegateParameters
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.json.JSONArray
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream
import kotlin.reflect.full.memberProperties
import kotlin.streams.asStream

/**
 * This test class checks Camunda element templates which implement delegates
 * Each delegate template has a subclass below
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class DelegateTemplateTest(
     val elementTemplateFileName: String,
     val parametersClass: DelegateParameters
) {

    private val inputParameterMap = mutableMapOf<String, Boolean>()
    private val outputParameterSources = mutableListOf<String>()
    private var implementationType = ""
    private var asyncBeforeProperty: Any? = null
    private var asyncAfterProperty: Any? = null

    companion object {
        // Camunda property names/types
        const val CAMUNDA_INPUT_TYPE = "camunda:inputParameter"
        const val CAMUNDA_OUTPUT_TYPE = "camunda:outputParameter"
        const val CAMUNDA_IMPLEMENTATION_TYPE = "camunda:delegateExpression"
        const val CAMUNDA_ASYNC_BEFORE = "camunda:asyncBefore"
        const val CAMUNDA_ASYNC_AFTER = "camunda:asyncAfter"

        const val ELEMENT_TEMPLATE_PATH_PREFIX = "src/main/resources/element-templates/"
        const val DELEGATE_EXPRESSION_PREFIX = "\${"
        const val DELEGATE_EXPRESSION_SUFFIX = "}"
    }

    // initialize map for input parameters in template
    @BeforeAll
    fun setup() {
        val elementTemplatePath = ELEMENT_TEMPLATE_PATH_PREFIX + elementTemplateFileName
        val elementTemplateJsonString = File(elementTemplatePath).readText()
        val elementTemplateJson = JSONArray(elementTemplateJsonString)
        val properties = elementTemplateJson.getJSONObject(0).getJSONArray("properties")

        // filter for properties input parameter, asyncBefore/After and delegate class
        for (i in 0 until properties.length()) {

            val currentProperty = properties.getJSONObject(i)
            val binding = currentProperty.getJSONObject("binding")
            val bindingType = binding.getString("type")
            val bindingName = when (bindingType) {
                CAMUNDA_OUTPUT_TYPE -> ""
                else -> binding.getString("name")
            }

            when (bindingType) {
                CAMUNDA_INPUT_TYPE -> {
                    val constraints = currentProperty.getJSONObject("constraints")
                    inputParameterMap[bindingName] = constraints.getBoolean("notEmpty")
                    continue
                }
                CAMUNDA_OUTPUT_TYPE -> {
                    outputParameterSources.add(binding.getString("source"))
                    continue
                }
                else -> when (bindingName) {
                    CAMUNDA_IMPLEMENTATION_TYPE -> implementationType = currentProperty.getString("value")
                    CAMUNDA_ASYNC_BEFORE -> asyncBeforeProperty = currentProperty
                    CAMUNDA_ASYNC_AFTER -> asyncAfterProperty = currentProperty
                }
            }
        }
    }

    @Test
    fun `should find async before and after properties`() {
        asyncBeforeProperty shouldNotBe null
        asyncAfterProperty shouldNotBe null
    }

    @Test
    fun `should find correct class in delegate bean name in expression`() {
        var delegateBeanFound = false

        // given
        val delegateExpression = implementationType.substringAfter(DELEGATE_EXPRESSION_PREFIX)
            .substringBefore(DELEGATE_EXPRESSION_SUFFIX)
        for (delegateBean in DelegateBeanNames::class.memberProperties) {
            if (delegateBean.getter.call() == delegateExpression) {
                delegateBeanFound = true
            }
        }

        // then
        delegateBeanFound shouldBe true
    }

    @ParameterizedTest
    @MethodSource("getDelegateInputs")
    fun `should find all enum inputs in template with correct constraint`(inputName: String, inputRequired: Boolean) {
        var inputParameterCorrect = false

        // given
        inputParameterMap.forEach {
            if (inputName == it.key && inputRequired == it.value) {
                inputParameterCorrect = true
            }
        }

        // then
        inputParameterCorrect shouldBe true
    }

    @ParameterizedTest
    @MethodSource("getDelegateOutputs")
    fun `should find all enum outputs in template with correct source`(outputVariableName: String) {
        // delegate might not have output
        if(outputVariableName == "") {
            return
        }

        // we expect output sources to have the format ${variable}
        outputParameterSources.forEach {
            it.substring(0, 2) shouldBe "\${"
            it.substring(it.length-1) shouldBe  "}"

            val outputVarInTemplate = it.substring(2, it.length-1)
            outputVarInTemplate shouldBe outputVariableName
        }
    }

     fun getDelegateInputs(): Stream<Arguments> {
        val inputMap = parametersClass.inputs
        val keys = inputMap.keys.toList()
        val values = inputMap.values.toList()

        return keys.indices.asSequence()
            .map { i -> Arguments.of(keys[i], values[i]) }
            .asStream()
    }

     fun getDelegateOutputs(): Stream<String> =
        if (parametersClass.outputs.isEmpty()) {
            listOf("").stream()
        } else {
            parametersClass.outputs.stream()
        }
}

private class CreateDocumentElementTemplateTest : DelegateTemplateTest(
    "create-document-delegate-template.json",
    CreateDocumentDelegateParameters()
)

private class SendEmailElementTemplateTest : DelegateTemplateTest(
    "send-email-delegate-template.json",
    SendEmailDelegateParameters()
)

private class DeleteRelationshipLinkElementTemplateTest : DelegateTemplateTest(
    "delete-relationship-delegate-element-template.json",
    DeleteRelationshipLinkDelegateParameters()
)

private class RelationshipLinkElementTemplateTest : DelegateTemplateTest(
    "relationship-link-delegate-template.json",
    RelationshipLinkDelegateParameters()
)

private class RelinkDocumentElementTemplateTest : DelegateTemplateTest(
    "relink-document-delegate-element-template.json",
    RelinkDocumentDelegateParameters()
)

private class SetStatusElementTemplateTest : DelegateTemplateTest(
    "set-status-delegate-template.json",
    SetStatusDelegateParameters()
)

private class SyncAvailableFieldsElementTemplateTest : DelegateTemplateTest(
    "sync-available-fields-delegate-template.json",
    SyncAvailableFieldsDelegateParameters()
)
