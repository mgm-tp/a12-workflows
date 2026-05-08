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
package com.mgmtp.a12.model.workflows.migration.validation

import com.mgmtp.a12.model.workflows.migration.exception.MigrationException
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationError
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationErrorType
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationInfo
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationLogger
import org.apache.commons.io.IOUtils
import org.jdom2.Document
import org.jdom2.JDOMException
import org.jdom2.input.SAXBuilder
import org.jdom2.input.sax.BuilderErrorHandler
import org.jdom2.input.sax.XMLReaderJDOMFactory
import org.jdom2.input.sax.XMLReaderXSDFactory
import org.jdom2.output.XMLOutputter
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * JDom implementation with SAXBuilder of SchemaValidator
 *
 */
class JDomSchemaValidator(private val schemaUrls: Array<URL>, private val quirks: Collection<String>) : SchemaValidator {

    constructor(schemaUrls: Array<URL>, quirks: InputStream?) : this(
        schemaUrls,
        IOUtils.readLines(quirks, StandardCharsets.UTF_8),
    )

    @Throws(MigrationException::class, IOException::class)
    override fun validate(modelDocument: Document?, migrationLogger: MigrationLogger?) {
        try {
            val xsdFactory: XMLReaderJDOMFactory = XMLReaderXSDFactory(*schemaUrls)
            val saxBuilder = SAXBuilder(xsdFactory)
            saxBuilder.setErrorHandler(createQuirkedErrorHandler(migrationLogger))
            val outputStream = ByteArrayOutputStream()
            outputStream.use { out -> XMLOutputter().output(modelDocument, out) }
            ByteArrayInputStream(outputStream.toByteArray()).use { inputStream -> saxBuilder.build(inputStream) }
        } catch (e: JDOMException) {
            throw MigrationException(
                MigrationError(
                    MigrationErrorType.MODEL_CONTENT_RESULT_INVALID_XML,
                    message = "result is not schema valid: ${e.message}",
                ),
            )
        }
    }

    private fun createQuirkedErrorHandler(migrationLogger: MigrationLogger?): QuirkedErrorHandler? = if (quirks.isNotEmpty()) QuirkedErrorHandler(quirks, migrationLogger) else null

    private class QuirkedErrorHandler(private val quirks: Collection<String>, private val logger: MigrationLogger?) : BuilderErrorHandler() {
        @Throws(SAXException::class)
        override fun warning(exception: SAXParseException) {
            logger?.addInfo(MigrationInfo(message = exception.message!!))
        }

        @Throws(SAXException::class)
        override fun error(exception: SAXParseException) {
            if (hasQuirk(exception.message)) {
                logger?.addInfo(MigrationInfo("quirked error: " + exception.message))
            } else {
                throw exception
            }
        }

        @Throws(SAXException::class)
        override fun fatalError(exception: SAXParseException) = throw exception

        private fun hasQuirk(message: String?): Boolean = quirks.any { message.toString().matches(it.toRegex()) }
    }
}
