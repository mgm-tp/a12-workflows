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

import * as matchers from "redux-saga-test-plan/matchers.js";
import { select } from "redux-saga-test-plan/matchers.js";
import { expectSaga } from "redux-saga-test-plan";

import { ModelSelectors } from "@com.mgmtp.a12.client/client-core/lib/core/model/index.js";
import { DocumentServiceFactory } from "@com.mgmtp.a12.kernel/kernel-md-facade/lib/main/js/index.js";
import { ApplicationActions } from "@com.mgmtp.a12.client/client-core/lib/core/application/index.js";
import { ActivityActions } from "@com.mgmtp.a12.client/client-core/lib/core/activity/index.js";

import { completeTaskSaga } from "../../sagas/internal/completeTaskSaga.js";
import { WorkflowsActions } from "../../actions/index.js";
import { dispatchJsonRpcRequests } from "../../utils/internal/requests/requestUtils.js";

import { testCanHandle, testNegativeResponse, testPositiveResponse } from "../testUtils/commonTests.js";
import { readFile } from "../testUtils/testUtils.js";

const action = WorkflowsActions.completeTask;
const saga = completeTaskSaga;

const payloadString = readFile("../resources/actionPayloads/jsonrpc_complete_task_payload.json");

const payload = JSON.parse(payloadString);

const documentModelService = new DocumentServiceFactory().getDocumentModelSerializer();

const personDocumentModel = readFile("../resources/models/documentModels/Person_DM.json");
// eslint-disable-next-line
vi.spyOn(ModelSelectors, "modelByName").mockImplementation((modelName: string) => {
	return () => {
		return documentModelService.deserialize(personDocumentModel);
	};
});

const startedAction = action.started(payload);

const failedAction = action.failed({
	params: payload,
	error: new Error("someError")
});

describe("Complete task saga ...", () => {
	describe("canHandle...", () => {
		testCanHandle(saga, [startedAction]);
	});

	describe("handle ...", () => {
		testPositiveResponse(
			saga,
			startedAction,
			action.done({
				params: startedAction.payload,
				result: {}
			}),
			[[select.selector(ModelSelectors.modelByName), personDocumentModel]],
			[{ type: ActivityActions.save.done.type }]
		);
		testNegativeResponse(saga, startedAction, failedAction, [], [{ type: ActivityActions.save.done.type }]);
	});

	describe("Set activity busy", () => {
		it("sets and unsets busy state for successful backend call", () => {
			return (
				expectSaga(saga.handle, startedAction)
					.provide([[matchers.call.fn(dispatchJsonRpcRequests), Promise.resolve()]])
					// This action will be dispatched on the occurring take effect.
					.dispatch({ type: ActivityActions.save.done.type })
					.put(ApplicationActions.setBusy(true))
					.put.actionType(WorkflowsActions.completeTask.done.type)
					.put(ApplicationActions.setBusy(false))
					.run()
			);
		});
		it("sets and unsets busy state for failed backend call", () => {
			return expectSaga(saga.handle, startedAction)
				.provide([[matchers.call.fn(dispatchJsonRpcRequests), Promise.reject()]])
				.dispatch({ type: ActivityActions.save.done.type })
				.put(ApplicationActions.setBusy(true))
				.put.actionType(WorkflowsActions.completeTask.failed.type)
				.put(ApplicationActions.setBusy(false))
				.run();
		});
	});
	describe("Dispatch SAVE_DONE or SAVE_FAILED action before dispatching COMPLETE_TASK action", () => {
		it("Dispatch COMPLETE_TASK_DONE action after SAVE_DONE action has been dispatched", () => {
			return expectSaga(saga.handle, startedAction)
				.provide([[matchers.call.fn(dispatchJsonRpcRequests), Promise.resolve()]])
				.dispatch({ type: ActivityActions.save.done.type })
				.put(ActivityActions.save.started({ activityId: startedAction.payload.activityId }))
				.put.actionType(WorkflowsActions.completeTask.done.type)
				.run();
		});
		it("Dispatch COMPLETE_TASK_FAILED action after SAVE_FAILED action has been dispatched", () => {
			return expectSaga(saga.handle, startedAction)
				.provide([[matchers.call.fn(dispatchJsonRpcRequests), Promise.resolve()]])
				.dispatch({ type: ActivityActions.save.failed.type })
				.put(ActivityActions.save.started({ activityId: startedAction.payload.activityId }))
				.put.actionType(WorkflowsActions.completeTask.failed.type)
				.run();
		});
	});
});
