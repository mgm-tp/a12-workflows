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
package com.mgmtp.a12.workflows.engine.internal.delegates.document

import com.mgmtp.a12.workflows.engine.internal.delegates.api.DelegateBeanNames.CREATE_DOCUMENT_DELEGATE
import com.mgmtp.a12.workflows.engine.internal.delegates.shared.DelegateParameters
import com.mgmtp.a12.workflows.engine.internal.delegates.shared.getDelegateStringInput
import com.mgmtp.a12.workflows.engine.internal.util.ProcessEngineVariableNames
import com.mgmtp.a12.workflows.shared.logging.utils.logDelegateExecutionComplete
import com.mgmtp.a12.workflows.shared.logging.utils.logDelegateExecutionStart
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.cibseven.bpm.engine.delegate.JavaDelegate
import org.springframework.stereotype.Component

@Component(CREATE_DOCUMENT_DELEGATE)
class CreateDocumentDelegate(private val service: CreateDocumentService) : JavaDelegate {

    private val delegateName = this::class.simpleName!!
    private val delegateInputs = CreateDocumentDelegateParameters().inputs
    private val documentModelNameVar = CreateDocumentDelegateParameters.InputVars.DOCUMENT_MODEL_NAME.label
    private val docRefNameVar = CreateDocumentDelegateParameters.OutputVars.NEW_DOC_REF.label

    override fun execute(execution: DelegateExecution) {
        logger().logDelegateExecutionStart()
        val documentModelName = getDelegateStringInput(
            execution,
            delegateName,
            documentModelNameVar,
            delegateInputs[documentModelNameVar],
        )

        val createdDocRef = service.createDocument(documentModelName)
        execution.setVariableLocal(docRefNameVar, createdDocRef.toString())
        logger().debug("Successfully created document [docRef={}]", createdDocRef)
        logger().logDelegateExecutionComplete()
    }
}

class CreateDocumentDelegateParameters :
    DelegateParameters(
        inputs = mapOf(InputVars.DOCUMENT_MODEL_NAME.label to true),
        outputs = listOf(OutputVars.NEW_DOC_REF.label),
    ) {
    enum class InputVars(val label: String) {
        DOCUMENT_MODEL_NAME("documentModelName"),
    }
    enum class OutputVars(val label: String) {
        NEW_DOC_REF(ProcessEngineVariableNames.NEW_DOC_REF_VARIABLE),
    }
}
