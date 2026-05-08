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

import Process from "node:process";
import Path from "node:path";
import { fileURLToPath } from "node:url";

import { run } from "jscodeshift/src/Runner.js";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

const cwd = Process.cwd();

const targetVersions = ["12"];

// hideBin(process.argv) is required to properly pass the command-line arguments to yargs.
// This removes the first two arguments (node and the script file path) from process.argv.
const argv = yargs(hideBin(process.argv))
	.usage(
		"Usage: $0 <source-dir-path> [options]\n\n" +
			`Note that the tool requires the current Workflows version to be the previous major one. E.g, if you are using Workflows 11.x, you can only migrate to 12.x`
	)
	.describe("help", "Show help")
	.help("h")
	.demandCommand(1, "Please specify the path to target source directory")
	.option({
		targetVersion: {
			demandOption: true,
			describe: "The target Workflows version is migrating to",
			alias: "t",
			string: true,
			choices: targetVersions
		}
	})
	.locale("en")
	.parseSync();

async function main() {
	const path = String(argv._?.[0]);

	const absolutePath = Path.resolve(cwd, path);

	// Workaround/Hack for the time being, since import.meta.dirname does not work.
	const dirname = Path.dirname(fileURLToPath(import.meta.url));
	await run(Path.join(dirname, "transforms", `${argv.targetVersion}.0.0`, "transform.js"), [absolutePath], {
		extensions: "ts,tsx",
		parser: "tsx",
		ignorePattern: "**/node_modules/**",
		verbose: 1
	});
}
main().catch((error) => {
	console.error(error);
	process.exit(1);
});
