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
import com.mgmtp.a12.model.workflows.migration.exception.MigrationException
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationError
import com.mgmtp.a12.model.workflows.migration.miscellaneous.MigrationErrorType
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.Validate
import java.io.InputStream
import java.nio.charset.StandardCharsets

class MigratorConfigLoader {

    private val jaxbSerializer = MigratorConfigJaxbSerializer()

    /**
     * Load migration config.
     *
     * @param config the config. Must not be `NULL`!
     * @return the migration config
     */
    fun load(config: InputStream): MigrationConfig {
        val configXML = runCatching {
            IOUtils.toString(config, StandardCharsets.UTF_8)
        }.getOrElse {
            throw MigrationException(
                MigrationError(MigrationErrorType.CONFIG_FILE_NOT_READABLE, message = it.message!!),
            )
        }

        return try {
            Validate.validState(configXML != null)
            jaxbSerializer.fromXml(configXML)
        } catch (e: Exception) {
            throw MigrationException(
                MigrationError(MigrationErrorType.CONFIG_FILE_INVALID_XML, message = "Cannot parse XML contents: ${e.message}"),
            )
        }
    }
}
