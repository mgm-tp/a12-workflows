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

import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.CREATE_DOCUMENT_OPERATION
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.CREATE_METADATA_OPERATION
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.REMOVE_METADATA_OPERATION
import com.mgmtp.a12.workflows.client.internal.operation.InternalOperationConstants.SET_STATUS_OPERATION
import jakarta.servlet.ServletContext
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class AllowedRpcOperationsConfig(private val environment: Environment) {

    @Bean
    fun configureAllowedRpcOperations(): ServletContextInitializer = ServletContextInitializer { servletContext: ServletContext ->
        // Bound like Data Services binds it: any key form, comma separated or indexed; its default if unset.
        val allowedOperations = Binder.get(environment)
            .bind("mgmtp.a12.dataservices.json-rpc.allowed-operations", Bindable.listOf(String::class.java))
            .orElse(null) ?: listOf("A12_INTERNAL_OPERATIONS")
        val operations = withAutomaticallyAllowedOperations(allowedOperations) ?: return@ServletContextInitializer
        servletContext.setInitParameter("mgmtp.a12.dataservices.jsonRpc.allowedOperations", operations)
    }

    companion object {

        // The mandatory operations CIB 7 calls, plus SET_STATUS, which earlier versions already added.
        internal val AUTOMATICALLY_ALLOWED_OPERATIONS = listOf(
            CREATE_DOCUMENT_OPERATION,
            CREATE_METADATA_OPERATION,
            REMOVE_METADATA_OPERATION,
            SET_STATUS_OPERATION,
        )

        internal fun withAutomaticallyAllowedOperations(allowedOperations: List<String>): String? {
            val configured = allowedOperations.map { it.trim() }.filter { it.isNotEmpty() }
            if (configured == listOf("*")) {
                return null
            }
            return (configured + AUTOMATICALLY_ALLOWED_OPERATIONS).distinct().joinToString(",")
        }
    }
}
