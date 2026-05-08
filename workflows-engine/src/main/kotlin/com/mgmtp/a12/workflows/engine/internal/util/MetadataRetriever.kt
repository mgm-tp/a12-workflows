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
package com.mgmtp.a12.workflows.engine.internal.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.mgmtp.a12.workflows.shared.dtos.Process
import com.mgmtp.a12.workflows.shared.dtos.ProcessDefinition
import com.mgmtp.a12.workflows.shared.dtos.ProcessMetadata
import com.mgmtp.a12.workflows.shared.dtos.Task
import com.mgmtp.a12.workflows.shared.dtos.TaskDefinition
import org.cibseven.bpm.engine.RepositoryService
import org.cibseven.bpm.engine.RuntimeService
import org.cibseven.bpm.engine.delegate.DelegateTask
import org.cibseven.bpm.engine.impl.persistence.entity.TaskEntity
import org.cibseven.bpm.model.bpmn.impl.BpmnModelConstants
import org.springframework.stereotype.Component
import java.util.*
import org.cibseven.bpm.model.bpmn.instance.Task as BpmnTask

@Component
class MetadataRetriever(
    val repositoryService: RepositoryService,
    val runtimeService: RuntimeService,
    val objectMapper: ObjectMapper,
) {

    private data class ProcessLevelMetadata(val processDefinition: ProcessDefinition, val process: Process)
    private data class TaskLevelMetadata(val taskDefinition: TaskDefinition, val task: Task)

    fun retrieveMetadata(delegateTask: DelegateTask): ProcessMetadata {
        val execution = delegateTask.execution
        val (processDefinition, process) = retrieveProcessLevelMetadata(
            execution.id,
            execution.processDefinitionId,
            execution.processInstanceId,
            execution.businessKey,
        )
        val (taskDefinition, task) = retrieveTaskLevelMetadata(delegateTask)

        return ProcessMetadata(processDefinition, taskDefinition, process, task)
    }

    private fun retrieveProcessLevelMetadata(
        executionId: String,
        processDefinitionId: String,
        processInstanceId: String,
        businessKey: String?,
    ): ProcessLevelMetadata {
        val processEngineProcessDefinition = repositoryService.getProcessDefinition(processDefinitionId)
        val processDefinition = ProcessDefinition(
            processEngineProcessDefinition.key,
            processEngineProcessDefinition.name ?: processEngineProcessDefinition.key,
        )
        val initiatorVariable = runtimeService.getVariable(executionId, "initiator") as String?
        val process = Process(
            processInstanceId,
            businessKey?.ifEmpty { null },
            initiatorVariable?.ifEmpty { null },
        )

        return ProcessLevelMetadata(processDefinition, process)
    }

    private fun retrieveTaskLevelMetadata(delegateTask: DelegateTask): TaskLevelMetadata {
        val formKey = getTaskFormKey(delegateTask, delegateTask.processDefinitionId, delegateTask.taskDefinitionKey)
        val taskDefinition =
            TaskDefinition(delegateTask.taskDefinitionKey, delegateTask.name, formKey)
        val task = createTask(delegateTask.id, delegateTask.createTime, delegateTask.dueDate, delegateTask.assignee)

        return TaskLevelMetadata(taskDefinition, task)
    }

    private fun getTaskFormKey(taskEntity: Any, processDefinitionId: String, taskDefinitionKey: String): String = if (taskEntity is TaskEntity) {
        getInitializedFormKey(taskEntity)
    } else {
        getTaskFormKeyFromModel(processDefinitionId, taskDefinitionKey)
    } ?: taskDefinitionKey

    private fun getInitializedFormKey(taskEntity: TaskEntity) = try {
        taskEntity.formKey
    } catch (e: Exception) {
        taskEntity.initializeFormKey()
        taskEntity.formKey
    }

    private fun getTaskFormKeyFromModel(processDefinitionId: String, taskDefinitionKey: String): String {
        val taskModel = repositoryService.getBpmnModelInstance(processDefinitionId)
        val taskModelElement = taskModel.getModelElementById<BpmnTask>(taskDefinitionKey)
        return taskModelElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, "formKey")
    }

    private fun createTask(taskId: String, createTime: Date, dueTime: Date?, assignee: String?): Task {
        val creationDate = objectMapper.dateFormat.format(createTime)
        val dueDate = formatDateOrEmpty(dueTime)

        return Task(taskId, creationDate, dueDate, assignee)
    }

    private fun formatDateOrEmpty(date: Date?): String? = when (date) {
        null -> null
        else -> objectMapper.dateFormat.format(date)
    }
}
