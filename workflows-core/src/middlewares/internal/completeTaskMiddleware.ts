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

import { FormEngineActions } from "@com.mgmtp.a12.formengine/formengine-core/lib/client-extensions/index.js";
import { CddState } from "@com.mgmtp.a12.relationshipengine/relationshipengine-core/lib/internal/cdm/cdd/core/cddState.js";
import { Events } from "@com.mgmtp.a12.formengine/formengine-core/lib/back-end/store/index.js";
import { Activity, ActivitySelectors } from "@com.mgmtp.a12.client/client-core/lib/core/activity/index.js";
import { FormActivity } from "@com.mgmtp.a12.formengine/formengine-core/lib/client-extensions/index.js";
import { LoggerFactory } from "@com.mgmtp.a12.utils/utils-logging/index.js";

import { WorkflowsActions } from "../../actions/index.js";
import { WorkflowsEvents } from "../../events/index.js";

import DataHolder = Activity.DataHolder;

const logger = LoggerFactory.getLogger("completeTaskMiddleware");

type CddData = { cddState: CddState };

export const completeTaskMiddleware: Middleware = (api) => (next) => (action) => {
	if (
		FormEngineActions.event.match(action) &&
		Events.eventButton.match(action.payload.engineEvent) &&
		action.payload.engineEvent.payload.name === WorkflowsEvents.COMPLETE_TASK
	) {
		logger.info("Handling matched COMPLETE_TASK event");
		logger.log(`Triggered by action: ${action}`);
		const activity = ActivitySelectors.activityById(action.payload.activityId)(api.getState());
		if (activity?.descriptor?.instance === undefined) {
			logger.warn(`No activity descriptor or descriptor.instance found`);
			return next(action);
		}

		const defaultDataHolder = activity.dataHolders?.find((it) =>
			Activity.DataHolder.hasDescriptor(activity.descriptor)(it)
		) as DataHolder<FormActivity.Data.SingleDocumentData | CddData>;
		if (defaultDataHolder === undefined) {
			logger.error(`No default data holder found for activity:`, activity);
			return next(action);
		}
		logger.log(`Found default data holder:`, defaultDataHolder);

		const { data } = defaultDataHolder || {};

		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		let document: any = undefined;

		if (FormActivity.Data.SingleDocumentData.isInstance(data)) {
			document = data.document;
		} else {
			document = data?.cddState.cachedCdd?.cdd;
			if (document) {
				logger.info(`Given document is a CDD.`);
			}
		}

		if (document === undefined) {
			logger.error(`No document found in default data holder for activity:`, activity);
			return next(action);
		}
		const completeTaskAction = WorkflowsActions.completeTask.started({
			activityId: activity.id,
			document: document,
			docRef: activity.descriptor.instance
		});
		logger.log(`Dispatching completeTask.started action:`, completeTaskAction);
		api.dispatch(completeTaskAction);
		logger.log(`Dispatched completeTask.started action:`, completeTaskAction);
		logger.log("Finished handling COMPLETE_TASK event");
	}
	return next(action);
};
