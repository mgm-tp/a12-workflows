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

package com.mgmtp.a12.workflows.extension.internal.config

import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.CREATE_METADATA_OPERATION
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.REMOVE_METADATA_OPERATION
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.SET_STATUS_OPERATION
import jakarta.servlet.ServletContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AllowedRpcOperationsConfig {

    @Value("\${mgmtp.a12.dataservices.json-rpc.allowed-operations}")
    lateinit var allowedOperations: String

    @Bean
    fun configureAllowedRpcOperations(): ServletContextInitializer = ServletContextInitializer { servletContext: ServletContext ->
        if (allowedOperations == "*") {
            return@ServletContextInitializer
        }
        if (!allowedOperations.contains(CREATE_METADATA_OPERATION)) {
            allowedOperations = "$allowedOperations,$CREATE_METADATA_OPERATION"
        }
        if (!allowedOperations.contains(REMOVE_METADATA_OPERATION)) {
            allowedOperations = "$allowedOperations,$REMOVE_METADATA_OPERATION"
        }
        if (!allowedOperations.contains(SET_STATUS_OPERATION)) {
            allowedOperations = "$allowedOperations,$SET_STATUS_OPERATION"
        }
        servletContext.setInitParameter("mgmtp.a12.dataservices.jsonRpc.allowedOperations", allowedOperations)
    }
}
