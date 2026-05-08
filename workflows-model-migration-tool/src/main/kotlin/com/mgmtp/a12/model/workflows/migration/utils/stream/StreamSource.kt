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
package com.mgmtp.a12.model.workflows.migration.utils.stream

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.*

/**
 * Simple interface for objects that are sources for an [InputStream].
 *
 *
 *
 * Allow one to read the underlying content stream multiple times.
 *
 */
interface StreamSource {

    /**
     * Return an [InputStream].
     *
     * It is expected that each call creates a *fresh* stream.
     *
     * This requirement is particularly important when you consider an API such
     * as JavaMail, which needs to be able to read the stream multiple times when
     * creating mail attachments. For such a use case, it is *required*
     * that each `getInputStream()` call returns a fresh stream.
     * @return the input stream for the underlying resource (must not be `null`)
     * @throws IOException if the stream could not be opened
     */
    @Throws(IOException::class)
    fun getInput(): InputStream

    /**
     * Return an [OutputStream] for the underlying resource, allowing to (over-)write its content.
     *
     * @throws IOException if the stream could not be opened
     * @see .getInput
     */
    @Throws(IOException::class)
    fun getOutput(): OutputStream

    fun getModelFilePath(): Path?
}
