/**
 * LICENSE
 * SPDX-License-Identifier: EUPL-1.2 OR LicenseRef-commercial
 *
 * mgm A12 Platform – https://www.mgm-tp.com
 * Copyright (c) 2012–2026 mgm technology partners GmbH
 *
 * This software is developed by mgm technology partners GmbH and
 * is available under a choice of two different licenses:
 *
 * 1. Open-Source License – EUPL v1.2
 *    You may redistribute and/or modify this software under the
 *    terms of the European Union Public Licence, version 1.2.
 *    The license text is available at https://eupl.eu/
 *    and included in this distribution as:
 *        licenses/EUPL-1.2.txt
 *
 * 2. Commercial License
 *    Alternatively, you may obtain a commercial license from
 *    mgm technology partners GmbH, that permits use of this
 *    software under different terms (including support and
 *    maintenance options). For commercial licensing, please contact:
 *        a12-license@mgm-tp.com
 *
 * You must select and comply with exactly one of the above
 * licensing options.
 *
 * Third-Party Components
 * ----------------------
 * This software includes third-party components.
 * Their licenses are provided in:
 *    THIRD_PARTY_NOTICES
 *    licenses/   (folder with full license texts)
 *
 * Trademarks
 * ----------
 * “mgm A12” is a registered trademark of mgm technology partners GmbH.
 * The A12 logo is protected by copyright.
 *
 * Warranty Disclaimer (applies to either option)
 * ----------------------------------------------
 * THIS SOFTWARE IS PROVIDED “AS IS” AND WITHOUT WARRANTY OF ANY KIND,
 * WHETHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT,
 * EXCEPT WHERE SUCH DISCLAIMERS ARE NOT PERMITTED BY APPLICABLE LAW.
 * SEE THE RESPECTIVE LICENSE TEXT FOR DETAILS.
 *
 * End of LICENSE.
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