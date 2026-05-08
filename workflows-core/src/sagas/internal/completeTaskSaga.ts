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

import { Action, AnyAction } from "typescript-fsa";
import { SagaIterator } from "redux-saga";
import { call, put, select, take } from "typed-redux-saga";

import { Activity, ActivityActions } from "@com.mgmtp.a12.client/client-core/lib/core/activity/index.js";
import { ApplicationActions, ApplicationSaga } from "@com.mgmtp.a12.client/client-core/lib/core/application/index.js";
import { LoggerFactory } from "@com.mgmtp.a12.utils/utils-logging/index.js";
import { Model, ModelSelectors } from "@com.mgmtp.a12.client/client-core/lib/core/model/index.js";

import { WorkflowsActions } from "../../actions/index.js";
import { RequestBuilder } from "../../utils/index.js";
import { dispatchJsonRpcRequests } from "../../utils/internal/requests/requestUtils.js";
import { TaskJsonRpc2Request } from "../../task/index.js";

const logger = LoggerFactory.getLogger("completeTaskSaga");

export const completeTaskSaga: ApplicationSaga.Descriptor = {
	canHandle: (_: Activity.Descriptor, action: AnyAction) => {
		return WorkflowsActions.completeTask.started.match(action);
	},
	handle: function* (action: Action<WorkflowsActions.CompleteTaskPayload>): SagaIterator<void> {
		logger.info("Handling matched Workflows/COMPLETE_TASK_STARTED action");
		logger.log(`Triggered by action: ${action}`);
		const { docRef, document } = action.payload;
		const documentModel = yield* select(ModelSelectors.modelByName(document.modelId, Model.isDocumentModel));

		if (documentModel === undefined) {
			return;
		}

		const completeTaskRequest = RequestBuilder.completeTask("completeTask", docRef);

		try {
			yield* put(ApplicationActions.setBusy(true));
			yield* put(ActivityActions.save.started({ activityId: action.payload.activityId }));
			const saveResult = yield* take([ActivityActions.save.done, ActivityActions.save.failed]);
			yield* put(ActivityActions.setDirty({ activityId: action.payload.activityId, dirty: false }));
			if (saveResult.type === ActivityActions.save.done.type) {
				yield* call(dispatchJsonRpcRequests, [completeTaskRequest]);
				yield* put(WorkflowsActions.completeTask.done({ params: action.payload, result: {} }));
				yield* put(ApplicationActions.setBusy(false));
			} else {
				logger.error(`Error occurred while saving document. Could not complete task.`);
				yield* call(dispatchCompleteTaskFailedAction, action, completeTaskRequest);
			}
		} catch (error) {
			logger.error(`Error sending RPC request:`, completeTaskRequest);
			logger.error(error);
			yield* call(dispatchCompleteTaskFailedAction, action, completeTaskRequest);
		}
		logger.log("Finished handling matched Workflows/COMPLETE_TASK action");
		yield* put(ApplicationActions.setBusy(false));
	}
};

function* dispatchCompleteTaskFailedAction(
	action: Action<WorkflowsActions.CompleteTaskPayload>,
	completeTaskRequest: TaskJsonRpc2Request.CompleteJsonRpc2Request
): SagaIterator<void> {
	yield* put(
		WorkflowsActions.completeTask.failed({
			params: action.payload,
			error: new Error(`Failed to complete task ${completeTaskRequest.id}.`)
		})
	);
}
