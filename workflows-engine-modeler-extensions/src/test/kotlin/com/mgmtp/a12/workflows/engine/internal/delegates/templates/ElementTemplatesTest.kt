/**
 * (c) copyright 2012-2026 mgm technology partners GmbH.
 * This software, the underlying source code and other artifacts are protected by copyright.
 * All rights, in particular the right to use, reproduce, publish and edit are reserved.
 * A simple right of use (license) can be acquired for use, duplication, publication, editing etc..
 * Requests for this can be made at A12-license@mgm-tp.com or other official channels of the copyright holder.
 */
package com.mgmtp.a12.workflows.engine.internal.delegates.templates

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
private class ElementTemplatesTest {

    companion object {
        const val ELEMENT_TEMPLATE_DIRECTORY_PATH = "src/main/resources/element-templates"
        const val ELEMENT_TEMPLATE_ID = "id"
    }

    @ParameterizedTest
    @MethodSource("getElementTemplates")
    fun `should verify all element template versions share the same template id`(parsedElementTemplate: JSONArray) {
       var elementTemplateId = ""
       parsedElementTemplate.forEach { elementTemplateVersion ->
           val currentElementTemplateVersionId = (elementTemplateVersion as JSONObject).getString(ELEMENT_TEMPLATE_ID)
           if(elementTemplateId == "") {
               elementTemplateId = currentElementTemplateVersionId
           }
           else {
               elementTemplateId shouldBe currentElementTemplateVersionId
           }
       }
        elementTemplateId shouldNotBe ""
    }

     fun getElementTemplates(): Stream<JSONArray> {
        var parsedElementTemplates: Stream<JSONArray> = Stream.empty()
        val elementTemplateDirectory = File(ELEMENT_TEMPLATE_DIRECTORY_PATH)
        elementTemplateDirectory.listFiles()!!.forEach { file ->
            val elementTemplateContent = file.bufferedReader().use { it.readText() }
            val elementTemplateJsonArray = JSONArray(elementTemplateContent)
            parsedElementTemplates = Stream.concat(parsedElementTemplates, Stream.of(elementTemplateJsonArray))
        }
        return parsedElementTemplates
    }
}