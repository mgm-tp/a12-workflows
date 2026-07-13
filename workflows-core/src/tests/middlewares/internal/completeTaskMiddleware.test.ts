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

import type { Action } from "@com.mgmtp.a12.client/typescript-fsa-redux-5-compat";
import { FormEngineActions } from "@com.mgmtp.a12.formengine/formengine-core";
import { Activity, ActivitySelectors } from "@com.mgmtp.a12.client/client-core";

import { completeTaskMiddleware } from "../../../middlewares/internal/completeTaskMiddleware.js";
import { createActivity, createEventButtonPayload, getStore } from "../../testUtils/testUtils.js";
import { WorkflowsEvents } from "../../../events/index.js";
import { WorkflowsActions } from "../../../actions/index.js";

type DataHolderDescriptor = Activity.DataHolderDescriptor;
import DataHolder = Activity.DataHolder;

let taskFormActivity: Activity;

const ACTIVITY_ID = "someCompleteTaskActivityId";
const DOC_REF = "someDocRef";

const MIDDLEWARES = [completeTaskMiddleware];
const INITIAL_STATE = { name: "getState" };

const completeTaskFromEngineEventAction = createEventButtonPayload(ACTIVITY_ID, WorkflowsEvents.COMPLETE_TASK);

vi.spyOn(ActivitySelectors, "activityById").mockReturnValue(() => {
	return taskFormActivity;
});

const documentData = {
	id: "someId",
	modelId: "someModelId"
};

const descriptor: DataHolderDescriptor = {
	view: "someView",
	formModel: "someFormModel",
	taskId: "someTaskId",
	instance: DOC_REF
};

const expectedCompleteTaskAction = createCompleteTaskAction(documentData);

function createCompleteTaskAction(documentData: {
	id: string;
	modelId: string;
}): Action<WorkflowsActions.CompleteTaskPayload> {
	return WorkflowsActions.completeTask.started({
		activityId: ACTIVITY_ID,
		document: documentData,
		docRef: "someDocRef"
	});
}

describe("completeTaskMiddleware tests", () => {
	describe("regular DM", () => {
		it("should dispatch completeTaskAction", () => {
			// given
			const dataHolder: DataHolder = {
				descriptor: descriptor,
				data: {
					document: documentData
				},
				datasourceActivityId: "someDatasourceActivityId",
				loadingState: "loaded",
				savingState: "saved",
				dirty: false,
				busy: false,
				slices: {}
			};
			taskFormActivity = createActivity({
				id: ACTIVITY_ID,
				descriptor: descriptor,
				dataHolders: [dataHolder],
				initiatingActivityId: "someInitialActivityId"
			});
			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			// when
			store.dispatch(FormEngineActions.event(completeTaskFromEngineEventAction));

			// then
			const actions = store.getActions();
			expect(actions).toHaveLength(2);
			expect(actions[0]).toStrictEqual(expectedCompleteTaskAction);
			expect(actions[1].type).toStrictEqual(FormEngineActions.event.type);
		});
	});
	describe("CDM", () => {
		it("should dispatch completeTaskAction", () => {
			const cddData = {
				cachedCdd: {
					cdd: documentData
				}
			};
			const dataHolder: DataHolder = {
				descriptor: descriptor,
				data: {
					cddState: cddData
				},
				datasourceActivityId: "someDatasourceActivityId",
				loadingState: "loaded",
				savingState: "saved",
				dirty: false,
				busy: false,
				slices: {}
			};
			taskFormActivity = createActivity({
				id: ACTIVITY_ID,
				descriptor: descriptor,
				dataHolders: [dataHolder],
				initiatingActivityId: "someInitialActivityId"
			});
			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			// when
			store.dispatch(FormEngineActions.event(completeTaskFromEngineEventAction));

			// then
			const actions = store.getActions();
			expect(actions).toHaveLength(2);
			expect(actions[0]).toStrictEqual(expectedCompleteTaskAction);
			expect(actions[1].type).toStrictEqual(FormEngineActions.event.type);
		});
	});
});
