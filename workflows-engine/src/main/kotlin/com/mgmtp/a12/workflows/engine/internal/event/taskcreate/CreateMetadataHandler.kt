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
package com.mgmtp.a12.workflows.engine.internal.event.taskcreate

import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.workflows.engine.internal.util.MetadataRetriever
import com.mgmtp.a12.workflows.engine.internal.util.ProcessEngineVariableNames.DOCUMENT_REFERENCE_VARIABLE
import com.mgmtp.a12.workflows.engine.internal.util.ProcessEngineVariableNames.NEW_DOC_REF_VARIABLE
import com.mgmtp.a12.workflows.engine.internal.util.getVariableAsStringOrEmpty
import com.mgmtp.a12.workflows.engine.internal.util.setOutputVariable
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.DataServicesRpcFacade
import com.mgmtp.a12.workflows.shared.dtos.ProcessMetadata
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.cibseven.bpm.engine.delegate.DelegateTask

class CreateMetadataHandler(
    private val rpcFacade: DataServicesRpcFacade,
    private val metadataRetriever: MetadataRetriever,
) {

    fun createDocumentMetadataByTask(delegateTask: DelegateTask) {
        logger().info("Triggered for task [${delegateTask.name}]")

        val metadata: ProcessMetadata = metadataRetriever.retrieveMetadata(delegateTask)

        val docRefString = delegateTask.getVariableAsStringOrEmpty(DOCUMENT_REFERENCE_VARIABLE)

        if (docRefString.isEmpty()) {
            val response = rpcFacade.tryCreateMetadata(null, metadata)
            val newDocRef = response.result.findValue("docRef").asText()
            delegateTask.setOutputVariable(NEW_DOC_REF_VARIABLE, newDocRef)
        } else {
            val docRef = DocumentReference(docRefString)
            rpcFacade.tryCreateMetadata(docRef, metadata)
        }
    }
}
