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

import type { Dispatch, Middleware, MiddlewareAPI } from "redux";

import { LoggerFactory, type Logger } from "@com.mgmtp.a12.utils/utils-logging";
import { ActivityActions, ActivityMap, ActivitySelectors } from "@com.mgmtp.a12.client/client-core";

import { WorkflowsConstants } from "../../utils/internal/constants.js";
import { WorkflowsActions } from "../../actions/index.js";

const reloadAfterCompleteTaskLogger = LoggerFactory.getLogger("reloadTaskOverviewAfterCompleteTaskMiddleware");
const reloadAfterAssignTaskLogger = LoggerFactory.getLogger("reloadTaskOverviewAfterAssignTaskMiddleware");
const reloadAfterSendMessageLogger = LoggerFactory.getLogger("reloadTaskOverviewAfterSendMessageMiddleware");
const reloadAfterStartProcessLogger = LoggerFactory.getLogger("reloadTaskOverviewAfterStartProcessMiddleware");

export const reloadTaskOverviewAfterCompleteTaskMiddleware: Middleware = (api) => (next) => (action) => {
	if (WorkflowsActions.completeTask.done.match(action)) {
		reloadAfterCompleteTaskLogger.info("Handling matched Workflows/COMPLETE_TASK_DONE action");
		reloadAfterCompleteTaskLogger.log(`Triggered by action: ${action}`);
		reloadData(api, reloadAfterCompleteTaskLogger);
		reloadAfterCompleteTaskLogger.log("Finished handling Workflows/COMPLETE_TASK_DONE action");
	}
	return next(action);
};

export const reloadTaskOverviewAfterAssignTaskMiddleware: Middleware = (api) => (next) => (action) => {
	if (WorkflowsActions.assignTask.done.match(action)) {
		reloadAfterAssignTaskLogger.info("Handling matched Workflows/ASSIGN_TASK_DONE action");
		reloadAfterAssignTaskLogger.log(`Triggered by action: ${action}`);
		reloadData(api, reloadAfterAssignTaskLogger);
		reloadAfterAssignTaskLogger.log("Finished handling Workflows/ASSIGN_TASK_DONE action");
	}
	return next(action);
};

export const reloadTaskOverviewAfterSendMessageMiddleware: Middleware = (api) => (next) => (action) => {
	if (WorkflowsActions.sendMessage.done.match(action)) {
		reloadAfterSendMessageLogger.info("Handling matched Workflows/SEND_MESSAGE_DONE action");
		reloadAfterSendMessageLogger.log(`Triggered by action: ${action}`);
		reloadData(api, reloadAfterSendMessageLogger);
		reloadAfterSendMessageLogger.log("Finished handling Workflows/SEND_MESSAGE_DONE action");
	}
	return next(action);
};

export const reloadTaskOverviewAfterStartProcessMiddleware: Middleware = (api) => (next) => (action) => {
	if (WorkflowsActions.startProcess.done.match(action)) {
		reloadAfterStartProcessLogger.info("Handling matched Workflows/START_PROCESS action");
		reloadAfterStartProcessLogger.log(`Triggered by action: ${action}`);
		reloadData(api, reloadAfterStartProcessLogger);
		reloadAfterStartProcessLogger.log("Finished handling Workflows/START_PROCESS action");
	}
	return next(action);
};

function reloadData(api: MiddlewareAPI<Dispatch>, logger: Logger) {
	const matchingActivities = ActivityMap.toList(ActivitySelectors.activities()(api.getState()))
		.filter((activity) => WorkflowsConstants.TASK_LIST_VIEW === activity.descriptor.view)
		.filter((activity) => undefined === activity.descriptor.instance);

	logger.log(`Found the following candidate activities for current task overview:`, matchingActivities);

	if (matchingActivities.length !== 1) {
		logger.error(`Could not find unique activity representing the current task overview`);
	}
	const taskOverviewActivityId = matchingActivities[0].id;

	logger.log(`Requesting data reload of activity with id ${taskOverviewActivityId} to refresh task overview`);
	api.dispatch(ActivityActions.reloadData({ activityId: taskOverviewActivityId }));
	logger.log(`Requested data reload of activity with id ${taskOverviewActivityId} to refresh task overview`);
}
