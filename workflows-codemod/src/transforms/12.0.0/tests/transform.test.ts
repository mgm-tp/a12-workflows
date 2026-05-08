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

import { applyTransform } from "@hypermod/utils";

import transformer from "../transform.js";

function inputWithDataProvider(dataProviderExpression: string): string {
	return `
			import { TaskOverviewDataProvider } from "@com.mgmtp.a12.workflows/workflows-core/lib/data/taskOverviewDataProvider";

			export function setup(): PreviewAppClientSetup {
				const dataHandlers: DataProvider[] = [
				    createCddDataProvider(),
				    createEmptyDocumentDataProvider(),
				    ${dataProviderExpression},
				    RelationshipFactories.createRelationshipDataProvider(),
				    TreeEngineFactories.createDataProvider(),
				    ...OverviewEngineFactories.createDataProviders(),
				    TreeEngineServerConnectorFactories.createDataProvider({ preloadChildNodes: true }),
				    platformSingleDocumentDataProvider,
				];
			}
		`;
}

const expectedOutput = `
			"import { WorkflowsFactories } from "@com.mgmtp.a12.workflows/workflows-core/lib/factories";

						export function setup(): PreviewAppClientSetup {
							const dataHandlers: DataProvider[] = [
							    createCddDataProvider(),
							    createEmptyDocumentDataProvider(),
							    RelationshipFactories.createRelationshipDataProvider(),
							    TreeEngineFactories.createDataProvider(),
							    ...OverviewEngineFactories.createDataProviders(WorkflowsFactories.createDataLoader()),
							    TreeEngineServerConnectorFactories.createDataProvider({ preloadChildNodes: true }),
							    platformSingleDocumentDataProvider,
							];
						}"
		`;

it("should insert workflowsDataLoader as argument into Overview Engine's dataProvider and remove new TaskOverviewDataProvider()", async () => {
	const result = await applyTransform(transformer, inputWithDataProvider("new TaskOverviewDataProvider()"), {
		parser: "ts"
	});

	expect(result).toMatchInlineSnapshot(expectedOutput);
});

it("should insert workflowsDataLoader as argument into Overview Engine's dataProvider and remove taskOverviewDataProvider", async () => {
	const result = await applyTransform(transformer, inputWithDataProvider("taskOverviewDataProvider"), { parser: "ts" });

	expect(result).toMatchInlineSnapshot(expectedOutput);
});

it("should not transform files without import of taskOverviewDataProvider and OverviewEngineFactories.createDataProviders call", async () => {
	const testCases = [
		`
    import { OtherFactory } from "@example/lib";
    import { taskOverviewDataProvider } from "@com.mgmtp.a12.workflows/workflows-core/lib/data/taskOverviewDataProvider";
    
    export function setup() {
      return [
        ...OtherFactory.createDataProviders(),
        taskOverviewDataProvider
      ];
    }
    `,
		`
    import { TaskOverviewDataProvider } from "@com.mgmtp.a12.workflows/workflows-core/lib/data/taskOverviewDataProvider";
    
    export function setup() {
      const dataProvider = new TaskOverviewDataProvider();
      return [dataProvider];
    }
    `,
		`
    import { OverviewEngineFactories } from "@com.mgmtp.a12.overviewengine/overviewengine-core/lib/main/client-extensions";
    
    export function setup() {
      return [
        ...OverviewEngineFactories.createApplicationSagas()
      ];
    }
    `,
		`
    import { OverviewEngineFactories } from "@com.mgmtp.a12.overviewengine/overviewengine-core/lib/main/client-extensions";
    
    export function setup() {
      return [
        ...OverviewEngineFactories.createDataProviders()
      ];
    }
    `
	];

	for (const testCase of testCases) {
		const result = await applyTransform(transformer, testCase, { parser: "ts" });

		// Ignore whitespace differences for comparison
		const normalizedResult = result.replace(/\s+/g, "");
		const normalizedTestCase = testCase.replace(/\s+/g, "");
		expect(normalizedResult).toBe(normalizedTestCase);
	}
});
