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

package com.mgmtp.a12.workflows.engine.internal.delegates.setstatus

import com.mgmtp.a12.dataservices.document.DocumentReference
import com.mgmtp.a12.workflows.engine.internal.utils.service.rpc.DataServicesRpcFacade
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

private class SetStatusServiceTest {

    val rpcFacade: DataServicesRpcFacade = mockk(relaxed = true)

    val service = SetStatusService(rpcFacade)

    companion object {
        const val PATH = "/group1/group2/field1"
        const val NEW_VALUE = "newValue"
        val docRef = DocumentReference("Test/1")
        val repetitions = intArrayOf(1, 2, 1)
    }

    @Test
    fun `should make a SET_STATUS RPC request with given parameters`() {
        // when
        service.setStatus(docRef, PATH, repetitions, NEW_VALUE)

        // then
        verify(exactly = 1) { rpcFacade.trySetStatus(docRef, PATH, repetitions, NEW_VALUE) }
    }
}
