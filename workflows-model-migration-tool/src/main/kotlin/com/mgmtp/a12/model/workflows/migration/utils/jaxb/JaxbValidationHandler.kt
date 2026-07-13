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

package com.mgmtp.a12.model.workflows.migration.utils.jaxb

import jakarta.xml.bind.ValidationEvent
import jakarta.xml.bind.ValidationEventLocator
import jakarta.xml.bind.util.ValidationEventCollector
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Validation handler for marshalling/unmarshalling of JAXB object. Collect all occurring events and log these to error logger.
 *
 *
 * Based on [ValidationEventCollector]. Call the getEvents method to retrieve all the reported errors
 * and warnings.
 *
 * @date $LastChangedDate: 2012-06-08 13:09:55 +0200 (Fr, 08. Jun 2012) $
 * @revision $LastChangedRevision: 423 $
 * @see ValidationEventCollector
 *
 * @see ValidationEvent
 *
 * @see ValidationEventLocator
 *
 * @since 1.0
 */
class JaxbValidationHandler : ValidationEventCollector() {

    private val logger: Logger = LoggerFactory.getLogger(JaxbValidationHandler::class.java)

    /**
     * Identifies xml document structure is valid or not.
     *
     * @return `true` if no errors or warning occurs during marshalling/unmarshalling.
     * `false/unmarshalling.` */
    val isValid: Boolean
        get() = !hasEvents()

    override fun handleEvent(event: ValidationEvent): Boolean {
        logger.error("JAXB parse error: " + event.message)
        return super.handleEvent(event)
    }

    override fun toString(): String {
        var result = ""
        for (i in events.indices) {
            val vi = events[i]
            if (i > 0) {
                result += "; "
            }
            result += """
     
     Event ${i + 1}> location=${formatLocation(vi.locator)}, msg=
            """.trimIndent() + vi
                .message
        }
        return result
    }

    private fun formatLocation(locator: ValidationEventLocator): String {
        var result = "{line:" + locator.lineNumber
        result += ", column:" + locator.columnNumber
        result += ", offset:" + locator.offset + "}"
        return result
    }
}
