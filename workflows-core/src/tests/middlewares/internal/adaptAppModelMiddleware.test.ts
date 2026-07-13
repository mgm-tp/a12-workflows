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
import { legacy_createStore as createStore, applyMiddleware } from "redux";
import type { Middleware, UnknownAction } from "redux";

import { Activity, ActivityActions, ActivitySelectors } from "@com.mgmtp.a12.client/client-core";

import { adaptAppModelMiddleware } from "../../../middlewares/index.js";
import { createActivity } from "../../testUtils/testUtils.js";
import { WorkflowsActions } from "../../../actions/index.js";
import { WorkflowsConstants } from "../../../utils/internal/constants.js";

type DataHolderDescriptor = Activity.DataHolderDescriptor;
import DataHolder = Activity.DataHolder;
import TASK_LIST_VIEW = WorkflowsConstants.TASK_LIST_VIEW;

const ACTIVITY_ID = "someActivityId";

const descriptor: DataHolderDescriptor = {
	view: TASK_LIST_VIEW,
	model: "someDocumentModel"
};

const documentData = {
	id: "someId",
	modelId: "someDocumentModelId"
};

const dataHolder: DataHolder = {
	descriptor: descriptor,
	data: {
		documents: [documentData]
	},
	datasourceActivityId: "someDatasourceActivityId",
	loadingState: "loading",
	savingState: "not_saved",
	dirty: false,
	busy: true,
	slices: {}
};

const taskOverviewActivity = createActivity({
	id: ACTIVITY_ID,
	descriptor: descriptor,
	dataHolders: [dataHolder],
	initiatingActivityId: "someInitialActivityId"
});

function getStore() {
	const capturedActions: UnknownAction[] = [];
	const captureMiddleware: Middleware = () => (next) => (action) => {
		capturedActions.push(action as UnknownAction);
		return next(action);
	};
	const initialState = { name: "getState" };
	const store = createStore(
		(state = initialState) => state,
		initialState,
		applyMiddleware(adaptAppModelMiddleware("FormEngine"), captureMiddleware)
	);
	return { dispatch: store.dispatch, getActions: () => capturedActions };
}

vi.spyOn(ActivitySelectors, "activityById").mockReturnValue(() => {
	return taskOverviewActivity;
});

describe("adaptAppModelMiddleware tests", () => {
	it("should successfully dispatch ADAPT_APP_MODEL action", () => {
		// given
		const store = getStore();
		const action = ActivityActions.setData({
			activityId: "someActivityId",
			data: {
				documents: [
					{
						documentData
					}
				]
			}
		});

		// when
		store.dispatch(action);

		// then
		const actions = store.getActions();
		expect(actions).toHaveLength(2);
		expect(actions[0].type).toStrictEqual(WorkflowsActions.adaptAppModel.type);
		expect(actions[1].type).toStrictEqual(ActivityActions.setData.type);
	});
});
