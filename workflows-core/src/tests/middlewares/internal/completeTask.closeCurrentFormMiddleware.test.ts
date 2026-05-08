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

import { configureStore } from "redux-mock-store";

import {
	Activity,
	ActivityActions,
	ActivitySelectors
} from "@com.mgmtp.a12.client/client-core/lib/core/activity/index.js";

import { WorkflowsActions } from "../../../actions/index.js";
import { closeCurrentFormAfterTaskCompleteMiddleware } from "../../../middlewares/internal/completeTask.closeCurrentFormMiddleware.js";
import { createActivity } from "../../testUtils/testUtils.js";
import { WorkflowsConstants } from "../../../utils/constants.js";

import DataHolder = Activity.DataHolder;
import DataHolderDescriptor = Activity.DataHolderDescriptor;
import TASK_LIST_VIEW = WorkflowsConstants.TASK_LIST_VIEW;

const ACTIVITY_ID = "someActivityId";
const DOC_REF = "someDocRef";

const descriptor: DataHolderDescriptor = {
	view: TASK_LIST_VIEW,
	model: "someDocumentModel",
	instance: DOC_REF
};

const documentData = {
	id: "someId",
	modelId: "someDocumentModelId"
};

const dataHolder: DataHolder = {
	descriptor: descriptor,
	data: {
		document: documentData
	},
	datasourceActivityId: "someDatasourceActivityId",
	loadingState: "loading",
	savingState: "not_saved",
	dirty: false,
	busy: true,
	slices: {}
};

const taskFormActivity = createActivity({
	id: ACTIVITY_ID,
	descriptor: descriptor,
	dataHolders: [dataHolder],
	initiatingActivityId: "someInitialActivityId"
});

function getStore() {
	const middlewares = [closeCurrentFormAfterTaskCompleteMiddleware];
	const mockStore = configureStore(middlewares);
	configureStore();
	const initialState = { name: "getState" };
	return mockStore(initialState);
}

vi.spyOn(ActivitySelectors, "activities").mockReturnValue(() => {
	return { Activity: taskFormActivity };
});

describe("completeTask.closeCurrentFormMiddleware tests", () => {
	it("should successfully close current form after task completion", () => {
		// given
		const store = getStore();

		// when
		store.dispatch(
			WorkflowsActions.completeTask.done({
				params: {
					activityId: "someActivityId",
					document: documentData,
					docRef: DOC_REF
				},
				result: {}
			})
		);

		// then
		const actions = store.getActions();
		expect(actions).toHaveLength(2);
		expect(actions[0].type).toStrictEqual(ActivityActions.cancelRequested.type);
		expect(actions[1].type).toStrictEqual(WorkflowsActions.completeTask.done.type);
	});
});
