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
package com.mgmtp.a12.workflows.extension.exception

object WorkflowsRpcExceptionKeys {
    const val START_PROCESS_HTTP_CLIENT_EXCEPTION = "error.workflows.start.process.http.client.exception"
    const val TASK_DOCUMENT_NOT_FOUND_EXCEPTION = "error.workflows.task.document.notFound"
    const val TASK_METADATA_NOT_FOUND_EXCEPTION = "error.workflows.task.metadata.notFound"
    const val SEND_MESSAGE_HTTP_CLIENT_EXCEPTION = "error.workflows.send.message.http.client.exception"
    const val ASSIGN_TASK_HTTP_CLIENT_EXCEPTION = "error.workflows.assign.task.http.client.exception"
    const val COMPLETE_TASK_HTTP_CLIENT_EXCEPTION = "error.workflows.complete.task.http.client.exception"
    const val METADATA_FIELD_NOT_FOUND_EXCEPTION = "error.workflows.task.metadataField.notFound"
    const val PROCESS_ENGINE_ACCESS_EXCEPTION = "error.workflows.process.engine.access.exception"
}
