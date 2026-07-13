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

package com.mgmtp.a12.workflows.extension.internal.sync

import com.mgmtp.a12.dataservices.model.document.persistence.DocumentModelLoader
import com.mgmtp.a12.workflows.engine.client.internal.ProcessEngineClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["mgmtp.a12.workflows.extension.automaticSynchronization.enabled"], havingValue = "true")
class SynchronizationConfiguration(
    private val documentModelLoader: DocumentModelLoader,
    private val processEngineClient: ProcessEngineClient,
    private val workflowsDocumentModelWalker: WorkflowsDocumentModelWalker,
) {

    @Bean
    fun synchronizationHandler(): SynchronizationHandler = SynchronizationHandler(documentModelLoader, processEngineClient, workflowsDocumentModelWalker)

    @Bean
    fun synchronizationListeners(synchronizationHandler: SynchronizationHandler): SynchronizationListeners = SynchronizationListeners(synchronizationHandler)
}
