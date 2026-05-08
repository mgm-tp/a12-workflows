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
package com.mgmtp.a12.workflows.engine.internal.utils.service.rpc

import com.mgmtp.a12.dataservices.rpc.JsonRpc2Response
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientException
import org.springframework.web.server.ResponseStatusException

private val logger = LoggerFactory.getLogger("com.mgmtp.a12.workflows.engine.internal.service.rpc.DataServicesRpcUtil")

fun tryRpcOrFail(request: () -> List<JsonRpc2Response>): List<JsonRpc2Response> {
    try {
        val response = request()
        logAndThrowIfAnyError(response)
        return response
    } catch (e: RestClientException) {
        logger.error("The document could not be created or modified due to following exception: ${e.message}")
        logger.error(e.stackTraceToString())
        throw e
    }
}

fun trySingleRpcOrFail(request: () -> JsonRpc2Response): JsonRpc2Response = tryRpcOrFail { listOf(request()) }.single()

private fun logAndThrowIfAnyError(response: List<JsonRpc2Response>) {
    var hasError = false
    val errors = mutableListOf<String>()
    response.filter { !it.isSuccess }.forEach {
        logger.debug("The document could not be created or modified due to exception in JSON RPC request: {}", it.error)
        errors.add(it.error.message)
        hasError = true
    }
    if (hasError) {
        throwBadRequest("The document could not be created or modified due to exception in JSON RPC request. Failed because : \n ${errors.joinToString(separator = "\n")}")
    }
}

fun throwBadRequest(message: String): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, message)
