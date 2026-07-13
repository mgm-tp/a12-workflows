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

package com.mgmtp.a12.workflows.engine.internal.delegates.setstatus

import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.workflows.engine.internal.delegates.api.DelegateBeanNames.SET_STATUS_DELEGATE
import com.mgmtp.a12.workflows.engine.internal.delegates.shared.DelegateParameters
import com.mgmtp.a12.workflows.engine.internal.delegates.shared.getDelegateStringInput
import com.mgmtp.a12.workflows.shared.logging.utils.logDelegateExecutionStart
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.cibseven.bpm.engine.delegate.JavaDelegate
import org.springframework.stereotype.Component

@Component(SET_STATUS_DELEGATE)
class SetStatusDelegate(private val service: SetStatusService) : JavaDelegate {

    private val delegateName = this::class.simpleName!!
    private val delegateInputs = SetStatusDelegateParameters().inputs
    private val docRefVar = SetStatusDelegateParameters.InputVars.DOCUMENT_REFERENCE.label
    private val newValueVar = SetStatusDelegateParameters.InputVars.NEW_VALUE.label
    private val pathToFieldVar = SetStatusDelegateParameters.InputVars.PATH_TO_FIELD.label
    private val repetitionsVar = SetStatusDelegateParameters.InputVars.REPETITIONS.label

    override fun execute(execution: DelegateExecution) {
        val documentReference = getDelegateStringInput(
            execution,
            delegateName,
            docRefVar,
            delegateInputs[docRefVar],
        )
        val pathToField = getDelegateStringInput(
            execution,
            delegateName,
            pathToFieldVar,
            delegateInputs[pathToFieldVar],
        )
        val repetitions = getDelegateStringInput(
            execution,
            delegateName,
            repetitionsVar,
            delegateInputs[repetitionsVar],
        )
        val newValue = getDelegateStringInput(
            execution,
            delegateName,
            newValueVar,
            delegateInputs[newValueVar],
        )

        val repetitionsArray: IntArray = when {
            repetitions.isEmpty() -> generateDefaultRepetitions(pathToField)
            else -> validateAndParseRepetitions(repetitions, pathToField)
        }
        logger().logDelegateExecutionStart(
            variables = mapOf(
                "docRef" to documentReference,
                "pathToField" to pathToField,
                "repetitions" to repetitions,
            ),
        )

        service.setStatus(DocumentReference(documentReference), pathToField, repetitionsArray, newValue)
    }

    /*
     * Default repetitions simply set index 1 for all path parts, e.g. for path /a/b/c/d would be [1,1,1,1].
     */
    private fun generateDefaultRepetitions(pathToField: String): IntArray {
        val numberOfPathParts = pathToField.count { it == '/' }
        return IntArray(numberOfPathParts) { 1 }
    }

    fun validateAndParseRepetitions(repetitions: String, pathToField: String): IntArray {
        val numberOfPathParts = pathToField.count { it == '/' }
        val numberOfRepetitionParts = repetitions.count { it == ',' } + 1

        if (numberOfRepetitionParts != numberOfPathParts) {
            throw IllegalArgumentException("Number of repetition values does not match number of path parts")
        }

        return repetitions.split(",")
            .map { it.trim().toInt() }
            .toIntArray()
    }
}

class SetStatusDelegateParameters :
    DelegateParameters(
        mapOf(
            InputVars.DOCUMENT_REFERENCE.label to true,
            InputVars.NEW_VALUE.label to true,
            InputVars.PATH_TO_FIELD.label to true,
            InputVars.REPETITIONS.label to false,
        ),
    ) {
    enum class InputVars(val label: String) {
        DOCUMENT_REFERENCE("documentReference"),
        NEW_VALUE("newValue"),
        PATH_TO_FIELD("pathToField"),
        REPETITIONS("repetitions"),
    }
}
