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

import { TaskJsonRpc2Request } from "../../task/index.js";
import { RequestBuilder } from "../../utils/index.js";

import { readFile } from "../testUtils/testUtils.js";

type AssignJsonRpc2Request = TaskJsonRpc2Request.AssignJsonRpc2Request;
type CompleteJsonRpc2Request = TaskJsonRpc2Request.CompleteJsonRpc2Request;
type SendMessageJsonRpc2Request = TaskJsonRpc2Request.SendMessageJsonRpc2Request;
type StartProcessJsonRpc2Request = TaskJsonRpc2Request.StartProcessJsonRpc2Request;

const assignTaskRequestString = readFile("../resources/requests/jsonrpc_assign_task_request.json");

const assignTaskRequestObject: AssignJsonRpc2Request = JSON.parse(assignTaskRequestString);

const completeTaskRequestString = readFile("../resources/requests/jsonrpc_complete_task_request.json");

const completeTaskRequestObject: CompleteJsonRpc2Request = JSON.parse(completeTaskRequestString);

const sendMessageRequestString = readFile("../resources/requests/jsonrpc_send_message_request.json");

const sendMessageRequestObject: SendMessageJsonRpc2Request = JSON.parse(sendMessageRequestString);

const startProcessRequestString = readFile("../resources/requests/jsonrpc_start_process_request.json");

const startProcessRequestObject: StartProcessJsonRpc2Request = JSON.parse(startProcessRequestString);

describe("RequestBuilder ... ", () => {
	it("builds assign task requests correctly", () => {
		const req = RequestBuilder.assignTask(
			assignTaskRequestObject.id,
			assignTaskRequestObject.params.docRef,
			assignTaskRequestObject.params.assignee
		);
		expect(req).toEqual(assignTaskRequestObject);
	});

	it("builds complete task requests correctly", () => {
		const req = RequestBuilder.completeTask(completeTaskRequestObject.id, completeTaskRequestObject.params.docRef);
		expect(req).toEqual(completeTaskRequestObject);
	});

	it.each([
		["someBusinessKey", "someId"],
		["someBusinessKey", undefined]
	])(
		`builds send message requests correctly with businessKey %s and processInstanceId %s`,
		(businessKey: string, processInstanceId?: string) => {
			sendMessageRequestObject.params.processInstanceId = processInstanceId;
			sendMessageRequestObject.params.businessKey = businessKey;

			const req = RequestBuilder.sendMessage(
				sendMessageRequestObject.params.messageName,
				sendMessageRequestObject.params.businessKey,
				sendMessageRequestObject.params.processInstanceId,
				sendMessageRequestObject.id,
				sendMessageRequestObject.params.payload
			);
			expect(req).toEqual(sendMessageRequestObject);
		}
	);

	it("builds start process requests correctly", () => {
		const req = RequestBuilder.startProcess(
			startProcessRequestObject.id,
			startProcessRequestObject.params.processDefinition,
			startProcessRequestObject.params.businessKey,
			startProcessRequestObject.params.variables
		);
		expect(req).toEqual(startProcessRequestObject);
	});
});
