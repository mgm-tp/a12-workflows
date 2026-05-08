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

import { Middleware } from "redux";

import {
	ActivityActions,
	ActivityMap,
	ActivitySelectors
} from "@com.mgmtp.a12.client/client-core/lib/core/activity/index.js";
import { LoggerFactory } from "@com.mgmtp.a12.utils/utils-logging/index.js";

import { WorkflowsActions } from "../../actions/index.js";
import { WorkflowsConstants } from "../../utils/constants.js";

const logger = LoggerFactory.getLogger("completeTask.closeCurrentFormMiddleware");

export const closeCurrentFormAfterTaskCompleteMiddleware: Middleware = (api) => (next) => (action) => {
	if (WorkflowsActions.completeTask.done.match(action)) {
		logger.info(`Handling matched Workflows/COMPLETE_TASK action.`);
		logger.log(`Triggered by action: `, action);

		const taskFormDescriptor = {
			view: WorkflowsConstants.TASK_LIST_VIEW,
			instance: action.payload.params.docRef
		};
		logger.log(`Trying to find activities for task forms by descriptor`, taskFormDescriptor);
		// We only need to match the descriptor partially, ActivitySelectors.activityByDescriptor does deepEqual
		const taskFormActivities = ActivityMap.toList(ActivitySelectors.activities()(api.getState()))
			.filter((activity) => activity.descriptor.view == taskFormDescriptor.view)
			.filter((activity) => activity.descriptor.instance == taskFormDescriptor.instance);

		logger.log(`Found the following task form activities:`, taskFormActivities);

		const taskFormActivityIds = taskFormActivities.map((activity) => activity.id);

		logger.log(`Requesting cancelling of activities with ids ${taskFormActivityIds} to close task form`);
		api.dispatch(ActivityActions.cancelRequested({ activityIds: taskFormActivityIds }));
		logger.log(`Requested cancelling of activity with id ${taskFormActivityIds} to close task form`);
		logger.log("Finished handling Workflows/COMPLETE_TASK action");
		return next(action);
	}
	return next(action);
};
