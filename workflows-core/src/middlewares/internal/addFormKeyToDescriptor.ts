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

import { Middleware, MiddlewareAPI } from "redux";
import { Action } from "typescript-fsa";

import {
	Activity,
	ActivityActions,
	ActivitySelectors
} from "@com.mgmtp.a12.client/client-core/lib/core/activity/index.js";
import { OverviewActivity } from "@com.mgmtp.a12.overviewengine/overviewengine-core/lib/main/client-extensions/index.js";
import { LoggerFactory } from "@com.mgmtp.a12.utils/utils-logging/index.js";

import { findA12wfObject, isTaskOverview } from "../../utils/internal/common/index.js";

const replacementActionLogger = LoggerFactory.getLogger("addFormKeyToReplacementAction");
const pushActionLogger = LoggerFactory.getLogger("addFormKeyToPushAction");

export const addFormKeyToReplacementAction: Middleware = (api) => (next) => (action) => {
	if (ActivityActions.cancelRequested.match(action)) {
		replacementActionLogger.info("Handling matched Activity/CANCEL_REQUESTED action");
		replacementActionLogger.log(`Triggered by action: ${action}`);
		const activity = action.payload.replacementActivity;

		const formKey = activity?.descriptor.formKey;
		if (formKey !== undefined) {
			replacementActionLogger.info(
				`Did not add form key to activity because it already contained form key ${formKey}.`
			);
			return next(action);
		}

		const modifiedActivity = addFormKeyToActivity(activity, api);

		if (modifiedActivity) {
			replacementActionLogger.log(`Added form key to activity: ${modifiedActivity}`);
			const result: Action<ActivityActions.CancelRequestedPayload> = {
				...action,
				payload: {
					...action.payload,
					replacementActivity: {
						...modifiedActivity
					}
				}
			};
			replacementActionLogger.log("Finished handling Activity/CANCEL_REQUESTED action");
			return next(result);
		}
	}
	return next(action);
};

export const addFormKeyToPushAction: Middleware = (api) => (next) => (action) => {
	if (ActivityActions.push.match(action)) {
		pushActionLogger.info("Handling matched Activity/PUSH action");
		pushActionLogger.log(`Triggered by action ${action}`);
		const activity = action.payload.activity;

		const formKey = activity?.descriptor.formKey;
		if (formKey !== undefined) {
			pushActionLogger.info(`Did not add form key to activity because it already contained form key ${formKey}.`);
			return next(action);
		}

		const modifiedActivity = addFormKeyToActivity(activity, api);

		if (modifiedActivity) {
			pushActionLogger.log(`Modified activity to add form key:`, modifiedActivity);
			const result: Action<ActivityActions.PushPayload> = {
				...action,
				payload: {
					...action.payload,
					activity: {
						...modifiedActivity
					}
				}
			};
			replacementActionLogger.log("Finished handling Activity/PUSH action");
			return next(result);
		}
	}
	return next(action);
};

const addFormKeyToActivity = (activity: Activity | undefined, api: MiddlewareAPI): Activity | undefined => {
	if (activity === undefined || !isTaskOverview(activity) || activity.descriptor.instance === undefined) {
		return;
	}

	const allDataHolders = activity.dataHolders || [];
	const activityDataHolder = allDataHolders.find(Activity.DataHolder.hasDescriptor(activity.descriptor));
	const otherDataHolders = allDataHolders.filter(Activity.DataHolder.hasNotDescriptor(activity.descriptor));

	if (activity.initiatingActivityId === undefined) {
		throw Error(`addFormKey middleware called from activity without parent activity.`);
	}

	const initiatingActivity = ActivitySelectors.activityById(activity.initiatingActivityId)(api.getState());

	if (initiatingActivity === undefined) {
		throw Error(`Initiating activity not found in state.`);
	}

	if (activityDataHolder === undefined) {
		throw Error(
			`No data holder could be found for activity descriptor ${JSON.stringify(activity.descriptor, null, 2)}.`
		);
	}

	const initiatingData = ActivitySelectors.data(activity.initiatingActivityId)(
		api.getState()
	) as OverviewActivity.Data.DocumentListData;

	const openedDocument = initiatingData.documents.find(
		(document) => document && document.id === activity.descriptor.instance
	);

	if (openedDocument === undefined) {
		throw Error(`Could not find necessary document in initiating activities' dataholders.`);
	}

	const A12WF = findA12wfObject(openedDocument) as { TaskDefinition: { FormKey: string; Key: string } };
	const formKey = A12WF.TaskDefinition.FormKey ?? A12WF.TaskDefinition.Key;

	return {
		...activity,
		descriptor: {
			...activity.descriptor,
			formKey: formKey
		},
		dataHolders: [
			...otherDataHolders,
			{
				...activityDataHolder,
				descriptor: {
					...activityDataHolder.descriptor,
					formKey: formKey
				}
			}
		]
	};
};
