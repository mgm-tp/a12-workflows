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

package com.mgmtp.a12.model.workflows.migration.config

import com.mgmtp.a12.model.workflows.migration.config.types.MigrationConfig
import com.mgmtp.a12.model.workflows.migration.config.types.ObjectFactory
import com.mgmtp.a12.model.workflows.migration.exception.JaxbAccessException
import com.mgmtp.a12.model.workflows.migration.utils.jaxb.JaxbSerializer

internal class MigratorConfigJaxbSerializer : JaxbSerializer() {

    override fun getJaxbContextPath(): String = ObjectFactory::class.java.getPackage().name

    /**
     * Converts XML string to configuration.
     *
     * @param migrationConfigXML Configuration xml document as String
     * @return configuration instance created from String
     * @throws JaxbAccessException If an error exists during JAXB transformation
     */
    @Throws(JaxbAccessException::class)
    fun fromXml(migrationConfigXML: String): MigrationConfig = deserialize(migrationConfigXML) as MigrationConfig

    /**
     * Converts Configuration to xml String. Convenience method.
     *
     * @param migrationConfig MigrationConfig to convert. Must not be `null`.
     * @return String representation of Configuration xml document
     * @throws JaxbAccessException If an error exists during JAXB transformation
     */
    @Throws(JaxbAccessException::class)
    fun toXml(migrationConfig: MigrationConfig): String = serialize(migrationConfig)
}
