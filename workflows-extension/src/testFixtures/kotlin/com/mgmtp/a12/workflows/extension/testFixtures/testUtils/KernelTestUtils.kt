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

package com.mgmtp.a12.workflows.extension.testFixtures.testUtils

import com.mgmtp.a12.kernel.md.document.api.services.DocumentDeserializationConfig
import com.mgmtp.a12.kernel.md.document.api.services.DocumentSerializationConfig
import com.mgmtp.a12.kernel.md.document.apiV2.immutable.DocumentV2
import com.mgmtp.a12.kernel.md.facade.DocumentModelServiceFactory
import com.mgmtp.a12.kernel.md.facade.DocumentServiceFactory
import com.mgmtp.a12.kernel.md.model.api.IDocumentModel
import com.mgmtp.a12.kernel.md.model.api.services.IDocumentModelResolver
import com.mgmtp.a12.model.notification.RankedNotification
import java.io.StringReader

fun deserializeDocumentV2(documentString: String, documentModelString: String, documentModelName: String): DocumentV2 {
    val documentModelSerializer = DocumentModelServiceFactory().createDocumentModelSerializer()
    val documentModelResolver = object : IDocumentModelResolver {
        override fun getDocumentModelById(id: String): IDocumentModel = documentModelSerializer.deserialize(documentModelString.reader())
    }
    val documentSerializer = DocumentServiceFactory(documentModelResolver).createDocumentV2Serializer()
    val jsonDeserializationConfig: DocumentDeserializationConfig = DocumentDeserializationConfig
        .builder()
        .format(DocumentSerializationConfig.Format.JSON)
        .build()
    return documentSerializer.deserializeV2(
        documentString.reader(),
        documentModelName,
        jsonDeserializationConfig,
    ) { n: RankedNotification ->
        println(n)
    }
}

fun deserializeDocumentModel(documentModelString: String): IDocumentModel = DocumentModelServiceFactory().createDocumentModelSerializer().deserialize(StringReader(documentModelString))
