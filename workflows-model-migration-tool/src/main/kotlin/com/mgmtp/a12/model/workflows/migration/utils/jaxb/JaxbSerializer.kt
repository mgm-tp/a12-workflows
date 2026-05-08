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
package com.mgmtp.a12.model.workflows.migration.utils.jaxb

import com.mgmtp.a12.model.workflows.migration.exception.JaxbAccessException
import com.mgmtp.a12.model.workflows.migration.exception.WorkflowsModelSerizalizationException
import com.mgmtp.a12.model.workflows.migration.utils.DefaultXmlTransformerCreator
import jakarta.xml.bind.*
import jakarta.xml.bind.util.JAXBSource
import org.apache.commons.lang3.Validate
import org.slf4j.LoggerFactory
import java.io.StringReader
import java.io.StringWriter
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerException
import javax.xml.transform.stream.StreamResult

/**
 * General implementation of functionality for access to a JAXB structure and converting between JAXB/String and
 * String/JAXB.
 *
 * @date $LastChangedDate: 2012-06-08 13:09:55 +0200 (Fr, 08. Jun 2012) $
 * @revision $LastChangedRevision: 423 $
 */
abstract class JaxbSerializer {

    private val log = LoggerFactory.getLogger(JaxbSerializer::class.java)

    companion object {
        /**
         * Cache of JAXBContext instances, since creation is a costly operation.
         */
        private val JAXB_CONTEXT_CACHE: HashMap<String, JAXBContext> = hashMapOf()
    }

    /**
     * Returns related [JAXBContext] instance.
     *
     * @return related [JAXBContext] instance.
     * @see JAXBContext
     */
    private val jaxbContext: JAXBContext = createJaxbContextForPackage(getJaxbContextPath())

    /**
     * Parse from the string `xmlContent` and convert it to JAXB element. Note that the method performs no
     * XML validation.
     *
     * @param xmlContent Content of XML document as String to parse. Must not be `null`.
     * @return JAXB object of generic type `JaxbObjectType`
     * @throws IllegalArgumentException if xmlContent is empty or `null`
     * @throws JaxbAccessException If any errors occur during converting
     */
    @Throws(JaxbAccessException::class)
    protected fun deserialize(xmlContent: String): Any {
        Validate.notEmpty(xmlContent, "Precondition violated. Argument: xmlContent must not be empty!")
        val unmarshaller: Unmarshaller
        val validationHandler: JaxbValidationHandler
        try {
            if (log.isDebugEnabled) {
                log.debug("Start unmarshalling of XML document from String")
            }
            unmarshaller = jaxbContext.createUnmarshaller()
            val useObjectFactories = useObjectFactories()
            if (useObjectFactories != null) {
                try {
                    unmarshaller.setProperty("com.sun.xml.internal.bind.ObjectFactory", useObjectFactories)
                } catch (pe: PropertyException) {
                    unmarshaller.setProperty("com.sun.xml.bind.ObjectFactory", useObjectFactories)
                }
            }
            validationHandler = JaxbValidationHandler()
            unmarshaller.eventHandler = validationHandler
        } catch (jbe: JAXBException) {
            throw JaxbAccessException("Error preparing unmarshaller", jbe)
        }
        val result: Any
        try {
            val elementObj = unmarshaller.unmarshal(StringReader(xmlContent))
            if (!isValidJaxbElementObject(elementObj, validationHandler)) {
                throw JaxbAccessException(
                    "Error during deserialize XML document as String to JAXB object.\nError: " +
                        validationHandler + "\nXmlContent: " + xmlContent,
                )
            }
            result = elementObj
            if (log.isTraceEnabled) {
                log.trace("Unmarshal XML content to: " + result.javaClass.getSimpleName())
            }
        } catch (cce: ClassCastException) {
            throw JaxbAccessException("Receive wrong type of JAXB object. xmlContent: $xmlContent", cce)
        } catch (jbe: JAXBException) {
            throw JaxbAccessException(
                (
                    "Error during deserialize XML document as String to JAXB object. xmlContent: " +
                        xmlContent
                    ),
                jbe,
            )
        }
        return result
    }

    /**
     * Convert provided `jaxbElement` to String.
     *
     * @param jaxbElement JAXB Element to convert
     * @param transformer the [Transformer] to use for serialization
     * @return XML content as String.
     * @throws JaxbAccessException If any errors occur during converting
     */
    @Throws(JaxbAccessException::class)
    protected fun serialize(jaxbElement: Any, transformer: Transformer): String {
        val result: String
        val marshaller: Marshaller
        val validationHandler: JaxbValidationHandler
        try {
            if (log.isDebugEnabled) {
                log.debug("Start marshalling of Jaxb-Type: <${jaxbElement.javaClass.getSimpleName()}> instance to xml string")
            }
            marshaller = jaxbContext.createMarshaller()
            validationHandler = JaxbValidationHandler()
            marshaller.eventHandler = validationHandler
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false)
            val sw = StringWriter()
            val source = JAXBSource(marshaller, jaxbElement)
            transformer.transform(source, StreamResult(sw))
            result = sw.toString()
        } catch (jbe: JAXBException) {
            throw JaxbAccessException("Error during serialize JAXB object to XML content as String", jbe)
        } catch (te: TransformerException) {
            throw WorkflowsModelSerizalizationException("Error using transformer.", te)
        }
        return result
    }

    /**
     * Convert provided `jaxbElement` to String.
     *
     * @param jaxbElement JAXB Element to convert
     * @return XML content as String.
     * @throws JaxbAccessException If any errors occur during converting
     */
    @Throws(JaxbAccessException::class)
    protected fun serialize(jaxbElement: Any): String {
        try {
            return serialize(jaxbElement, DefaultXmlTransformerCreator.create())
        } catch (e: TransformerException) {
            throw WorkflowsModelSerizalizationException("Error using transformer.", e)
        }
    }

    protected abstract fun getJaxbContextPath(): String

    private fun useObjectFactories(): Array<Any>? = null

    /**
     * Checks provided `elementObj` after deserialize XML document.
     *
     * @param elementObj Object to check
     * @param validationHandler ValidationHandler instance.
     * @return `true` if elementObj is valid for further use or `false` if not.
     */
    private fun isValidJaxbElementObject(elementObj: Any?, validationHandler: JaxbValidationHandler): Boolean = validationHandler.isValid &&
        elementObj != null

    /**
     *
     *
     * Initialize JAXBContext for provided package. Creates one if not exists.
     *
     *
     *
     *
     *
     * NOTE: JAXBContext creation is expensive operation that must be cached!
     *
     *
     * @param packageName Package name of generated JAXB Objects that associated with XML document to be parsed
     * @return Registered JaxbContext instance for package name.
     */
    @Synchronized
    private fun createJaxbContextForPackage(packageName: String): JAXBContext {
        try {
            var jc: JAXBContext? = JAXB_CONTEXT_CACHE[packageName]
            if (jc == null) {
                if (log.isInfoEnabled) {
                    log.info("Init JAXBContext for package: $packageName")
                }
                jc = JAXBContext.newInstance(packageName)
                JAXB_CONTEXT_CACHE[packageName] = jc
            }
            return jc!!
        } catch (e: JAXBException) {
            throw JaxbAccessException("JAXBContext initialisation error for package: $packageName", e)
        }
    }
}
