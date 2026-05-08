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

import {
	Activity,
	ActivityActions,
	ActivitySelectors
} from "@com.mgmtp.a12.client/client-core/lib/core/activity/index.js";

import {
	addFormKeyToPushAction,
	addFormKeyToReplacementAction
} from "../../../middlewares/internal/addFormKeyToDescriptor.js";
import { createActivity, getStore } from "../../testUtils/testUtils.js";
import { WorkflowsConstants } from "../../../utils/constants.js";

import PushPayload = ActivityActions.PushPayload;
import TASK_LIST_VIEW = WorkflowsConstants.TASK_LIST_VIEW;
import CancelRequestedPayload = ActivityActions.CancelRequestedPayload;

const DOCUMENT_MODEL = "Person_DM";
const DOCUMENT_MODEL_INSTANCE = `${DOCUMENT_MODEL}/1`;
const FORM_KEY = "Person_FM";
const FORM_KEY_2 = "someFormKeyThatShouldNotBeOverwritten";
const PROCESS_INSTANCE_ID = "someProcessInstanceId";
const BUSINESS_KEY = "someBusinessKey";

const a12wfMetadata = {
	Process: {
		ID: PROCESS_INSTANCE_ID,
		BusinessKey: BUSINESS_KEY
	},
	ProcessDefinition: {
		Key: "someProcessDefinitionKey",
		Name: "someProcessDefinitionName"
	},
	Task: {
		Assignee: "someAssignee",
		ID: PROCESS_INSTANCE_ID
	},
	TaskDefinition: {
		Key: "someTaskDefinitionKey",
		Name: "someTaskDefinitionName",
		FormKey: FORM_KEY
	}
};

const document = {
	id: DOCUMENT_MODEL_INSTANCE,
	modelId: DOCUMENT_MODEL,
	A12WF: a12wfMetadata
};

const INITIATING_ACTIVITY_ID = "someInitiatingActivityId";
const initiatingActivity = createInitiatingActivity(document);

const MIDDLEWARES = [addFormKeyToReplacementAction, addFormKeyToPushAction];
const INITIAL_STATE = { name: "getState" };

function createInitiatingActivity(document: object): Activity {
	return createActivity({
		id: INITIATING_ACTIVITY_ID,
		descriptor: {
			view: TASK_LIST_VIEW,
			model: DOCUMENT_MODEL
		},
		activityDataHolder: {
			data: {
				documents: [document]
			}
		}
	});
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
vi.spyOn(ActivitySelectors, "data").mockReturnValue((INITIATING_ACTIVITY_ID) => {
	if (initiatingActivity.dataHolders !== undefined) {
		return initiatingActivity.dataHolders[0].data;
	} else {
		return undefined;
	}
});

describe(`addFormKeyToDescriptor tests`, () => {
	const expectedActivityDescriptor = {
		view: TASK_LIST_VIEW,
		model: DOCUMENT_MODEL,
		instance: DOCUMENT_MODEL_INSTANCE,
		formKey: FORM_KEY
	};

	const expectedActivityDescriptor2 = {
		view: TASK_LIST_VIEW,
		model: DOCUMENT_MODEL,
		instance: DOCUMENT_MODEL_INSTANCE,
		formKey: FORM_KEY_2
	};

	const activityWithFormKey = createActivity({
		id: "someActivityId",
		descriptor: {
			view: TASK_LIST_VIEW,
			model: DOCUMENT_MODEL,
			instance: DOCUMENT_MODEL_INSTANCE,
			formKey: FORM_KEY_2
		},
		initiatingActivityId: INITIATING_ACTIVITY_ID
	});

	const activityWithoutFormKey = createActivity({
		id: "someActivityId",
		descriptor: { view: TASK_LIST_VIEW, model: DOCUMENT_MODEL, instance: DOCUMENT_MODEL_INSTANCE },
		initiatingActivityId: INITIATING_ACTIVITY_ID
	});

	describe(`formkey does not exist in descriptor`, () => {
		// eslint-disable-next-line @typescript-eslint/no-unused-vars
		vi.spyOn(ActivitySelectors, "activityById").mockReturnValue((OPEN_FORM_MODEL_ACTIVITY_ID) => {
			return activityWithoutFormKey;
		});

		it(`formkey should be added to descriptor for CANCEL_REQUESTED action`, () => {
			const cancelRequestedPayload: CancelRequestedPayload = {
				activityIds: ["someActivityIdToCancel"],
				replacementActivity: activityWithoutFormKey
			};
			const cancelRequestedAction = ActivityActions.cancelRequested(cancelRequestedPayload);

			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			store.dispatch(cancelRequestedAction);

			const actions = store.getActions();
			expect(actions).toHaveLength(1);
			expect(actions[0].type).toStrictEqual(cancelRequestedAction.type);
			expect(actions[0].payload.replacementActivity.descriptor).toStrictEqual(expectedActivityDescriptor);
		});
		it(`formkey should be added to descriptor for PUSH action`, () => {
			const pushPayload: PushPayload = { activity: activityWithoutFormKey };
			const pushPayloadAction = ActivityActions.push(pushPayload);

			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			store.dispatch(pushPayloadAction);

			const actions = store.getActions();
			expect(actions).toHaveLength(1);
			expect(actions[0].type).toStrictEqual(pushPayloadAction.type);
			expect(actions[0].payload.activity.descriptor).toStrictEqual(expectedActivityDescriptor);
		});
		it(`formkey should be found in nested a12wf metadata and added to descriptor`, () => {
			const documentWithNestedA12wf = {
				id: DOCUMENT_MODEL_INSTANCE,
				modelId: DOCUMENT_MODEL,
				person: {
					name: "John",
					A12WF: a12wfMetadata
				}
			};
			const initiatingActivityWithNestedA12wf = createInitiatingActivity(documentWithNestedA12wf);
			// eslint-disable-next-line @typescript-eslint/no-unused-vars
			vi.spyOn(ActivitySelectors, "data").mockReturnValue((INITIATING_ACTIVITY_ID) => {
				if (initiatingActivityWithNestedA12wf.dataHolders !== undefined) {
					return initiatingActivityWithNestedA12wf.dataHolders[0].data;
				} else {
					return undefined;
				}
			});

			const pushPayload: PushPayload = { activity: activityWithoutFormKey };
			const pushPayloadAction = ActivityActions.push(pushPayload);

			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			store.dispatch(pushPayloadAction);

			const actions = store.getActions();
			expect(actions).toHaveLength(1);
			expect(actions[0].type).toStrictEqual(pushPayloadAction.type);
			expect(actions[0].payload.activity.descriptor).toStrictEqual(expectedActivityDescriptor);
		});
	});

	describe(`formkey already exists in descriptor`, () => {
		// eslint-disable-next-line @typescript-eslint/no-unused-vars
		vi.spyOn(ActivitySelectors, "activityById").mockReturnValue((OPEN_FORM_MODEL_ACTIVITY_ID) => {
			return activityWithFormKey;
		});

		it(`existing formkey in descriptor should not be overwritten for CANCEL_REQUESTED action`, () => {
			const cancelRequestedPayload: CancelRequestedPayload = {
				activityIds: ["someActivityIdToCancel"],
				replacementActivity: activityWithFormKey
			};
			const cancelRequestedAction = ActivityActions.cancelRequested(cancelRequestedPayload);

			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			store.dispatch(cancelRequestedAction);

			const actions = store.getActions();
			expect(actions).toHaveLength(1);
			expect(actions[0].type).toStrictEqual(cancelRequestedAction.type);
			expect(actions[0].payload.replacementActivity.descriptor).toStrictEqual(expectedActivityDescriptor2);
		});
		it(`existing formkey in descriptor should not be overwritten for PUSH action`, () => {
			const pushPayload: PushPayload = { activity: activityWithFormKey };
			const pushPayloadAction = ActivityActions.push(pushPayload);

			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			store.dispatch(pushPayloadAction);

			const actions = store.getActions();
			expect(actions).toHaveLength(1);
			expect(actions[0].type).toStrictEqual(pushPayloadAction.type);
			expect(actions[0].payload.activity.descriptor).toStrictEqual(expectedActivityDescriptor2);
		});
	});
});
