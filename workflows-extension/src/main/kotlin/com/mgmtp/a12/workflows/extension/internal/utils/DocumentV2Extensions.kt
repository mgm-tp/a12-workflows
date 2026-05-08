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
package com.mgmtp.a12.workflows.extension.internal.utils

import com.mgmtp.a12.kernel.md.document.apiV2.DocumentPointer
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.DocumentV2
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.GroupInstanceV2
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.utils.IDocumentV2Visitor
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.utils.IDocumentV2Visitor.DescendType
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.A12WF

fun DocumentV2.getDocumentPointerToA12wfMetadata(): DocumentPointer? {
    var a12wfPointer: DocumentPointer? = null
    this.traverse(object : IDocumentV2Visitor {
        override fun visitGroup(
            pointerRelativeToBase: DocumentPointer,
            group: GroupInstanceV2,
        ): DescendType = if (pointerRelativeToBase.fullName().contains(A12WF)) {
            a12wfPointer = pointerRelativeToBase
            DescendType.SKIP_CHILDREN
        } else {
            DescendType.VISIT_CHILDREN
        }
    })
    return a12wfPointer
}

fun DocumentV2.getTextValueFromA12wfMetadata(relativePath: String): String? {
    val a12wfPointer = this.getDocumentPointerToA12wfMetadata()
    if (a12wfPointer == null) {
        return null
    }
    val pointerToMetadataField = DocumentPointer.of(a12wfPointer.toString() + relativePath)
    return this.fieldValue(pointerToMetadataField) as String?
}
