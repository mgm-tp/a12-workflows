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
package com.mgmtp.a12.workflows.engine.client.internal

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.mgmtp.a12.connector.rest.RestGetConnector
import com.mgmtp.a12.connector.rest.RestPostConnector
import com.mgmtp.a12.connector.rest.RestServerRequest
import com.mgmtp.a12.workflows.engine.client.internal.configuration.ProcessEngineClientProperties
import com.mgmtp.a12.workflows.engine.client.internal.dto.process.ProcessInstanceDto
import com.mgmtp.a12.workflows.engine.client.internal.dto.process.StartProcessInstanceDto
import com.mgmtp.a12.workflows.engine.client.internal.dto.task.CompleteTaskDto
import com.mgmtp.a12.workflows.engine.client.internal.dto.task.UpdateTaskDto
import com.mgmtp.a12.workflows.engine.client.internal.dto.task.UserIdDto
import com.mgmtp.a12.workflows.shared.dtos.MessageDto
import com.mgmtp.a12.workflows.shared.dtos.ProcessEngineVariable
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component

/**
 * REST implementation of ProcessEngineClient which uses HTTP client to issue HTTP request to server.
 */
@Component
@EnableConfigurationProperties(ProcessEngineClientProperties::class)
class ProcessEngineRestClient(
    val processEngineClientProperties: ProcessEngineClientProperties,
    private val restPostConnector: RestPostConnector,
    val restGetConnector: RestGetConnector,
    private val objectMapper: ObjectMapper,
) : ProcessEngineClient {

    override fun startProcessInstance(
        processDefinitionKey: String,
        businessKey: String,
        variables: Map<String, ProcessEngineVariable<*>>,
    ) {
        val uri = "${processEngineClientProperties.baseUrl}/process-definition/key/$processDefinitionKey/start"
        val requestPayload = StartProcessInstanceDto(businessKey, variables)
        val request = RestServerRequest.withPayload(requestPayload)

        restPostConnector.callServer(uri, request, ProcessInstanceDto::class.java)
    }

    override fun updateTask(
        taskId: String,
        variables: Map<String, ProcessEngineVariable<*>>,
    ) {
        val uri = "${processEngineClientProperties.baseUrl}/task/$taskId/variables"
        val requestPayload = UpdateTaskDto(variables)
        val request = RestServerRequest.withPayload(requestPayload)
        restPostConnector.callServer(uri, request, Unit::class.java)
    }

    override fun completeTask(taskId: String, businessKey: String, variables: Map<String, ProcessEngineVariable<*>>) {
        val uri = "${processEngineClientProperties.baseUrl}/task/$taskId/complete"

        val requestPayload = CompleteTaskDto(variables)
        val request = RestServerRequest.withPayload(requestPayload)
        restPostConnector.callServer(uri, request, Unit::class.java)
    }

    override fun assign(taskId: String, businessKey: String, assignee: String) {
        val uri = "${processEngineClientProperties.baseUrl}/task/$taskId/assignee"
        val request = RestServerRequest.withPayload(UserIdDto(assignee))
        restPostConnector.callServer(uri, request, Unit::class.java)
    }

    override fun sendMessage(message: MessageDto) {
        val uri = "${processEngineClientProperties.baseUrl}/message/"
        val request = RestServerRequest.withPayload(message)
        restPostConnector.callServer(uri, request, Unit::class.java)
    }

    override fun getVariables(taskId: String): Map<String, ProcessEngineVariable<*>> {
        val uri = "${processEngineClientProperties.baseUrl}/task/$taskId/variables"
        val request = RestServerRequest.empty()
        val responseData = restGetConnector.callServer(uri, request, String::class.java).data

        return objectMapper.readValue(
            responseData,
            object : TypeReference<Map<String, ProcessEngineVariable<*>>?>() {},
        ) ?: emptyMap()
    }
}
