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
import { call, put } from "typed-redux-saga";

import { Activity } from "@com.mgmtp.a12.client/client-core/lib/core/activity/index.js";
import { ApplicationSaga } from "@com.mgmtp.a12.client/client-core/lib/core/application/index.js";
import { LoggerFactory } from "@com.mgmtp.a12.utils/utils-logging/index.js";

import { WorkflowsActions } from "../../actions/index.js";
import { RequestBuilder } from "../../utils/index.js";
import { dispatchJsonRpcRequest } from "../../utils/internal/requests/requestUtils.js";

const logger = LoggerFactory.getLogger("sendMessageSaga");

export const sendMessageSaga: ApplicationSaga.Descriptor = {
	canHandle: (_: Activity.Descriptor, action: AnyAction) => {
		return WorkflowsActions.sendMessage.started.match(action);
	},
	handle: function* (action: Action<WorkflowsActions.SendMessagePayload>): SagaIterator<void> {
		logger.info("Handling matched Workflows/SEND_MESSAGE_STARTED action");
		logger.log(`Triggered by action: `, action);
		const { eventName, processInstanceId, payload, businessKey } = action.payload;
		const sendMessageRequest = RequestBuilder.sendMessage(
			eventName,
			businessKey,
			processInstanceId,
			"sendMessage",
			payload
		);
		try {
			yield* call(dispatchJsonRpcRequest, sendMessageRequest);
			yield* put(WorkflowsActions.sendMessage.done({ params: action.payload, result: {} }));
		} catch (error) {
			logger.error(`Error sending RPC request:`, sendMessageRequest);
			logger.error(error);
			yield* put(
				WorkflowsActions.sendMessage.failed({
					params: action.payload,
					error: new Error(`Failed to send message for process ${processInstanceId}.`)
				})
			);
		}
		logger.log("Finished handling Workflows/SEND_MESSAGE_STARTED action");
	}
};
