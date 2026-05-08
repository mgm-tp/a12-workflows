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

import { AnyAction, Middleware, MiddlewareAPI, Dispatch } from "redux";
import { v4 as uuidv4 } from "uuid";

import { OverviewEngineActions } from "@com.mgmtp.a12.overviewengine/overviewengine-core/lib/main/client-extensions/index.js";
import { Events } from "@com.mgmtp.a12.overviewengine/overviewengine-core/lib/main/store/index.js";
import { LoggerFactory } from "@com.mgmtp.a12.utils/utils-logging/lib/factory.js";
import {
	Activity,
	ActivityActions,
	ActivityMap,
	ActivitySelectors
} from "@com.mgmtp.a12.client/client-core/lib/core/activity/index.js";

import { WorkflowsEvents } from "../../events/index.js";
import { WorkflowsActions } from "../../actions/index.js";
import { WorkflowsConstants } from "../../utils/constants.js";

const logger = LoggerFactory.getLogger("taskOverviewActionsMiddleware");

export const taskOverviewActionsMiddleware: Middleware = (api) => (next) => (action) => {
	switch (true) {
		case OverviewEngineActions.event.match(action) &&
			Events.onRowButtonClicked.match(action.payload.engineAction) &&
			WorkflowsEvents.CLAIM === action.payload.engineAction.payload.rowActionModel.event:
			claimTask(api, action);
			break;
		case OverviewEngineActions.event.match(action) &&
			Events.onEventButtonClicked.match(action.payload.engineAction) &&
			action.payload.engineAction.payload.event.startsWith(WorkflowsEvents.START_PROCESS):
			startProcess(api, action);
			break;
		case OverviewEngineActions.event.match(action) && Events.onRowClicked.match(action.payload.engineAction):
			openForm(api, action);
			break;
	}

	return next(action);
};

function claimTask(api: MiddlewareAPI<Dispatch<AnyAction>, any>, action: any) {
	logger.info(`Handling matched CLAIM event.`);
	logger.log(`Triggered by action: ${action}`);

	const claimAction = WorkflowsActions.claimRequested({
		docRef: action.payload.engineAction.payload.documentId
	});

	logger.log(`Dispatching claimRequested action:`, claimAction);
	api.dispatch(claimAction);
	logger.log(`Successfully dispatched claimRequested action:`, claimAction);
	logger.log(`Finished handling CLAIM event`);
}

function startProcess(api: MiddlewareAPI<Dispatch<AnyAction>, any>, action: any) {
	logger.info(`Handling matched START_PROCESS event.`);
	logger.log(`Triggered by action: ${action}`);

	const processPosition = 1;
	const subtypePosition = 2;
	const eventName = action.payload.engineAction.payload.event;
	const eventStringData = eventName.split("_");

	const processDefinitionName = eventStringData[processPosition];
	const subtypeName = eventStringData[subtypePosition];

	if (processDefinitionName === undefined) {
		logger.error(`Could not figure out which process to start based on event name ${eventName}`);
		return;
	}

	const startProcessAction = WorkflowsActions.startProcess.started({
		processDefinition: processDefinitionName,
		businessKey: uuidv4(),
		variables: subtypeName ? { subtype: subtypeName } : undefined
	});

	logger.log(`Dispatching startProcess action:`, startProcessAction);
	api.dispatch(startProcessAction);
	logger.log(`Successfully dispatched startProcess action:`, startProcessAction);
	logger.log(`Finished handling START_PROCESS event`);
}

function openForm(api: MiddlewareAPI<Dispatch<AnyAction>, any>, action: any) {
	const allActivities = ActivitySelectors.activities()(api.getState());

	const currentFormActivity: Activity | undefined = ActivityMap.toList(allActivities)
		.filter((it) => it.descriptor.view === WorkflowsConstants.TASK_LIST_VIEW)
		.find((it) => it.descriptor.instance !== undefined);

	const overviewActivityDescriptor: Activity.Descriptor | undefined = ActivityMap.toList(allActivities)
		.filter((activity) => activity.descriptor.view === WorkflowsConstants.TASK_LIST_VIEW)
		.find((activity) => activity.descriptor.instance === undefined)?.descriptor;

	if (!overviewActivityDescriptor) {
		logger.error("Could not find task overview activity descriptor");
		return;
	}

	const documentId = action.payload.engineAction.payload.documentId;

	const newFormPushAction = ActivityActions.create({
		initiatingActivityId: action.payload.activityId,
		activityDescriptor: {
			...overviewActivityDescriptor,
			model: documentId.split("/")[0],
			instance: documentId
		}
	});

	if (currentFormActivity) {
		const replaceFormActivityAction = ActivityActions.cancelRequested({
			activityIds: [currentFormActivity.id],
			replacementActivity: newFormPushAction.payload.activity
		});
		api.dispatch(replaceFormActivityAction);
	} else {
		api.dispatch(newFormPushAction);
	}
}
