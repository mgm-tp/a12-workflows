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
package com.mgmtp.a12.workflows.shared.dtos

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.ASSIGNEE
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.BUSINESS_KEY
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.CREATION_DATE
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.DUE_DATE
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.FORM_KEY
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.ID
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.INITIATOR
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.KEY
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.NAME
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.PROCESS
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.PROCESS_DEFINITION
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.TASK
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.TASK_DEFINITION
import com.mgmtp.a12.workflows.shared.internal.constants.MetadataPropertyNames.WORKFLOWS_INTERNAL_DOCUMENT_STATUS

data class ProcessMetadata @JsonCreator constructor(
    @JsonProperty(PROCESS_DEFINITION)
    val processDefinition: ProcessDefinition,
    @JsonProperty(TASK_DEFINITION)
    val taskDefinition: TaskDefinition,
    @JsonProperty(PROCESS)
    val process: Process,
    @JsonProperty(TASK)
    val task: Task,
    @Deprecated("This property is not being used anymore and will be removed in the next June release.")
    @JsonProperty(WORKFLOWS_INTERNAL_DOCUMENT_STATUS)
    val workflowsInternalDocumentStatus: String = "",
)

data class ProcessDefinition(
    @JsonProperty(KEY)
    val key: String,
    @JsonProperty(NAME)
    val name: String,
)

data class TaskDefinition(
    @JsonProperty(KEY)
    val key: String,
    @JsonProperty(NAME)
    val name: String,
    @JsonProperty(FORM_KEY)
    val formKey: String?,
)

data class Process(
    @JsonProperty(ID)
    val id: String,
    @JsonProperty(BUSINESS_KEY)
    val businessKey: String?,
    @JsonProperty(INITIATOR)
    val initiator: String?,
)

data class Task(
    @JsonProperty(ID)
    val id: String,
    @JsonProperty(CREATION_DATE)
    val creationDate: String,
    @Deprecated("This property is not being used anymore and will be removed in the next June release.")
    @JsonProperty(DUE_DATE)
    val dueDate: String?,
    @JsonProperty(ASSIGNEE)
    val assignee: String?,
)
