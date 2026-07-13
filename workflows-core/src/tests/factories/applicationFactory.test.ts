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

import { vi } from "vitest";
import type { Middleware } from "redux";

import { combineFeatures } from "@com.mgmtp.a12.client/client-core";

import {
	withWorkflows,
	withWorkflowsPlatformSagas,
	withWorkflowsMiddlewares,
	type ApplicationWithWorkflowsConfig
} from "../../factories/internal/applicationFactory.js";
import { WorkflowsFactories } from "../../factories/index.js";
import type { WorkflowsMiddlewareOptions, WorkflowsMiddlewareInclusions } from "../../middlewares/index.js";

// Helper type for testing - we use this to access the internal structure of the config
// eslint-disable-next-line @typescript-eslint/no-explicit-any
type TestConfig = Record<string, any>;

describe("applicationFactory", () => {
	// Create a base config for testing with the minimal required structure
	const createBaseConfig = (): ApplicationWithWorkflowsConfig => {
		return {
			config: {}
		} as ApplicationWithWorkflowsConfig;
	};

	describe("withWorkflowsPlatformSagas", () => {
		it("should add workflow sagas to the configuration", () => {
			// given
			const baseConfig = createBaseConfig();
			const expectedSagas = WorkflowsFactories.createApplicationSagas();

			// when
			const result = withWorkflowsPlatformSagas(baseConfig) as TestConfig;

			// then
			expect(result.config.overridePlatformSagas).toBeDefined();
			expect(result.config.overridePlatformSagas.length).toBe(expectedSagas.length);
			expectedSagas.forEach((saga) => {
				expect(result.config.overridePlatformSagas).toContain(saga);
			});
		});

		it("should preserve existing configuration properties", () => {
			// given
			const existingSaga = { canHandle: vi.fn(), handle: vi.fn() };
			const baseConfig = {
				config: {
					overridePlatformSagas: [existingSaga],
					additionalMiddlewares: []
				}
			} as ApplicationWithWorkflowsConfig;

			// when
			const result = withWorkflowsPlatformSagas(baseConfig) as TestConfig;

			// then
			expect(result.config.overridePlatformSagas).toContain(existingSaga);
			expect(result.config.additionalMiddlewares).toEqual([]);
		});

		it("should not throw errors when given a valid config", () => {
			// given
			const baseConfig = createBaseConfig();

			// when/then
			expect(() => withWorkflowsPlatformSagas(baseConfig)).not.toThrow();
		});
	});

	describe("withWorkflowsMiddlewares", () => {
		it("should add workflow middlewares to the configuration", () => {
			// given
			const baseConfig = createBaseConfig();
			const expectedMiddlewares = WorkflowsFactories.createMiddlewares();

			// when
			const result = withWorkflowsMiddlewares(baseConfig) as TestConfig;

			// then
			expect(result.config.additionalMiddlewares).toBeDefined();
			expect(result.config.additionalMiddlewares.length).toBe(expectedMiddlewares.length);

			const expectedFilteredMiddlewares = expectedMiddlewares.filter((mw) => mw.name !== "");
			const actualFilteredMiddlewares = result.config.additionalMiddlewares.filter((mw: Middleware) => mw.name !== "");

			expect(actualFilteredMiddlewares).toEqual(expect.arrayContaining(expectedFilteredMiddlewares));
		});

		it("should preserve existing configuration properties", () => {
			// given
			const existingMiddleware = vi.fn();
			const baseConfig = {
				config: {
					overridePlatformSagas: [],
					additionalMiddlewares: [existingMiddleware]
				}
			} as ApplicationWithWorkflowsConfig;

			// when
			const result = withWorkflowsMiddlewares(baseConfig) as TestConfig;

			// then
			expect(result.config.additionalMiddlewares).toContain(existingMiddleware);
			expect(result.config.overridePlatformSagas).toEqual([]);
		});

		it("should not throw errors when given a valid config", () => {
			// given
			const baseConfig = createBaseConfig();

			// when/then
			expect(() => withWorkflowsMiddlewares(baseConfig)).not.toThrow();
		});

		describe("with custom options", () => {
			it("should pass middlewareOptions to middleware factory", () => {
				// given
				const customOptions: WorkflowsMiddlewareOptions = {
					formComponentName: "CustomFormEngine"
				};
				const configWithOptions = {
					config: {},
					workflows: {
						middlewareOptions: customOptions,
						middlewareInclusions: undefined as unknown as WorkflowsMiddlewareInclusions
					}
				} as ApplicationWithWorkflowsConfig;

				const createMiddlewaresSpy = vi.spyOn(WorkflowsFactories, "createMiddlewaresFromOptions");

				// when
				withWorkflowsMiddlewares(configWithOptions);

				// then
				expect(createMiddlewaresSpy).toHaveBeenCalledWith(customOptions, undefined);
				createMiddlewaresSpy.mockRestore();
			});

			it("should pass middlewareInclusions to middleware factory", () => {
				// given
				const customInclusions: WorkflowsMiddlewareInclusions = {
					includeAdaptAppModelMiddleware: false,
					includeAddFormKeyToPushAction: false,
					includeAddFormKeyToReplacementAction: true,
					includeReloadTaskOverviewAfterCompleteTaskMiddleware: false,
					includeReloadTaskOverviewAfterAssignTaskMiddleware: true,
					includeReloadTaskOverviewAfterSendMessageMiddleware: false,
					includeReloadTaskOverviewAfterStartProcessMiddleware: true
				};
				const configWithInclusions = {
					config: {},
					workflows: {
						middlewareOptions: undefined as unknown as WorkflowsMiddlewareOptions,
						middlewareInclusions: customInclusions
					}
				} as ApplicationWithWorkflowsConfig;

				const createMiddlewaresSpy = vi.spyOn(WorkflowsFactories, "createMiddlewaresFromOptions");

				// when
				withWorkflowsMiddlewares(configWithInclusions);

				// then
				expect(createMiddlewaresSpy).toHaveBeenCalledWith(undefined, customInclusions);
				createMiddlewaresSpy.mockRestore();
			});

			it("should pass both middlewareOptions and middlewareInclusions to middleware factory", () => {
				// given
				const customOptions: WorkflowsMiddlewareOptions = {
					formComponentName: "CustomFormEngine"
				};
				const customInclusions: WorkflowsMiddlewareInclusions = {
					includeAdaptAppModelMiddleware: true,
					includeAddFormKeyToPushAction: false,
					includeAddFormKeyToReplacementAction: false,
					includeReloadTaskOverviewAfterCompleteTaskMiddleware: true,
					includeReloadTaskOverviewAfterAssignTaskMiddleware: false,
					includeReloadTaskOverviewAfterSendMessageMiddleware: false,
					includeReloadTaskOverviewAfterStartProcessMiddleware: false
				};
				const configWithBothOptions = {
					config: {},
					workflows: {
						middlewareOptions: customOptions,
						middlewareInclusions: customInclusions
					}
				} as ApplicationWithWorkflowsConfig;

				const createMiddlewaresSpy = vi.spyOn(WorkflowsFactories, "createMiddlewaresFromOptions");

				// when
				withWorkflowsMiddlewares(configWithBothOptions);

				// then
				expect(createMiddlewaresSpy).toHaveBeenCalledWith(customOptions, customInclusions);
				createMiddlewaresSpy.mockRestore();
			});

			it("should create fewer middlewares when inclusions are set to false", () => {
				// given
				const allInclusionsFalse: WorkflowsMiddlewareInclusions = {
					includeAdaptAppModelMiddleware: false,
					includeAddFormKeyToPushAction: false,
					includeAddFormKeyToReplacementAction: false,
					includeReloadTaskOverviewAfterCompleteTaskMiddleware: false,
					includeReloadTaskOverviewAfterAssignTaskMiddleware: false,
					includeReloadTaskOverviewAfterSendMessageMiddleware: false,
					includeReloadTaskOverviewAfterStartProcessMiddleware: false
				};
				const configWithAllFalse = {
					config: {},
					workflows: {
						middlewareOptions: undefined as unknown as WorkflowsMiddlewareOptions,
						middlewareInclusions: allInclusionsFalse
					}
				} as ApplicationWithWorkflowsConfig;

				const baseConfig = createBaseConfig();

				// when
				const resultWithDefaults = withWorkflowsMiddlewares(baseConfig) as TestConfig;
				const resultWithAllFalse = withWorkflowsMiddlewares(configWithAllFalse) as TestConfig;

				// then
				expect(resultWithAllFalse.config.additionalMiddlewares.length).toBeLessThan(
					resultWithDefaults.config.additionalMiddlewares.length
				);
			});
		});
	});

	describe("withWorkflows", () => {
		it("should add both platform sagas and middlewares to the configuration", () => {
			// given
			const baseConfig = createBaseConfig();
			const expectedSagas = WorkflowsFactories.createApplicationSagas();
			const expectedMiddlewares = WorkflowsFactories.createMiddlewares();

			// when
			const result = withWorkflows(baseConfig) as TestConfig;

			// then
			expect(result.config.overridePlatformSagas.length).toBe(expectedSagas.length);
			expect(result.config.additionalMiddlewares.length).toBe(expectedMiddlewares.length);
			expectedSagas.forEach((saga) => {
				expect(result.config.overridePlatformSagas).toContain(saga);
			});

			const expectedFilteredMiddlewares = expectedMiddlewares.filter((mw) => mw.name !== "");
			const actualFilteredMiddlewares = result.config.additionalMiddlewares.filter((mw: Middleware) => mw.name !== "");
			expect(actualFilteredMiddlewares).toEqual(expect.arrayContaining(expectedFilteredMiddlewares));
		});

		it("should mark the wf feature as configured", () => {
			// given
			const baseConfig = createBaseConfig();

			// when
			const result = withWorkflows(baseConfig) as TestConfig;

			// then
			// The wf property should be set (indicating the feature is configured)
			expect(result.configured).toHaveProperty("workflows");
		});

		it("should preserve existing configuration properties", () => {
			// given
			const existingSaga = { canHandle: vi.fn(), handle: vi.fn() };
			const existingMiddleware = vi.fn();
			const baseConfig = {
				config: {
					overridePlatformSagas: [existingSaga],
					additionalMiddlewares: [existingMiddleware]
				}
			} as ApplicationWithWorkflowsConfig;

			// when
			const result = withWorkflows(baseConfig) as TestConfig;

			// then
			expect(result.config.overridePlatformSagas).toContain(existingSaga);
			expect(result.config.additionalMiddlewares).toContain(existingMiddleware);
		});

		describe("with custom options", () => {
			it("should pass options through to middleware factory", () => {
				// given
				const customOptions: WorkflowsMiddlewareOptions = {
					formComponentName: "CustomFormEngine"
				};
				const customInclusions: WorkflowsMiddlewareInclusions = {
					includeAdaptAppModelMiddleware: true,
					includeAddFormKeyToPushAction: false,
					includeAddFormKeyToReplacementAction: false,
					includeReloadTaskOverviewAfterCompleteTaskMiddleware: false,
					includeReloadTaskOverviewAfterAssignTaskMiddleware: false,
					includeReloadTaskOverviewAfterSendMessageMiddleware: false,
					includeReloadTaskOverviewAfterStartProcessMiddleware: false
				};
				const configWithOptions = {
					config: {},
					workflows: {
						middlewareOptions: customOptions,
						middlewareInclusions: customInclusions
					}
				} as ApplicationWithWorkflowsConfig;

				const createMiddlewaresSpy = vi.spyOn(WorkflowsFactories, "createMiddlewaresFromOptions");

				// when
				withWorkflows(configWithOptions);

				// then
				expect(createMiddlewaresSpy).toHaveBeenCalledWith(customOptions, customInclusions);
				createMiddlewaresSpy.mockRestore();
			});

			it("should work correctly with partial inclusions", () => {
				// given
				const partialInclusions: WorkflowsMiddlewareInclusions = {
					includeAdaptAppModelMiddleware: true,
					includeAddFormKeyToPushAction: true,
					includeAddFormKeyToReplacementAction: true,
					includeReloadTaskOverviewAfterCompleteTaskMiddleware: false,
					includeReloadTaskOverviewAfterAssignTaskMiddleware: false,
					includeReloadTaskOverviewAfterSendMessageMiddleware: false,
					includeReloadTaskOverviewAfterStartProcessMiddleware: false
				};
				const configWithPartialInclusions = {
					config: {},
					workflows: {
						middlewareOptions: { formComponentName: "FormEngine" },
						middlewareInclusions: partialInclusions
					}
				} as ApplicationWithWorkflowsConfig;

				// when
				const result = withWorkflows(configWithPartialInclusions) as TestConfig;

				// then
				expect(result.config.additionalMiddlewares).toBeDefined();
				expect(result.configured).toHaveProperty("workflows");
			});
		});
	});

	describe("Feature Flag / Type Safety", () => {
		it("should return the correct type with workflows marked as configured", () => {
			// given
			const baseConfig = createBaseConfig();

			// when
			const result = withWorkflows(baseConfig);

			// then
			expect("workflows" in result.configured).toBe(true);
		});
	});

	describe("Composability with Other Features", () => {
		it("should be composable using combineFeatures", () => {
			// given
			const baseConfig = createBaseConfig();
			const expectedSagas = WorkflowsFactories.createApplicationSagas();
			const expectedMiddlewares = WorkflowsFactories.createMiddlewares();

			// when
			const combined = combineFeatures(withWorkflowsPlatformSagas, withWorkflowsMiddlewares);
			const result = combined(baseConfig) as TestConfig;

			// then
			expect(result.config.overridePlatformSagas.length).toBe(expectedSagas.length);
			expect(result.config.additionalMiddlewares.length).toBe(expectedMiddlewares.length);
		});

		it("should work with withWorkflows in a composition chain", () => {
			// given
			const baseConfig = createBaseConfig();
			const customFeature = <T extends ApplicationWithWorkflowsConfig>(cfg: T) => ({
				...cfg,
				configured: {
					customProperty: "test"
				}
			});

			// when
			const combined = combineFeatures(customFeature, withWorkflows);
			const result = combined(baseConfig) as TestConfig;

			// then
			expect(result.configured).toHaveProperty("customProperty", "test");
			expect(result.configured).toHaveProperty("workflows");
		});

		it("should produce the same result regardless of individual function order when using combineFeatures", () => {
			// given
			const firstBaseConfig = createBaseConfig();
			const secondBaseConfig = createBaseConfig();

			// when
			const resultSagasFirst = combineFeatures(
				withWorkflowsPlatformSagas,
				withWorkflowsMiddlewares
			)(firstBaseConfig) as TestConfig;

			const resultMiddlewaresFirst = combineFeatures(
				withWorkflowsMiddlewares,
				withWorkflowsPlatformSagas
			)(secondBaseConfig) as TestConfig;

			// then
			expect(resultSagasFirst.config.overridePlatformSagas.length).toBe(
				resultMiddlewaresFirst.config.overridePlatformSagas.length
			);
			expect(resultSagasFirst.config.additionalMiddlewares.length).toBe(
				resultMiddlewaresFirst.config.additionalMiddlewares.length
			);
		});
	});
});
