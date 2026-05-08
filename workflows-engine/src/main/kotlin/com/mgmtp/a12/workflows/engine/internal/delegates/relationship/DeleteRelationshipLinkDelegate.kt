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
import com.mgmtp.a12.dataservices.relationship.spec.RelationshipLinkSpec
import com.mgmtp.a12.workflows.engine.internal.delegates.api.DelegateBeanNames
import com.mgmtp.a12.workflows.engine.internal.delegates.shared.DelegateParameters
import com.mgmtp.a12.workflows.engine.internal.delegates.shared.createRelationshipRoleSpec
import com.mgmtp.a12.workflows.engine.internal.delegates.shared.getDelegateStringInput
import com.mgmtp.a12.workflows.engine.internal.delegates.shared.getOrFail
import com.mgmtp.a12.workflows.shared.logging.utils.logDelegateExecutionComplete
import com.mgmtp.a12.workflows.shared.logging.utils.logDelegateExecutionStart
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.cibseven.bpm.engine.delegate.BpmnError
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.cibseven.bpm.engine.delegate.JavaDelegate
import org.springframework.stereotype.Component
import java.io.IOException

@Component(DelegateBeanNames.DELETE_LINK_DELEGATE)
class DeleteRelationshipLinkDelegate(private val relationshipLinkService: RelationshipLinkService) : JavaDelegate {

    private val delegateName = this::class.simpleName!!
    private val delegateInputs = DeleteRelationshipLinkDelegateParameters().inputs
    private val relationshipModelVar =
        DeleteRelationshipLinkDelegateParameters.InputVars.RELATIONSHIP_MODEL.label
    private val sourceRoleVar =
        DeleteRelationshipLinkDelegateParameters.InputVars.SOURCE_ROLE.label
    private val sourceDocRefVar =
        DeleteRelationshipLinkDelegateParameters.InputVars.SOURCE_DOC_REF.label
    private val targetRoleVar =
        DeleteRelationshipLinkDelegateParameters.InputVars.TARGET_ROLE.label
    private val targetDocRefVar =
        DeleteRelationshipLinkDelegateParameters.InputVars.TARGET_DOC_REF.label
    private val id = DeleteRelationshipLinkDelegateParameters.InputVars.ID.label

    override fun execute(execution: DelegateExecution) {
        val relationshipLinkSpec = extractRelationshipLinkSpec(execution)
        logger().logDelegateExecutionStart(
            variables = mapOf(
                "relationshipLinkSpec" to relationshipLinkSpec.toString(),
            ),
        )
        try {
            relationshipLinkService.deleteLink(relationshipLinkSpec)
        } catch (e: IOException) {
            throw BpmnError(LINKAGE_ERROR_ID)
        }
        logger().logDelegateExecutionComplete()
    }

    private fun extractRelationshipLinkSpec(execution: DelegateExecution) = RelationshipLinkSpec(
        LinkDescriptor(
            execution.getOrFail(relationshipModelVar, delegateName),
            listOf(
                createRelationshipRoleSpec(
                    getDelegateStringInput(
                        execution,
                        delegateName,
                        sourceDocRefVar,
                        delegateInputs[sourceDocRefVar],
                    ),
                    getDelegateStringInput(
                        execution,
                        delegateName,
                        sourceRoleVar,
                        delegateInputs[sourceRoleVar],
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
        ),
        execution.getOrFail(id, delegateName),
    ).also { logger().debug(it.toString()) }
}

class DeleteRelationshipLinkDelegateParameters :
    DelegateParameters(
        mapOf(
            InputVars.RELATIONSHIP_MODEL.label to true,
            InputVars.SOURCE_ROLE.label to true,
            InputVars.SOURCE_DOC_REF.label to true,
            InputVars.TARGET_ROLE.label to true,
            InputVars.TARGET_DOC_REF.label to true,
            InputVars.ID.label to true,
        ),
    ) {
    enum class InputVars(val label: String) {
        RELATIONSHIP_MODEL("relationshipModel"),
        SOURCE_ROLE("sourceRole"),
        SOURCE_DOC_REF("sourceDocRef"),
        TARGET_ROLE("targetRole"),
        TARGET_DOC_REF("targetDocRef"),
        ID("id"),
    }
}
