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
package com.mgmtp.a12.model.workflows.migration.accessor

import com.mgmtp.a12.model.workflows.migration.bpmn.validation.BPMNModelValidator
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.Parent
import org.jdom2.filter.ElementFilter
import org.jdom2.xpath.XPathExpression
import org.jdom2.xpath.XPathFactory

class ElementQuery private constructor(private val queryRoot: Parent, private vararg val namespaces: Namespace) {

    companion object {
        @JvmStatic
        fun createForBPMN(queryRoot: Parent): ElementQuery = ElementQuery(
            queryRoot,
            Namespace.getNamespace("bpmn", BPMNModelValidator.NS_BPMN.uri),
            Namespace.getNamespace("camunda", BPMNModelValidator.NS_CAMUNDA.uri),
        )
    }

    /**
     * Select (multiple) elements, which are matched by the xpath.
     *
     * @param query the query
     * @return the list
     */
    fun byXpath(query: String): List<Element> = elementExpression(query).evaluate(queryRoot)

    private fun elementExpression(query: String): XPathExpression<Element> = XPathFactory.instance().compile(query, ElementFilter(), null, *namespaces)
}
