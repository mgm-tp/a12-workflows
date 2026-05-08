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
package com.mgmtp.a12.workflows.engine.internal.delegates.relationship

import com.mgmtp.a12.dataservices.relationship.spec.LinkDescriptor
import com.mgmtp.a12.workflows.engine.internal.delegates.api.DelegateBeanNames.RELINK_DOCUMENT_DELEGATE
import com.mgmtp.a12.workflows.engine.internal.delegates.shared.DelegateParameters
import com.mgmtp.a12.workflows.engine.internal.delegates.shared.createRelationshipRoleSpec
import com.mgmtp.a12.workflows.engine.internal.delegates.shared.getDelegateStringInput
import com.mgmtp.a12.workflows.engine.internal.delegates.shared.getOrFail
import com.mgmtp.a12.workflows.engine.internal.util.ProcessEngineVariableNames
import com.mgmtp.a12.workflows.shared.logging.utils.logDelegateExecutionComplete
import com.mgmtp.a12.workflows.shared.logging.utils.logDelegateExecutionStart
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.cibseven.bpm.engine.delegate.BpmnError
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.cibseven.bpm.engine.delegate.JavaDelegate
import org.springframework.stereotype.Component
import java.io.IOException

data class RelinkDocumentInfo(val linkRef: String, val linkDescriptor: LinkDescriptor)

@Component(RELINK_DOCUMENT_DELEGATE)
class RelinkDocumentDelegate(private val relationshipLinkService: RelationshipLinkService) : JavaDelegate {

    private val delegateName = this::class.simpleName!!
    private val delegateInputs = RelinkDocumentDelegateParameters().inputs
    private val relationshipModelVar =
        RelinkDocumentDelegateParameters.InputVars.RELATIONSHIP_MODEL.label
    private val sourceRoleVar = RelinkDocumentDelegateParameters.InputVars.SOURCE_ROLE.label
    private val sourceDocRefVar = RelinkDocumentDelegateParameters.InputVars.SOURCE_DOC_REF.label
    private val targetRoleVar = RelinkDocumentDelegateParameters.InputVars.TARGET_ROLE.label
    private val targetDocRefVar = RelinkDocumentDelegateParameters.InputVars.TARGET_DOC_REF.label
    private val linkRefVar = RelinkDocumentDelegateParameters.InputVars.LINK_REF.label
    private val relationshipLinkIdOutputVariable = RelinkDocumentDelegateParameters.OutputVars.NEW_LINK_ID.label

    override fun execute(execution: DelegateExecution) {
        val relinkDocumentInfo = extractRelinkDocumentOperationVars(execution)
        logger().logDelegateExecutionStart(
            variables = mapOf(
                "relinkDocumentInformation" to relinkDocumentInfo.toString(),
            ),
        )

        try {
            val relationshipLinkId = relationshipLinkService.relinkDocument(relinkDocumentInfo)
            execution.setVariableLocal(relationshipLinkIdOutputVariable, relationshipLinkId)
            logger().logDelegateExecutionComplete(
                variables = mapOf(
                    "newRelationshipLinkId" to relationshipLinkId,
                ),
            )
        } catch (e: IOException) {
            execution.setVariableLocal(relationshipLinkIdOutputVariable, null)
            throw BpmnError(LINKAGE_ERROR_ID)
        }
    }

    private fun extractRelinkDocumentOperationVars(execution: DelegateExecution) = RelinkDocumentInfo(
        execution.getOrFail(linkRefVar, delegateName),
        LinkDescriptor(
            execution.getOrFail(relationshipModelVar, delegateName),
            listOf(
                createRelationshipRoleSpec(
                    getDelegateStringInput(
                        execution,
                        delegateName,
                        sourceDocRefVar,
                        delegateInputs[targetDocRefVar],
                    ),
                    getDelegateStringInput(
                        execution,
                        delegateName,
                        sourceRoleVar,
                        delegateInputs[targetDocRefVar],
                    ),
                ),
                createRelationshipRoleSpec(
                    getDelegateStringInput(
                        execution,
                        delegateName,
                        targetDocRefVar,
                        delegateInputs[targetDocRefVar],
                    ),
                    getDelegateStringInput(
                        execution,
                        delegateName,
                        targetRoleVar,
                        delegateInputs[targetRoleVar],
                    ),
                ),
            ),
        ).also { logger().debug(it.toString()) },
    )
}

class RelinkDocumentDelegateParameters :
    DelegateParameters(
        inputs = mapOf(
            InputVars.LINK_REF.label to true,
            InputVars.RELATIONSHIP_MODEL.label to true,
            InputVars.SOURCE_ROLE.label to true,
            InputVars.SOURCE_DOC_REF.label to true,
            InputVars.TARGET_ROLE.label to true,
            InputVars.TARGET_DOC_REF.label to true,
        ),
        outputs = listOf(OutputVars.NEW_LINK_ID.label),
    ) {
    enum class InputVars(val label: String) {
        LINK_REF("id"),
        RELATIONSHIP_MODEL("relationshipModel"),
        SOURCE_ROLE("sourceRole"),
        SOURCE_DOC_REF("sourceDocRef"),
        TARGET_ROLE("targetRole"),
        TARGET_DOC_REF("targetDocRef"),
    }
    enum class OutputVars(val label: String) {
        NEW_LINK_ID(ProcessEngineVariableNames.NEW_LINK_ID_VARIABLE),
    }
}
