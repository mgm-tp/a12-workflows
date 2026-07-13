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

import { ActivityActions, ActivitySelectors } from "@com.mgmtp.a12.client/client-core";

import {
	reloadTaskOverviewAfterAssignTaskMiddleware,
	reloadTaskOverviewAfterCompleteTaskMiddleware,
	reloadTaskOverviewAfterSendMessageMiddleware,
	reloadTaskOverviewAfterStartProcessMiddleware
} from "../../../middlewares/internal/reloadTaskOverviewMiddleware.js";
import { createActivity, getStore } from "../../testUtils/testUtils.js";
import { WorkflowsConstants } from "../../../utils/internal/constants.js";
import { WorkflowsActions } from "../../../actions/index.js";

const ACTIVITY_ID = "someOverviewActivityId";

const MIDDLEWARES = [
	reloadTaskOverviewAfterCompleteTaskMiddleware,
	// reloadTaskOverviewAfterAssignTaskMiddleware is disabled by purpose for a test case
	// reloadTaskOverviewAfterAssignTaskMiddleware,
	reloadTaskOverviewAfterSendMessageMiddleware,
	reloadTaskOverviewAfterStartProcessMiddleware
];
const INITIAL_STATE = { name: "getState" };

const appOverviewActivity = createActivity({
	id: ACTIVITY_ID,
	descriptor: {
		view: WorkflowsConstants.TASK_LIST_VIEW,
		instance: undefined
	},
	initiatingActivityId: "someInitialActivityId",
	activityDataHolder: {}
});

vi.spyOn(ActivitySelectors, "activities").mockReturnValue(() => {
	return { Activity: appOverviewActivity };
});

describe(`reloadTaskOverviewMiddleware tests`, () => {
	describe(`reloadTaskOverviewAfterCompleteTaskMiddleware tests`, () => {
		it(`should dispatch RELOAD action if Task has been completed`, () => {
			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			store.dispatch(
				WorkflowsActions.completeTask.done({
					params: {
						activityId: "someActivityId",
						document: {
							id: "someDocId",
							modelId: "someModelId"
						},
						docRef: "someDocRef"
					},
					result: {}
				})
			);

			const actions = store.getActions();
			expect(actions).toHaveLength(2);
			expect(actions[0].type).toStrictEqual(ActivityActions.reloadData.type);
			expect(actions[1].type).toStrictEqual(WorkflowsActions.completeTask.done.type);
		});
		it("should not dispatch RELOAD action if completing Task has failed", () => {
			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			store.dispatch(
				WorkflowsActions.completeTask.failed({
					params: {
						activityId: "someActivityId",
						document: {
							id: "someDocId",
							modelId: "someModelId"
						},
						docRef: "someDocRef"
					},
					error: new Error("someErrorMessage")
				})
			);

			const actions = store.getActions();
			expect(actions).toHaveLength(1);
			expect(actions[0].type).toStrictEqual(WorkflowsActions.completeTask.failed.type);
		});
	});
	describe(`reloadTaskOverviewAfterAssignTaskMiddleware tests`, () => {
		it(`should dispatch RELOAD action if Task has been assigned`, () => {
			const store = getStore([reloadTaskOverviewAfterAssignTaskMiddleware], INITIAL_STATE);

			store.dispatch(
				WorkflowsActions.assignTask.done({
					params: {
						docRef: "someDocRef",
						assignee: "someAssignee"
					},
					result: {}
				})
			);

			const actions = store.getActions();
			expect(actions).toHaveLength(2);
			expect(actions[0].type).toStrictEqual(ActivityActions.reloadData.type);
			expect(actions[1].type).toStrictEqual(WorkflowsActions.assignTask.done.type);
		});
		it("should not dispatch RELOAD action if Assign Task action has been invoked but reloadTaskOverviewAfterAssignTaskMiddleware is not included", () => {
			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			store.dispatch(
				WorkflowsActions.assignTask.done({
					params: {
						docRef: "someDocRef",
						assignee: "someAssignee"
					},
					result: {}
				})
			);

			const actions = store.getActions();
			expect(actions).toHaveLength(1);
			expect(actions[0].type).toStrictEqual(WorkflowsActions.assignTask.done.type);
		});
	});
	describe("reloadTaskOverviewAfterSendMessageMiddleware tests", () => {
		it(`should dispatch RELOAD action if message has been sent`, () => {
			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			store.dispatch(
				WorkflowsActions.sendMessage.done({
					params: {
						eventName: "someEventName",
						businessKey: "someBusinessKey",
						processInstanceId: "someProcessInstanceId",
						payload: {
							id: "someDocId",
							modelId: "someModelId"
						}
					},
					result: {}
				})
			);

			const actions = store.getActions();
			expect(actions).toHaveLength(2);
			expect(actions[0].type).toStrictEqual(ActivityActions.reloadData.type);
			expect(actions[1].type).toStrictEqual(WorkflowsActions.sendMessage.done.type);
		});
	});
	describe("reloadTaskOverviewAfterStartProcessMiddleware tests", () => {
		it(`should dispatch RELOAD action if process has been started`, () => {
			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			store.dispatch(
				WorkflowsActions.startProcess.done({
					params: {
						processDefinition: "someProcessDefinition",
						businessKey: "someBusinessKey",
						variables: {}
					},
					result: {}
				})
			);

			const actions = store.getActions();
			expect(actions).toHaveLength(2);
			expect(actions[0].type).toStrictEqual(ActivityActions.reloadData.type);
			expect(actions[1].type).toStrictEqual(WorkflowsActions.startProcess.done.type);
		});
	});
});
