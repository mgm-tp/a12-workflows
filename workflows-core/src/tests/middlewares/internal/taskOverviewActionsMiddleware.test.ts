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

import { OverviewEngineActions } from "@com.mgmtp.a12.overviewengine/overviewengine-core/lib/main/client-extensions/index.js";
import { Events } from "@com.mgmtp.a12.overviewengine/overviewengine-core/lib/main/store/index.js";
import { Activity, ActivitySelectors } from "@com.mgmtp.a12.client/client-core/lib/core/activity/index.js";

import { createActivity, getStore } from "../../testUtils/testUtils.js";
import { WorkflowsEvents } from "../../../events/index.js";
import { WorkflowsActions } from "../../../actions/index.js";
import { WorkflowsConstants } from "../../../utils/constants.js";
import { taskOverviewActionsMiddleware } from "../../../middlewares/internal/taskOverviewActionsMiddleware.js";

import TASK_LIST_VIEW = WorkflowsConstants.TASK_LIST_VIEW;
import EventButtonClickedPayload = Events.EventButtonClickedPayload;
import RowButtonClickedPayload = Events.RowButtonClickedPayload;
import RowClickedPayload = Events.RowClickedPayload;

const ACTIVITY_ID = "someTaskListActivityId";
const MIDDLEWARES = [taskOverviewActionsMiddleware];
const INITIAL_STATE = { name: "getState" };

const documentModelName = "SomeDocumentModel";
const docRef = `${documentModelName}/1`;
const rowClickEventName = "EVENT/onRowClicked";
const rowButtonClickEventName = "EVENT/onRowButtonClicked";
const eventButtonClickEventName = "EVENT/onEventButtonClicked";
const processDefinition = "SomeProcessDefinition";
const someOtherEventName = "someOtherEvent";
const subtypeName = "SubtypeName";

const rowButtonClickPayload = (eventName: string): RowButtonClickedPayload => {
	return {
		rowActionModel: {
			event: eventName
		},
		documentId: docRef
	};
};

const eventButtonClickPayload = (eventName: string): EventButtonClickedPayload => {
	return {
		event: eventName,
		button: {
			destructive: false,
			primary: true,
			event: ""
		}
	};
};

const rowClickPayload = (): RowClickedPayload => {
	return {
		documentId: docRef
	};
};

function createOverviewEvent(
	type: string,
	payload: EventButtonClickedPayload | RowButtonClickedPayload | RowClickedPayload
) {
	return {
		activityId: ACTIVITY_ID,
		engineAction: {
			type: type,
			payload: payload
		}
	};
}

function createAppActivity(docRef: string | undefined, view: string = WorkflowsConstants.TASK_LIST_VIEW): Activity {
	return createActivity({
		id: ACTIVITY_ID,
		descriptor: {
			view: view,
			instance: docRef
		},
		initiatingActivityId: "someInitialActivityId",
		activityDataHolder: {}
	});
}

describe(`taskOverviewActionsMiddleware tests`, () => {
	describe(`CLAIM event tests`, () => {
		it(`should dispatch CLAIM action with correct doc ref`, () => {
			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			store.dispatch(
				OverviewEngineActions.event(
					createOverviewEvent(rowButtonClickEventName, rowButtonClickPayload(WorkflowsEvents.CLAIM))
				)
			);

			const actions = store.getActions();
			expect(actions).toHaveLength(2);
			const expectedClaimAction = WorkflowsActions.claimRequested({ docRef: docRef });
			expect(actions[0]).toStrictEqual(expectedClaimAction);
			expect(actions[1].type).toStrictEqual(`OverviewEngine/${rowButtonClickEventName}`);
		});
	});
	describe(`START_PROCESS event tests`, () => {
		it.each([
			["process definition", `${WorkflowsEvents.START_PROCESS}_${processDefinition}`],
			["process definition and subtype", `${WorkflowsEvents.START_PROCESS}_${processDefinition}_${subtypeName}`]
		])(
			`should dispatch START_PROCESS action with correct %s`,
			(printedValueForDocumentModelType: string, startProcessEventName: string) => {
				// given
				const store = getStore(MIDDLEWARES, INITIAL_STATE);

				// when
				store.dispatch(
					OverviewEngineActions.event(
						createOverviewEvent(eventButtonClickEventName, eventButtonClickPayload(startProcessEventName))
					)
				);

				// then
				const actions = store.getActions();
				expect(actions).toHaveLength(2);
				const actualBusinessKey = actions[0].payload.businessKey;
				const expectedStartProcessAction = WorkflowsActions.startProcess.started({
					processDefinition: processDefinition,
					businessKey: actualBusinessKey,
					variables: startProcessEventName.split("_")[2] ? { subtype: subtypeName } : undefined
				});
				expect(actions[0]).toStrictEqual(expectedStartProcessAction);
				expect(actions[1].type).toStrictEqual(`OverviewEngine/${eventButtonClickEventName}`);
			}
		);
		it(`should throw error if process definition is missing`, () => {
			// given
			const store = getStore(MIDDLEWARES, INITIAL_STATE);
			const startProcessEventName = `${WorkflowsEvents.START_PROCESS}`;

			// when
			store.dispatch(
				OverviewEngineActions.event(
					createOverviewEvent(eventButtonClickEventName, eventButtonClickPayload(startProcessEventName))
				)
			);

			// then
			const actions = store.getActions();
			expect(actions).toHaveLength(1);
			expect(actions[0].type).toStrictEqual(`OverviewEngine/${eventButtonClickEventName}`);
		});
	});
	describe(`Task list onRowClick event tests`, () => {
		it(`should dispatch form push action`, () => {
			// given
			vi.spyOn(ActivitySelectors, "activities").mockReturnValue(() => {
				return { Activity: createAppActivity(undefined) };
			});

			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			// when
			store.dispatch(OverviewEngineActions.event(createOverviewEvent(rowClickEventName, rowClickPayload())));

			// then
			const actions = store.getActions();
			expect(actions).toHaveLength(2);

			const expectedFormPushDescriptor = {
				view: TASK_LIST_VIEW,
				model: documentModelName,
				instance: docRef
			};

			expect(actions[0].payload.activity.descriptor).toStrictEqual(expectedFormPushDescriptor);
			expect(actions[0].type).toStrictEqual(`Activity/PUSH`);
			expect(actions[1].type).toStrictEqual(`OverviewEngine/${rowClickEventName}`);
		});
		it(`should dispatch form replace action`, () => {
			// given
			vi.spyOn(ActivitySelectors, "activities").mockReturnValue(() => {
				return {
					OverviewActivity: createAppActivity(undefined),
					FormActivity: createAppActivity("SomeOldDocRef/1")
				};
			});

			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			// when
			store.dispatch(OverviewEngineActions.event(createOverviewEvent(rowClickEventName, rowClickPayload())));

			// then
			const actions = store.getActions();
			expect(actions).toHaveLength(2);

			const expectedFormReplaceDescriptor = {
				view: TASK_LIST_VIEW,
				model: documentModelName,
				instance: docRef
			};

			expect(actions[0].payload.replacementActivity.descriptor).toStrictEqual(expectedFormReplaceDescriptor);
			expect(actions[0].type).toStrictEqual(`Activity/CANCEL_REQUESTED`);
			expect(actions[1].type).toStrictEqual(`OverviewEngine/${rowClickEventName}`);
		});
	});
	describe(`Other actions/events`, () => {
		it(`should not trigger any action for unmatched events`, () => {
			// given
			vi.spyOn(ActivitySelectors, "activities").mockReturnValue(() => {
				return {
					OverviewActivity: createAppActivity(undefined, "anotherView"),
					FormActivity: createAppActivity("SomeOldDocRef/1", "anotherView")
				};
			});

			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			// when
			const someOtherEventButtonEvent = createOverviewEvent(
				eventButtonClickEventName,
				eventButtonClickPayload(someOtherEventName)
			);
			const someOtherRowButtonEvent = createOverviewEvent(
				rowButtonClickEventName,
				rowButtonClickPayload(someOtherEventName)
			);
			const someOtherRowClickEvent = createOverviewEvent(rowClickEventName, rowClickPayload());
			store.dispatch(OverviewEngineActions.event(someOtherEventButtonEvent));
			store.dispatch(OverviewEngineActions.event(someOtherRowButtonEvent));
			store.dispatch(OverviewEngineActions.event(someOtherRowClickEvent));

			// then
			const actions = store.getActions();
			expect(actions).toHaveLength(3);
			expect(actions[0].payload).toStrictEqual(someOtherEventButtonEvent);
			expect(actions[1].payload).toStrictEqual(someOtherRowButtonEvent);
			expect(actions[2].payload).toStrictEqual(someOtherRowClickEvent);
		});
		it(`should not throw any error when other actions than OE actions are received`, () => {
			// given
			const someOtherAction = {
				type: "UAA/LOGOUT_REQUESTED",
				payload: undefined
			};
			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			// when
			store.dispatch(someOtherAction);

			// then
			const actions = store.getActions();
			expect(actions).toHaveLength(1);
			expect(actions[0].type).toStrictEqual(someOtherAction.type);
		});
	});
});
