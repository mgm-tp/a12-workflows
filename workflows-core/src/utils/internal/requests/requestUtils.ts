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

import { call, type SagaGenerator, select } from "typed-redux-saga";

import { ConnectorLocator, type RestRequestPayload, RestServerConnector } from "@com.mgmtp.a12.utils/utils-connector";
import {
	JsonRpc2Request,
	JsonRpc2Response,
	type JsonRpc2ResponseOK
} from "@com.mgmtp.a12.dataservices/dataservices-access";
import { LoggerFactory } from "@com.mgmtp.a12.utils/utils-logging";
import { LocaleSelectors } from "@com.mgmtp.a12.client/client-core";

const logger = LoggerFactory.getLogger("requestUtils");

export function* dispatchJsonRpcRequests(jsonRpcRequests: JsonRpc2Request[]): SagaGenerator<JsonRpc2ResponseOK[]> {
	const request = JsonRpc2Request.build(jsonRpcRequests);
	const locale = yield* select(LocaleSelectors.locale());
	const requestWithHeaders: typeof request = {
		...request,
		customHeaders: [...(request.customHeaders ?? []), ["Accept-Language", locale.language]]
	};

	const data = yield* call(sendRequest, requestWithHeaders);
	logger.log(`Retrieved response data:`, data);

	if (JsonRpc2Response.hasErrors(data)) {
		const errorDescription = (data[0].error?.data as JsonRpc2Response.Exception).description.default;
		logger.error(errorDescription);
		throw new Error(`Received error in Data Services response: ${errorDescription}`);
	}

	return data as JsonRpc2ResponseOK[];
}

async function sendRequest(request: RestRequestPayload): Promise<any> {
	const connector = ConnectorLocator.getInstance().getServerConnector() as RestServerConnector;
	logger.log(`Sending RPC request:`, request);
	const response = await connector.fetchData(request);
	if (!response.ok) {
		throw new Error(`Error fetching data with status code ${response.status}`);
	}
	return await response.json();
}

export function* dispatchJsonRpcRequest(jsonRpcRequest: JsonRpc2Request): SagaGenerator<JsonRpc2ResponseOK> {
	const response = yield* call(dispatchJsonRpcRequests, [jsonRpcRequest]);
	return response[0];
}
