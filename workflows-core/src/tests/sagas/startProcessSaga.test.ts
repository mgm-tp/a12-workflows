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

import { WorkflowsActions } from "../../actions/index.js";
import { startProcessSaga } from "../../sagas/internal/startProcessSaga.js";

import { testCanHandle, testNegativeResponse, testPositiveResponse } from "../testUtils/commonTests.js";
import { readFile } from "../testUtils/testUtils.js";

const action = WorkflowsActions.startProcess;
const saga = startProcessSaga;

const payloadString = readFile("../resources/actionPayloads/jsonrpc_assign_task_payload.json");

const payload = JSON.parse(payloadString);

const startedAction = action.started(payload);

const failedAction = action.failed({ params: payload, error: new Error("someError") });

describe("Start process saga ...", () => {
	describe("canHandle...", () => {
		testCanHandle(saga, [startedAction]);
	});

	describe("handle ...", () => {
		testPositiveResponse(saga, startedAction, action.done({ params: startedAction.payload, result: {} }));
		testNegativeResponse(saga, startedAction, failedAction);
	});
});
