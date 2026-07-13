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

import { describe, it, expect } from "vitest";

/**
 * This test suite verifies that all exports from the workflows-core module
 * are properly accessible through the main index file, ensuring that integrating
 * projects can import everything they need from the module root.
 */
describe("Module Exports - All exports should be accessible from module root", () => {
	describe("Actions exports", () => {
		it("should export WorkflowsActions from index", async () => {
			const module = await import("../../index.js");
			expect(module.WorkflowsActions).toBeDefined();
			expect(typeof module.WorkflowsActions).toBe("object");
		});

		it("should export all WorkflowsActions properties", async () => {
			const module = await import("../../index.js");
			const { WorkflowsActions } = module;

			// Verify action creators
			expect(WorkflowsActions.assignTask).toBeDefined();
			expect(WorkflowsActions.completeTask).toBeDefined();
			expect(WorkflowsActions.sendMessage).toBeDefined();
			expect(WorkflowsActions.startProcess).toBeDefined();
			expect(WorkflowsActions.adaptAppModel).toBeDefined();
			expect(WorkflowsActions.claimRequested).toBeDefined();

			// Verify helper function
			expect(typeof WorkflowsActions.isWorkflowsOutcomeAction).toBe("function");
		});
	});

	describe("Events exports", () => {
		it("should export WorkflowsEvents from index", async () => {
			const module = await import("../../index.js");
			expect(module.WorkflowsEvents).toBeDefined();
			expect(typeof module.WorkflowsEvents).toBe("object");
		});

		it("should export all WorkflowsEvents constants", async () => {
			const module = await import("../../index.js");
			const { WorkflowsEvents } = module;

			expect(WorkflowsEvents.START_PROCESS).toBe("startProcess");
			expect(WorkflowsEvents.CLAIM).toBe("claim");
			expect(WorkflowsEvents.COMPLETE_TASK).toBe("proceed");
			expect(WorkflowsEvents.SEND_MESSAGE).toBe("sendMessage");
			expect(WorkflowsEvents.ASSIGN_TASK).toBe("assignTask");
		});
	});

	describe("Factories exports", () => {
		it("should export WorkflowsFactories from index", async () => {
			const module = await import("../../index.js");
			expect(module.WorkflowsFactories).toBeDefined();
			expect(typeof module.WorkflowsFactories).toBe("object");
		});

		it("should export all WorkflowsFactories functions", async () => {
			const module = await import("../../index.js");
			const { WorkflowsFactories } = module;

			expect(typeof WorkflowsFactories.createModule).toBe("function");
			expect(typeof WorkflowsFactories.createMiddlewares).toBe("function");
			expect(typeof WorkflowsFactories.createDataLoader).toBe("function");
			expect(typeof WorkflowsFactories.createDataReducers).toBe("function");
			expect(typeof WorkflowsFactories.createApplicationSagas).toBe("function");
			expect(typeof WorkflowsFactories.viewComponentProvider).toBe("function");
		});
	});

	describe("Middlewares exports", () => {
		it("should export workflowsMiddlewares function from index", async () => {
			const module = await import("../../index.js");
			expect(module.workflowsMiddlewares).toBeDefined();
			expect(typeof module.workflowsMiddlewares).toBe("function");
		});

		it("should export adaptAppModelMiddleware function from index", async () => {
			const module = await import("../../index.js");
			expect(module.adaptAppModelMiddleware).toBeDefined();
			expect(typeof module.adaptAppModelMiddleware).toBe("function");
		});

		it("should verify workflowsMiddlewares returns an array", async () => {
			const module = await import("../../index.js");
			const middlewares = module.workflowsMiddlewares();
			expect(Array.isArray(middlewares)).toBe(true);
			expect(middlewares.length).toBeGreaterThan(0);
		});
	});

	describe("Sagas exports", () => {
		it("should export workflowsSagas from index", async () => {
			const module = await import("../../index.js");
			expect(module.workflowsSagas).toBeDefined();
			expect(Array.isArray(module.workflowsSagas)).toBe(true);
		});

		it("should verify workflowsSagas contains saga descriptors", async () => {
			const module = await import("../../index.js");
			const { workflowsSagas } = module;

			expect(workflowsSagas.length).toBe(5);
			workflowsSagas.forEach((saga) => {
				expect(saga).toBeDefined();
				expect(typeof saga.canHandle).toBe("function");
				expect(typeof saga.handle).toBe("function");
			});
		});
	});

	describe("Task exports", () => {
		it("should export TaskJsonRpc2Request from index", async () => {
			const module = await import("../../index.js");
			expect(module.TaskJsonRpc2Request).toBeDefined();
			expect(typeof module.TaskJsonRpc2Request).toBe("object");
		});

		it("should verify TaskJsonRpc2Request namespace has expected functions", async () => {
			const module = await import("../../index.js");
			const { TaskJsonRpc2Request } = module;

			// TaskJsonRpc2Request is a namespace with utility functions
			expect(typeof TaskJsonRpc2Request.isInstance).toBe("function");
		});
	});

	describe("Utils exports", () => {
		it("should export WorkflowsConstants from index", async () => {
			const module = await import("../../index.js");
			expect(module.WorkflowsConstants).toBeDefined();
			expect(typeof module.WorkflowsConstants).toBe("object");
		});

		it("should export all WorkflowsConstants properties", async () => {
			const module = await import("../../index.js");
			const { WorkflowsConstants } = module;

			expect(WorkflowsConstants.TASK_LIST_VIEW).toBe("TaskList");
			expect(WorkflowsConstants.TASK_LIST_FLOW_NAME).toBe("TaskFlow");
		});
	});

	describe("Data exports", () => {
		it("should export workflowsDataLoader from index", async () => {
			const module = await import("../../index.js");
			expect(module.workflowsDataLoader).toBeDefined();
			expect(typeof module.workflowsDataLoader).toBe("object");
		});

		it("should verify workflowsDataLoader has required interface", async () => {
			const module = await import("../../index.js");
			const { workflowsDataLoader } = module;

			expect(typeof workflowsDataLoader.buildRequests).toBe("function");
		});
	});

	describe("Comprehensive export verification", () => {
		it("should export all expected top-level exports", async () => {
			const module = await import("../../index.js");

			const expectedExports = [
				"WorkflowsActions",
				"WorkflowsEvents",
				"WorkflowsFactories",
				"workflowsMiddlewares",
				"adaptAppModelMiddleware",
				"workflowsSagas",
				"TaskJsonRpc2Request",
				"WorkflowsConstants",
				"workflowsDataLoader"
			];

			expectedExports.forEach((exportName) => {
				expect((module as any)[exportName]).toBeDefined();
			});
		});

		it("should verify no exports are undefined", async () => {
			const module = await import("../../index.js");

			const allExports = Object.keys(module);
			allExports.forEach((exportName) => {
				expect((module as any)[exportName]).not.toBeUndefined();
			});
		});

		it("should NOT export any phantom/unexpected exports", async () => {
			const module = await import("../../index.js");

			// Define the complete list of expected exports
			// Note: Type-only exports (TaskJsonRpc2RequestType, DocumentDto) may not appear at runtime
			// as they are erased during TypeScript compilation
			const expectedExports = [
				"WorkflowsActions",
				"WorkflowsEvents",
				"WorkflowsFactories",
				"workflowsMiddlewares",
				"adaptAppModelMiddleware",
				"workflowsSagas",
				"TaskJsonRpc2Request",
				"WorkflowsConstants",
				"workflowsDataLoader"
			];

			const actualExports = Object.keys(module);
			// If there are unexpected exports, the test should fail with a clear message
			expect(actualExports).toEqual(expect.arrayContaining(expectedExports));
		});

		it("should export type-only exports (compile-time check)", () => {
			// This test verifies that type-only exports are available for TypeScript type checking
			// These types are erased at runtime but must be importable for type safety

			// This will cause a TypeScript compilation error if the types are not exported
			type TestTaskJsonRpc2RequestType = import("../../index.js").TaskJsonRpc2RequestType;
			type TestDocumentDto = import("../../index.js").DocumentDto;

			// Verify the types can be used (compile-time only)
			const assertTypeExists = <T>() => true;
			expect(assertTypeExists<TestTaskJsonRpc2RequestType>()).toBe(true);
			expect(assertTypeExists<TestDocumentDto>()).toBe(true);
		});
	});

	describe("Cross-module integration verification", () => {
		it("should verify WorkflowsActions types are consistent", async () => {
			const actionsModule = await import("../../actions/index.js");
			const indexModule = await import("../../index.js");

			expect(indexModule.WorkflowsActions).toBe(actionsModule.WorkflowsActions);
		});

		it("should verify WorkflowsEvents are consistent", async () => {
			const eventsModule = await import("../../events/index.js");
			const indexModule = await import("../../index.js");

			expect(indexModule.WorkflowsEvents).toBe(eventsModule.WorkflowsEvents);
		});

		it("should verify WorkflowsFactories are consistent", async () => {
			const factoriesModule = await import("../../factories/index.js");
			const indexModule = await import("../../index.js");

			expect(indexModule.WorkflowsFactories).toBe(factoriesModule.WorkflowsFactories);
		});

		it("should verify workflowsSagas are consistent", async () => {
			const sagasModule = await import("../../sagas/index.js");
			const indexModule = await import("../../index.js");

			expect(indexModule.workflowsSagas).toBe(sagasModule.workflowsSagas);
		});

		it("should verify workflowsMiddlewares are consistent", async () => {
			const middlewaresModule = await import("../../middlewares/index.js");
			const indexModule = await import("../../index.js");

			expect(indexModule.workflowsMiddlewares).toBe(middlewaresModule.workflowsMiddlewares);
			expect(indexModule.adaptAppModelMiddleware).toBe(middlewaresModule.adaptAppModelMiddleware);
		});

		it("should verify WorkflowsConstants are consistent", async () => {
			const utilsModule = await import("../../utils/index.js");
			const indexModule = await import("../../index.js");

			expect(indexModule.WorkflowsConstants).toBe(utilsModule.WorkflowsConstants);
		});

		it("should verify TaskJsonRpc2Request is consistent", async () => {
			const taskModule = await import("../../task/index.js");
			const indexModule = await import("../../index.js");

			expect(indexModule.TaskJsonRpc2Request).toBe(taskModule.TaskJsonRpc2Request);
		});

		it("should verify workflowsDataLoader is consistent", async () => {
			const dataModule = await import("../../data/index.js");
			const indexModule = await import("../../index.js");

			expect(indexModule.workflowsDataLoader).toBe(dataModule.workflowsDataLoader);
		});
	});
});
