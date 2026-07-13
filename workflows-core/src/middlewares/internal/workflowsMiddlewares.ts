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

import { type Middleware } from "redux";

import { adaptAppModelMiddleware } from "./adaptAppModelMiddleware.js";
import { completeTaskMiddleware } from "./completeTaskMiddleware.js";
import { addFormKeyToPushAction, addFormKeyToReplacementAction } from "./addFormKeyToDescriptor.js";
import { claimMiddleware } from "./claimMiddleware.js";
import {
	reloadTaskOverviewAfterAssignTaskMiddleware,
	reloadTaskOverviewAfterCompleteTaskMiddleware,
	reloadTaskOverviewAfterSendMessageMiddleware,
	reloadTaskOverviewAfterStartProcessMiddleware
} from "./reloadTaskOverviewMiddleware.js";
import { sendMessageFromFormMiddleware } from "./sendMessageFromFormMiddleware.js";
import { setInternalWorkflowsStateOnTaskFormMiddleware } from "./setInternalWorkflowsStateOnTaskFormMiddleware.js";
import { closeCurrentFormAfterTaskCompleteMiddleware } from "./completeTask.closeCurrentFormMiddleware.js";
import { taskOverviewActionsMiddleware } from "./taskOverviewActionsMiddleware.js";

export type WorkflowsMiddlewareOptions = {
	formComponentName: string;
};

export type WorkflowsMiddlewareInclusions = {
	includeAdaptAppModelMiddleware: boolean;
	includeAddFormKeyToPushAction: boolean;
	includeAddFormKeyToReplacementAction: boolean;
	includeReloadTaskOverviewAfterCompleteTaskMiddleware: boolean;
	includeReloadTaskOverviewAfterAssignTaskMiddleware: boolean;
	includeReloadTaskOverviewAfterSendMessageMiddleware: boolean;
	includeReloadTaskOverviewAfterStartProcessMiddleware: boolean;
};

const defaultOptions: WorkflowsMiddlewareOptions = { formComponentName: "FormEngine" };
const defaultMiddlewareOptions: WorkflowsMiddlewareInclusions = {
	includeAdaptAppModelMiddleware: true,
	includeAddFormKeyToPushAction: true,
	includeAddFormKeyToReplacementAction: true,
	includeReloadTaskOverviewAfterCompleteTaskMiddleware: true,
	includeReloadTaskOverviewAfterAssignTaskMiddleware: true,
	includeReloadTaskOverviewAfterSendMessageMiddleware: true,
	includeReloadTaskOverviewAfterStartProcessMiddleware: true
};

export function workflowsMiddlewares(
	options: WorkflowsMiddlewareOptions = defaultOptions,
	middlewareOptions: WorkflowsMiddlewareInclusions = defaultMiddlewareOptions
): Middleware[] {
	const middlewares: Middleware[] = [
		completeTaskMiddleware,
		closeCurrentFormAfterTaskCompleteMiddleware,
		claimMiddleware,
		sendMessageFromFormMiddleware,
		setInternalWorkflowsStateOnTaskFormMiddleware,
		taskOverviewActionsMiddleware
	];
	if (middlewareOptions.includeAdaptAppModelMiddleware) {
		middlewares.push(adaptAppModelMiddleware(options.formComponentName));
	}
	if (middlewareOptions.includeAddFormKeyToPushAction) {
		middlewares.push(addFormKeyToPushAction);
	}
	if (middlewareOptions.includeAddFormKeyToReplacementAction) {
		middlewares.push(addFormKeyToReplacementAction);
	}
	if (middlewareOptions.includeReloadTaskOverviewAfterCompleteTaskMiddleware) {
		middlewares.push(reloadTaskOverviewAfterCompleteTaskMiddleware);
	}
	if (middlewareOptions.includeReloadTaskOverviewAfterAssignTaskMiddleware) {
		middlewares.push(reloadTaskOverviewAfterAssignTaskMiddleware);
	}
	if (middlewareOptions.includeReloadTaskOverviewAfterSendMessageMiddleware) {
		middlewares.push(reloadTaskOverviewAfterSendMessageMiddleware);
	}
	if (middlewareOptions.includeReloadTaskOverviewAfterStartProcessMiddleware) {
		middlewares.push(reloadTaskOverviewAfterStartProcessMiddleware);
	}
	return middlewares;
}
