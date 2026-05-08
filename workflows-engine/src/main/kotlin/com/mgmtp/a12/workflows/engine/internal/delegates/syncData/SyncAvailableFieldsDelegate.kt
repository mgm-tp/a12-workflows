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
package com.mgmtp.a12.workflows.engine.internal.delegates.syncData

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.workflows.engine.internal.delegates.api.DelegateBeanNames.SYNC_AVAILABLE_FIELDS_DELEGATE
import com.mgmtp.a12.workflows.engine.internal.delegates.shared.DelegateParameters
import com.mgmtp.a12.workflows.shared.dtos.FieldUpdatesDto
import com.mgmtp.a12.workflows.shared.logging.utils.logDelegateExecutionComplete
import com.mgmtp.a12.workflows.shared.logging.utils.logDelegateExecutionStart
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.cibseven.bpm.engine.delegate.JavaDelegate
import org.springframework.stereotype.Component

@Component(SYNC_AVAILABLE_FIELDS_DELEGATE)
class SyncAvailableFieldsDelegate(private val service: SyncAvailableFieldsService) : JavaDelegate {

    override fun execute(execution: DelegateExecution) {
        logger().logDelegateExecutionStart()
        val documentReferences = execution.getListOrFail(
            SyncAvailableFieldsDelegateParameters.InputVars.DOCUMENT_REFERENCES.label,
        )
        val result = service.syncAvailableFields(documentReferences)

        val fieldUpdatesDto = jacksonObjectMapper().convertValue(result, FieldUpdatesDto::class.java)
        updateFieldsInProcess(execution, fieldUpdatesDto)
        logger().logDelegateExecutionComplete()
    }

    private fun updateFieldsInProcess(execution: DelegateExecution, fieldUpdatesDto: FieldUpdatesDto) {
        for (field in fieldUpdatesDto.modificationMap) {
            execution.processInstance.setVariable(field.key, field.value)
        }
    }

    fun DelegateExecution.getListOrFail(variableName: String): List<DocumentReference> {
        val documentReferencesVariable = this.getVariable(variableName)
        val documentReferences = mutableListOf<DocumentReference>()

        when (documentReferencesVariable) {
            is String -> {
                for (docRef in documentReferencesVariable.toString().split(",").map { it.trim() }) {
                    documentReferences.add(DocumentReference(docRef))
                }
            }
            is List<*> -> {
                for (docRef in documentReferencesVariable as List<String>) {
                    documentReferences.add(DocumentReference(docRef))
                }
            }
            else -> {
                logger().error(
                    "$SYNC_AVAILABLE_FIELDS_DELEGATE was called with wrong assigment type for input parameter [documentReferences]. The assignment type has to be a List or comma separated String.",
                )
                throw ClassCastException()
            }
        }

        return documentReferences
    }
}

class SyncAvailableFieldsDelegateParameters :
    DelegateParameters(
        mapOf(InputVars.DOCUMENT_REFERENCES.label to true),
    ) {
    enum class InputVars(val label: String) {
        DOCUMENT_REFERENCES("documentReferences"),
    }
}
