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
import { expectSaga } from "redux-saga-test-plan";

import { ConnectorLocator, RestServerConnector } from "@com.mgmtp.a12.utils/utils-connector";
import { LocaleSelectors } from "@com.mgmtp.a12.client/client-core";
import { Locale } from "@com.mgmtp.a12.utils/utils-localization";

import { RequestBuilder } from "../../utils/index.js";
import { TaskJsonRpc2Request } from "../../task/index.js";
import { dispatchJsonRpcRequest } from "../../utils/internal/requests/requestUtils.js";

import { readFile } from "../testUtils/testUtils.js";

type CompleteJsonRpc2Request = TaskJsonRpc2Request.CompleteJsonRpc2Request;

const completeTaskRequestString = readFile("../resources/requests/jsonrpc_complete_task_request.json");
const completeTaskRequestObject: CompleteJsonRpc2Request = JSON.parse(completeTaskRequestString);
const jsonRpc2Request = RequestBuilder.completeTask(
	completeTaskRequestObject.id,
	completeTaskRequestObject.params.docRef
);

const restServerConnector: RestServerConnector = new RestServerConnector("someUrlBase");
const connectorLocator: ConnectorLocator = new ConnectorLocator(restServerConnector);
vi.spyOn(ConnectorLocator, "getInstance").mockReturnValue(connectorLocator);

const positiveResponse = {
	ok: true,
	status: 200,
	headers: {
		get: (header: string) => (header === "Content-Type" ? "text/plain" : null)
	},
	text: () => Promise.resolve("This is some plain text response"),
	json: () => Promise.resolve({})
} as Response;
vi.spyOn(restServerConnector, "fetchData").mockReturnValue(Promise.resolve(positiveResponse));

const singleData = { result: "someResult" };
const data = [singleData];
vi.spyOn(positiveResponse, "json").mockReturnValue(Promise.resolve(data));

const locale: Locale = { language: "en", country: "US" };
vi.spyOn(LocaleSelectors, "locale").mockReturnValue(() => {
	return locale;
});

describe("requestUtils tests", () => {
	it("should successfully dispatch JsonRpcRequest", () => {
		// when/then
		return expectSaga(dispatchJsonRpcRequest, jsonRpc2Request)
			.run()
			.then((sagaResult) => {
				expect(sagaResult.returnValue.result).toStrictEqual(singleData.result);
			});
	});
	it("should throw error when fetching data failed", () => {
		const errorResponse = {
			status: 500,
			headers: {
				get: (header: string) => (header === "Content-Type" ? "text/plain" : null)
			},
			text: () => Promise.resolve("This is some error response"),
			json: () => Promise.resolve({})
		} as Response;
		vi.spyOn(restServerConnector, "fetchData").mockReturnValue(Promise.resolve(errorResponse));
		vi.spyOn(errorResponse, "json").mockReturnValue(Promise.resolve(data));

		// when/then
		return expectSaga(dispatchJsonRpcRequest, jsonRpc2Request)
			.throws(new Error(`Error fetching data with status code ${errorResponse.status}`))
			.run();
	});
	it("should throw error when received error in Data Services response", () => {
		// given
		vi.spyOn(restServerConnector, "fetchData").mockReturnValue(Promise.resolve(positiveResponse));
		const singleErrorData = {
			error: {
				data: {
					level: "someLevel",
					title: {
						default: "someDefaultTitle",
						key: "someTitleKey"
					},
					description: {
						default: "someErrorDescription",
						key: "someDescriptionKey"
					},
					details: {
						code: "someCode",
						subsystem: "someSubSystem",
						time: "someTime"
					}
				}
			}
		};
		const errorData = [singleErrorData];
		vi.spyOn(positiveResponse, "json").mockReturnValue(Promise.resolve(errorData));

		// when/then
		return expectSaga(dispatchJsonRpcRequest, jsonRpc2Request)
			.throws(new Error(`Received error in Data Services response: ${singleErrorData.error.data.description.default}`))
			.run();
	});
});
