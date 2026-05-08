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

import { JsonRpc2Request } from "@com.mgmtp.a12.dataservices/dataservices-access/lib/json-rpc/index.js";

import { TaskJsonRpc2Request } from "../../../task/index.js";
import { readFile } from "../../testUtils/testUtils.js";

const DOCUMENT_REFERENCE = "23";

describe(`TaskCompleteOperation`, () => {
	const rpcRequest: JsonRpc2Request = extractJsonRpcRequest("jsonrpc_complete_task_request.json");
	const invalidRpcRequest: JsonRpc2Request = extractJsonRpcRequest("jsonrpc_invalid_request.json");

	it(`tests type guard of COMPLETE_TASK request`, () => {
		expect(TaskJsonRpc2Request.isInstance(rpcRequest)).toBeTruthy();
	});

	it(`tests type guard with invalid request`, () => {
		expect(TaskJsonRpc2Request.isInstance(invalidRpcRequest)).not.toBeTruthy();
	});

	it(`tests parameters of COMPLETE_TASK request`, () => {
		const {
			id,
			method,
			params: { docRef }
		} = rpcRequest as TaskJsonRpc2Request.CompleteJsonRpc2Request;

		expect(id).toBe("taskCompleteOperation");
		expect(method).toBe("COMPLETE_TASK");
		expect(docRef).toBe(DOCUMENT_REFERENCE);
	});
});

describe(`StartProcessOperation`, () => {
	const rpcRequest: JsonRpc2Request = extractJsonRpcRequest("jsonrpc_start_process_request.json");

	it(`tests type guard of START_PROCESS request`, () => {
		expect(TaskJsonRpc2Request.isInstance(rpcRequest)).toBeTruthy();
	});

	it(`tests parameters of START_PROCESS request`, () => {
		const {
			id,
			method,
			params: { processDefinition, variables, businessKey }
		} = rpcRequest as TaskJsonRpc2Request.StartProcessJsonRpc2Request;

		expect(id).toBe("startProcessOperation");
		expect(method).toBe("START_PROCESS");
		expect(processDefinition).toBe("TestProcess");
		expect(businessKey).toBe("abc");
		expect(variables).not.toBeNull();
	});
});

describe(`SendMessageOperation`, () => {
	const rpcRequest: JsonRpc2Request = extractJsonRpcRequest("jsonrpc_send_message_request.json");

	it(`tests type guard of SEND_MESSAGE request`, () => {
		expect(TaskJsonRpc2Request.isInstance(rpcRequest)).toBeTruthy();
	});

	it.each([
		["someBusinessKey", "someId"],
		["someBusinessKey", undefined]
	])(
		`tests parameters of SEND_MESSAGE request with processInstanceId %s and businessKey %s`,
		(businessKeyInput: string, processInstanceIdInput?: string) => {
			const sendMessageRequestObject = rpcRequest as TaskJsonRpc2Request.SendMessageJsonRpc2Request;
			sendMessageRequestObject.params.processInstanceId = processInstanceIdInput;
			sendMessageRequestObject.params.businessKey = businessKeyInput;
			const {
				id,
				method,
				params: { messageName, businessKey, processInstanceId, payload }
			} = sendMessageRequestObject;

			expect(id).toBe("SendMessageOperation");
			expect(method).toBe("SEND_MESSAGE");
			expect(processInstanceId).toBe(processInstanceIdInput);
			expect(messageName).toBe("EVENT_ACCEPT");
			expect(payload).not.toBeNull();
			expect(businessKey).toBe(businessKeyInput);
		}
	);
});

describe(`TaskAssignOperation`, () => {
	const rpcRequest: JsonRpc2Request = extractJsonRpcRequest("jsonrpc_assign_task_request.json");

	it(`tests type guard of ASSIGN_TASK request`, () => {
		expect(TaskJsonRpc2Request.isInstance(rpcRequest)).toBeTruthy();
	});

	it(`tests parameters of ASSIGN_TASK request`, () => {
		const {
			id,
			method,
			params: { docRef, assignee }
		} = rpcRequest as TaskJsonRpc2Request.AssignJsonRpc2Request;

		expect(id).toBe("TaskAssignOperation");
		expect(method).toBe("ASSIGN_TASK");
		expect(docRef).toBe(DOCUMENT_REFERENCE);
		expect(assignee).toBe("admin");
	});
});

function extractJsonRpcRequest(path: string): JsonRpc2Request {
	const requestAsString = readFile("../../tests/access/resources/rpc/" + path);
	const rpcRequest = JSON.parse(requestAsString);
	expect(rpcRequest).not.toBeNull();
	return rpcRequest;
}
