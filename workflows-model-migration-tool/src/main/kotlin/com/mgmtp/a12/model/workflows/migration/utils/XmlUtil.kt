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

package com.mgmtp.a12.model.workflows.migration.utils

import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.JDOMException
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * XML utility.
 */
object XmlUtil {

    /**
     * Creates an empty jdom2 element.
     *
     * @param name      the name of the element to create
     * @param namespace the namespace of the element to create
     * @return the new instance of created element
     */
    private fun newElement(name: String?, namespace: Namespace?): Element = Element(name, namespace)

    /**
     * Creates a new jdom2 element with the given text content inside.
     *
     * @param name      the name of the element to create
     * @param namespace the namespace of the element to create
     * @param textValue the text value to set.
     * @return the new instance of created element
     */
    private fun newElement(name: String?, namespace: Namespace?, textValue: String?): Element {
        val elem = newElement(name, namespace)
        elem.text = textValue
        return elem
    }

    /**
     * Copies optionally text value of element with `elementName` from `sourceParent` to new element with
     * the same `elementName` in `targetParent`.
     *
     * @param elementName     the element name
     * @param sourceParent    the source parent
     * @param sourceNamespace the source namespace
     * @param targetParent    the target parent
     * @param targetNamespace the target namespace
     * @param optional        the optional flag
     */
    @JvmOverloads
    fun copyTextElement(
        elementName: String?,
        sourceParent: Element,
        sourceNamespace: Namespace?,
        targetParent: Element,
        targetNamespace: Namespace?,
        optional: Boolean = false,
    ) {
        val sourceTextValue = sourceParent.getChildText(elementName, sourceNamespace)
        if (!optional || sourceTextValue != null) {
            targetParent.addContent(newElement(elementName, targetNamespace, sourceTextValue))
        }
    }

    @JvmOverloads
    fun copyAttribute(
        attributeName: String?,
        sourceParent: Element,
        targetParent: Element,
        optional: Boolean = false,
    ) {
        val sourceTextValue = sourceParent.getAttributeValue(attributeName)
        if (!optional || sourceTextValue != null) {
            targetParent.setAttribute(attributeName, sourceTextValue)
        }
    }

    /**
     * Serialize model.
     *
     * @param out the target
     * @param document the doc
     */
    @Throws(IOException::class)
    fun serializeModel(out: OutputStream?, document: Document?) {
        DocumentSerializer().serializeDocumentToStream(out, document!!)
    }

    /**
     * Parse model.
     *
     * @param in the source
     * @return the document
     */
    @JvmStatic
    @Throws(JDOMException::class, IOException::class)
    fun parseModel(`in`: InputStream): Document {
        val xmlIn = SAXBuilder()
        xmlIn.ignoringBoundaryWhitespace = true
        return xmlIn.build(`in`)
    }

    /**
     * recursively update namespaces according to a given mapping
     *
     * @param element the root element
     * @param mapping a from -> to namespace map
     */
    private fun updateNamespacesRecursive(element: Element, mapping: Map<Namespace, Namespace>) {
        val newNamespace = mapping[element.namespace]
        if (newNamespace != null) {
            element.setNamespace(newNamespace)
        }

        element.additionalNamespaces.forEach {
            val newAdditionalNamespace = mapping[it]
            if (newAdditionalNamespace != null) {
                element.removeNamespaceDeclaration(it)
                element.addNamespaceDeclaration(newAdditionalNamespace)
            }
        }

        element.children.forEach { updateNamespacesRecursive(it, mapping) }
    }

    fun updateNamespacesRecursive(
        elements: List<Element>,
        namespaceMapping: Map<Namespace, Namespace>,
    ) {
        elements.forEach { updateNamespacesRecursive(it, namespaceMapping) }
    }
}
