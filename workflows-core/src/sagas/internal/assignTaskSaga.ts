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

import { call, put, type SagaGenerator } from "typed-redux-saga";

import type { Action, AnyAction } from "@com.mgmtp.a12.client/typescript-fsa-redux-5-compat";
import { Activity, type ApplicationSaga } from "@com.mgmtp.a12.client/client-core";
import { LoggerFactory } from "@com.mgmtp.a12.utils/utils-logging";

import { WorkflowsActions } from "../../actions/index.js";
import { RequestBuilder } from "../../utils/index.js";
import { dispatchJsonRpcRequest } from "../../utils/internal/requests/requestUtils.js";

const logger = LoggerFactory.getLogger("assignTaskSaga");

export const assignTaskSaga: ApplicationSaga.Descriptor = {
	canHandle: (_: Activity.Descriptor, action: AnyAction) => {
		return WorkflowsActions.assignTask.started.match(action);
	},
	handle: function* (action: Action<WorkflowsActions.AssignTaskPayload>): SagaGenerator<void> {
		logger.info("Handling matched Workflows/ASSIGN_TASK_STARTED action");
		logger.log(`Triggered by action: `, action);
		const { assignee, docRef } = action.payload;

		const assignTaskRequest = RequestBuilder.assignTask("TaskAssignOperation", docRef, assignee);
		try {
			yield* call(dispatchJsonRpcRequest, assignTaskRequest);
			yield* put(WorkflowsActions.assignTask.done({ params: action.payload, result: {} }));
			logger.log("Finished handling Workflows/ASSIGN_TASK_STARTED action.");
		} catch (error) {
			logger.error(`Error sending RPC request:`, assignTaskRequest);
			logger.error(error);
			yield* put(
				WorkflowsActions.assignTask.failed({
					params: assignTaskRequest.params,
					error: new Error(`Failed to assign assignee ${assignee} to task ${assignTaskRequest.id}.`)
				})
			);
		}
	}
};
