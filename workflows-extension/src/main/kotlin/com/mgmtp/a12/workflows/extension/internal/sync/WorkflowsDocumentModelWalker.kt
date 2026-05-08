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
package com.mgmtp.a12.workflows.extension.internal.sync

import com.mgmtp.a12.dataservices.utils.internal.DocumentUtils
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.DocumentV2
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.FieldInstanceV2
import com.mgmtp.a12.kernel.md.facade.DocumentServiceFactory
import com.mgmtp.a12.kernel.md.model.api.IDocumentModel
import com.mgmtp.a12.kernel.md.model.api.IField
import com.mgmtp.a12.kernel.md.model.api.fieldtypes.IDateRangeType
import com.mgmtp.a12.kernel.md.model.api.services.IDocumentModelResolver
import com.mgmtp.a12.kernel.md.model.api.visitor.DocumentModelVisitor
import com.mgmtp.a12.kernel.md.model.api.visitor.DocumentModelWalker
import com.mgmtp.a12.kernel.md.serializer.document.internal.service.serialization.DocumentSerializationUtils
import com.mgmtp.a12.utils.conversion.InstantRange
import com.mgmtp.a12.workflows.shared.dtos.ProcessEngineVariable
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import org.springframework.stereotype.Component
import java.util.*

private const val WORKFLOWS_SYNC_ANNOTATION = "availableInProcessAs"

@Component
class WorkflowsDocumentModelWalker(
    private val documentModelResolver: IDocumentModelResolver,
) {

    private val documentModelWalker = DocumentModelWalker()

    private val documentServiceFactory = DocumentServiceFactory(documentModelResolver)

    fun collectSyncAnnotatedFields(
        documentModel: IDocumentModel,
        documentV2: DocumentV2,
    ): SyncAnnotationCollectionResult {
        val modificationMap = mutableMapOf<String, ProcessEngineVariable<*>>()

        documentModelWalker.acceptDocumentModel(
            documentModel,
            object : DocumentModelVisitor() {
                override fun visitField(field: IField): DocumentModelWalker.VisitProcess {
                    val processVariableName = field.annotations
                        .firstOrNull { it.name == WORKFLOWS_SYNC_ANNOTATION }
                        ?.value

                    if (processVariableName === null) {
                        return super.visitField(field)
                    }

                    logger().debug("Found field {} to sync", field.name)

                    val dmAwareDocService = documentServiceFactory.createDmAwareDocService(documentModel.header.id)
                    val fieldList = dmAwareDocService.getFieldInstancesById(documentV2, field.id)

                    if (fieldList.isEmpty()) {
                        modificationMap[processVariableName] =
                            getFieldValueAsProcessEngineVariable(field, null, documentModel)
                    } else {
                        fieldList
                            .forEach {
                                modificationMap[processVariableName] =
                                    getFieldValueAsProcessEngineVariable(field, it.value, documentModel)
                            }
                    }
                    return super.visitField(field)
                }
            },
        )
        return SyncAnnotationCollectionResult(modificationMap)
    }

    private fun getFieldValueAsProcessEngineVariable(
        field: IField,
        fieldInstance: FieldInstanceV2?,
        documentModel: IDocumentModel,
    ): ProcessEngineVariable<String?> {
        val type = field.effectiveType.get()
        val value = fieldInstance?.value()
        val timeZone = TimeZone.getTimeZone(
            documentModel.content.documentModelConfig.timeZone,
        )
        val syncValue = if (type is IDateRangeType) {
            if (value is InstantRange) {
                val dateArray = arrayOf(Date.from(value.start()), Date.from(value.end()))
                DocumentSerializationUtils.serializeDateRange(
                    dateArray,
                    type.rangeSeparator,
                    type.format,
                    timeZone,
                )
            } else {
                throw IllegalArgumentException("Mismatched field type and value.")
            }
        } else {
            DocumentUtils.format(type, value, timeZone)
        }
        return ProcessEngineVariable(syncValue)
    }
}
