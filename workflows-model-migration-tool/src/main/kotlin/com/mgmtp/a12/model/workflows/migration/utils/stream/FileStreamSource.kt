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

package com.mgmtp.a12.model.workflows.migration.utils.stream

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

/**
 * File based implementation of [StreamSource]
 *
 */
class FileStreamSource : StreamSource {
    private val inputPath: Path
    private val outputPath: Path

    /**
     * Creates a [StreamSource] based on content of the given file.
     *
     * @param pathname A pathname string. Must not be null.
     * @see
     */
    constructor(pathname: Path) {
        inputPath = pathname
        outputPath = inputPath
    }

    /**
     * Instantiates a new File stream source.
     *
     * @param inputPathname the input pathname
     * @param outputPathname the output pathname
     */
    constructor(inputPathname: Path, outputPathname: Path) {
        inputPath = inputPathname
        outputPath = outputPathname
    }

    @Throws(IOException::class)
    override fun getOutput(): OutputStream = Files.newOutputStream(outputPath)

    override fun getModelFilePath(): Path = inputPath

    @Throws(IOException::class)
    override fun getInput(): InputStream = Files.newInputStream(inputPath)
}
