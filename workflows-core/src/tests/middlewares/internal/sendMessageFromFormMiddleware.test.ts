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
import { Activity, ActivitySelectors, ModelSelectors } from "@com.mgmtp.a12.client/client-core";
import { FormEngineActions, type FormModel } from "@com.mgmtp.a12.formengine/formengine-core";

import { WorkflowsActions } from "../../../actions/index.js";
import { sendMessageFromFormMiddleware } from "../../../middlewares/internal/sendMessageFromFormMiddleware.js";
import { createActivity, createEventButtonPayload, getStore, readFile } from "../../testUtils/testUtils.js";
import { WorkflowsEvents } from "../../../events/index.js";

const MIDDLEWARES = [sendMessageFromFormMiddleware];
const INITIAL_STATE = { name: "getState" };

let taskFormActivity: Activity;

const sendMessageFormEngineEventAction = createEventButtonPayload(
	"someSendMessageActivityId",
	WorkflowsEvents.SEND_MESSAGE,
	[{ elementName: "footerBox1" }, { elementName: "Save" }]
);

const formModelMockString = readFile("../resources/models/formModels/Person_FM_runtimeRepresentation.json");
const formModelMockObject: FormModel = JSON.parse(formModelMockString);

vi.spyOn(ModelSelectors, "modelInScene").mockReturnValue(() => {
	return formModelMockObject;
});
vi.spyOn(ActivitySelectors, "activityById").mockReturnValue(() => {
	return taskFormActivity;
});

function createSendMessageData(businessKey: string, processInstanceId?: string): Partial<Activity.DataHolder> {
	return {
		data: {
			document: {
				A12WF: createA12wfMetadata(businessKey, processInstanceId)
			}
		}
	};
}

function createA12wfMetadata(businessKey: string, processInstanceId?: string) {
	return {
		Process: {
			ID: processInstanceId,
			BusinessKey: businessKey
		}
	};
}

function createTestActivityWithDataHolder(taskDetailsDataHolder: Partial<Activity.DataHolder>): Activity {
	return createActivity({
		id: "someTaskFormActivityId",
		descriptor: {
			view: "someView",
			formModel: "someFormModel",
			taskId: "someTaskId"
		},
		initiatingActivityId: "someInitialActivityId",
		activityDataHolder: taskDetailsDataHolder
	});
}

function createSendMessageAction(
	businessKey: string,
	processInstanceId?: string
): Action<WorkflowsActions.SendMessagePayload> {
	return WorkflowsActions.sendMessage.started({
		processInstanceId: processInstanceId,
		businessKey: businessKey,
		// value for eventName comes from Person_FM_runtimeRepresentation.json
		eventName: "someMessage"
	});
}

describe(`sendMessageFromFormMiddleware tests`, () => {
	it.each([
		["someBusinessKey", "someId"],
		["someBusinessKey", undefined]
	])(
		`should dispatch SEND_MESSAGE action with businessKey %s and processInstanceId %s`,
		(businessKey: string, processInstanceId?: string) => {
			const taskDetailsDataHolder = createSendMessageData(businessKey, processInstanceId);
			taskFormActivity = createTestActivityWithDataHolder(taskDetailsDataHolder);

			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			store.dispatch(FormEngineActions.event(sendMessageFormEngineEventAction));

			const actions = store.getActions();
			expect(actions).toHaveLength(2);
			const expectedSendMessageAction = createSendMessageAction(businessKey, processInstanceId);
			expect(actions[0]).toStrictEqual(expectedSendMessageAction);
			expect(actions[1].type).toStrictEqual(FormEngineActions.event.type);
		}
	);
	it(`should set processInstanceId and businessKey correctly in nested A12WF metadata`, () => {
		const processInstanceId = "someProcessInstanceId";
		const businessKey = "someBusinessKey";
		const taskDetailsDataHolder = {
			data: {
				document: {
					person: {
						name: "someName",
						A12WF: createA12wfMetadata(businessKey, processInstanceId)
					}
				}
			}
		};
		taskFormActivity = createTestActivityWithDataHolder(taskDetailsDataHolder);

		const store = getStore(MIDDLEWARES, INITIAL_STATE);

		store.dispatch(FormEngineActions.event(sendMessageFormEngineEventAction));

		const actions = store.getActions();
		expect(actions).toHaveLength(2);
		const expectedSendMessageAction = createSendMessageAction(businessKey, processInstanceId);
		expect(actions[0]).toStrictEqual(expectedSendMessageAction);
		expect(actions[1].type).toStrictEqual(FormEngineActions.event.type);
	});
});
