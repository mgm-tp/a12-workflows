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

import type { API, Collection, FileInfo, JSCodeshift } from "jscodeshift";

export default function transformer(file: FileInfo, { jscodeshift: j }: API) {
	const source = j(file.source);

	const containsWorkflowsAndTaskOverviewImport =
		source
			.find(j.ImportDeclaration)
			.filter((path) => {
				const importSource = path.node.source.value as string;
				return (
					typeof importSource === "string" &&
					importSource.includes("workflows-core") &&
					importSource.includes("taskOverviewDataProvider")
				);
			})
			.size() > 0;

	const containsOverviewEngineFactoriesCall =
		source
			.find(j.CallExpression, {
				callee: {
					type: "MemberExpression",
					object: { name: "OverviewEngineFactories" },
					property: { name: "createDataProviders" }
				}
			})
			.size() > 0;

	if (!containsWorkflowsAndTaskOverviewImport || !containsOverviewEngineFactoriesCall) {
		return file.source;
	}

	transformationForA12WF1601(j, source);

	return source.toSource({ trailingComma: true, useTabs: true });
}

function transformationForA12WF1601(j: JSCodeshift, source: Collection<any>) {
	// Adjust imports
	source
		.find(j.ImportDeclaration, {
			source: { value: "@com.mgmtp.a12.workflows/workflows-core/lib/data/taskOverviewDataProvider" }
		})
		.remove();

	const factoriesPath = "@com.mgmtp.a12.workflows/workflows-core/lib/factories";
	const existingWFImport = source.find(j.ImportDeclaration, { source: { value: factoriesPath } });

	if (existingWFImport.length === 0) {
		source
			.get()
			.node.program.body.unshift(
				j.importDeclaration([j.importSpecifier(j.identifier("WorkflowsFactories"))], j.literal(factoriesPath))
			);
	} else {
		existingWFImport.forEach((path) => {
			const specs = path.node.specifiers;
			const alreadyImported =
				specs && specs.some((spec) => spec.type === "ImportSpecifier" && spec.imported.name === "WorkflowsFactories");
			if (!alreadyImported) {
				specs?.push(j.importSpecifier(j.identifier("WorkflowsFactories")));
			}
		});
	}

	// Remove TaskOverviewDataProvider()
	source.find(j.NewExpression, { callee: { name: "TaskOverviewDataProvider" } }).forEach((path) => j(path).remove());

	// Remove taskOverviewDataProvider
	source.find(j.ArrayExpression).forEach((path) => {
		path.node.elements = path.node.elements.filter((element) => {
			return !(element && element.type === "Identifier" && element.name === "taskOverviewDataProvider");
		});
	});

	// Insert WorkflowsFactories.createDataLoader() as argument into OverviewEngineFactories.createDataProviders()
	source
		.find(j.SpreadElement, {
			argument: {
				type: "CallExpression",
				callee: { object: { name: "OverviewEngineFactories" }, property: { name: "createDataProviders" } }
			}
		})
		.forEach((path) => {
			const callExpression: any = path.node.argument;
			callExpression.arguments = [
				j.callExpression(
					j.memberExpression(j.identifier("WorkflowsFactories"), j.identifier("createDataLoader"), false),
					[]
				)
			];
		});
}
