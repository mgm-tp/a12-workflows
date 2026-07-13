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

package com.mgmtp.a12.model.workflows.migration.utils

import java.io.*
import java.util.*

/**
 * Captures the output written to System.out
 */
class ConsoleOutputCapturer {
    private var baos: ByteArrayOutputStream? = null
    private var previous: PrintStream? = null
    private var capturing = false
    fun start() {
        if (capturing) {
            return
        }
        capturing = true
        previous = System.out
        baos = ByteArrayOutputStream()
        val outputStreamCombiner = OutputStreamCombiner(arrayListOf(previous, baos))
        val custom = runCatching {
            PrintStream(outputStreamCombiner, false, "UTF-8")
        }.getOrElse { exception -> throw RuntimeException(exception) }
        System.setOut(custom)
    }

    fun stop(): String {
        if (!capturing) {
            return ""
        }
        System.setOut(previous)
        val capturedValue = baos?.toString("utf-8")
        baos?.close()
        baos = null
        previous = null
        capturing = false
        return capturedValue.toString()
    }

    private class OutputStreamCombiner(private val outputStreams: List<OutputStream?>) : OutputStream() {
        @Throws(IOException::class)
        override fun write(b: Int) = outputStreams.forEach { it?.write(b) }

        @Throws(IOException::class)
        override fun flush() = outputStreams.forEach { it?.flush() }

        @Throws(IOException::class)
        override fun close() = outputStreams.forEach { it?.close() }
    }
}
