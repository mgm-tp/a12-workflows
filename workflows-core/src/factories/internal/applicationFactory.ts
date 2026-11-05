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

import {
	setConfigured,
	combineFeatures,
	addPlatformSagas,
	type RequireFeatures,
	addAdditionalMiddlewares,
	type A12ApplicationConfig,
	type ApplicationWithConfiguredFeature
} from "@com.mgmtp.a12.client/client-core";

import { type WorkflowsMiddlewareInclusions, type WorkflowsMiddlewareOptions } from "../../middlewares/index.js";

import { WorkflowsFactories } from "./WorkflowsFactories.js";

// we use module augmentation to extend the A12ApplicationConfig type with more options
// for users, this is applied once they import anything from this file
declare module "@com.mgmtp.a12.client/client-core" {
	interface A12ApplicationConfig {
		readonly workflows?: {
			middlewareOptions: WorkflowsMiddlewareOptions;
			middlewareInclusions: WorkflowsMiddlewareInclusions;
		};
	}
}

/**
 * @experimental
 */
export type ApplicationWithWorkflowsConfig = RequireFeatures<A12ApplicationConfig, { workflows?: never }>;

/**
 * @experimental
 */
export const withWorkflowsPlatformSagas = <T extends ApplicationWithWorkflowsConfig>(cfg: T) =>
	addPlatformSagas(...WorkflowsFactories.createApplicationSagas())(cfg);

/**
 * @experimental
 */
export const withWorkflowsMiddlewares = <T extends ApplicationWithWorkflowsConfig>(cfg: T) =>
	addAdditionalMiddlewares(
		...WorkflowsFactories.createMiddlewaresFromOptions(
			cfg?.workflows?.middlewareOptions,
			cfg?.workflows?.middlewareInclusions
		)
	)(cfg);

/**
 * @experimental
 */
export const withWorkflows = <T extends ApplicationWithWorkflowsConfig>(
	cfg: T
): ApplicationWithConfiguredFeature<T, "workflows"> =>
	setConfigured<T, "workflows">("workflows")(
		combineFeatures(withWorkflowsPlatformSagas, withWorkflowsMiddlewares)(cfg)
	);
