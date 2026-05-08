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

import { ActivityActions, ActivitySelectors } from "@com.mgmtp.a12.client/client-core/lib/core/activity/index.js";
import { LoggerFactory } from "@com.mgmtp.a12.utils/utils-logging/index.js";

import { WorkflowsActions } from "../../actions/index.js";
import { isTaskOverview } from "../../utils/internal/common/index.js";
import { WorkflowsConstants } from "../../utils/constants.js";

const adaptAppModel = WorkflowsActions.adaptAppModel;

const logger = LoggerFactory.getLogger("adaptAppModelMiddleware");

export const adaptAppModelMiddleware: (formComponentName: string) => Middleware =
	(formComponentName: string) => (api) => (next) => (action) => {
		if (ActivityActions.setData.match(action)) {
			logger.info(`Handling matched Activity/SET_DATA action.`);
			logger.log(`Triggered by action: ${action}`);
			const activity = ActivitySelectors.activityById(action.payload.activityId)(api.getState());

			if (activity === undefined) {
				throw Error("Could not find activity in redux store");
			}

			if (isTaskOverview(activity) && activity.descriptor.instance === undefined) {
				logger.info("Dispatching ADAPT_APP_MODEL action.");
				api.dispatch(
					adaptAppModel({
						activityId: action.payload.activityId,
						flowName: WorkflowsConstants.TASK_LIST_FLOW_NAME,
						formComponentName
					})
				);
			}
			logger.log("Finished handling Activity/SET_DATA action");
		}
		return next(action);
	};
