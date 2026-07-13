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

import { JsonRpc2Request } from "@com.mgmtp.a12.dataservices/dataservices-access";

export type DocumentDto = object;

export type TaskJsonRpc2RequestType =
	| TaskJsonRpc2Request.CompleteJsonRpc2Request
	| TaskJsonRpc2Request.AssignJsonRpc2Request
	| TaskJsonRpc2Request.SendMessageJsonRpc2Request
	| TaskJsonRpc2Request.StartProcessJsonRpc2Request;

export namespace TaskJsonRpc2Request {
	export function isInstance(obj: object): obj is TaskJsonRpc2RequestType {
		const taskDocumentRpcMethods = ["COMPLETE_TASK", "ASSIGN_TASK", "SEND_MESSAGE", "START_PROCESS"];
		return JsonRpc2Request.isInstance(obj) && taskDocumentRpcMethods.some((op) => op === obj.method);
	}

	export interface CompleteJsonRpc2Request extends JsonRpc2Request {
		readonly method: "COMPLETE_TASK";
		readonly params: {
			docRef: string;
		};
	}

	export interface AssignJsonRpc2Request extends JsonRpc2Request {
		readonly method: "ASSIGN_TASK";
		readonly params: {
			docRef: string;
			assignee: string;
		};
	}

	export interface SendMessageJsonRpc2Request extends JsonRpc2Request {
		readonly method: "SEND_MESSAGE";
		readonly params: {
			messageName: string;
			businessKey: string;
			processInstanceId: string | undefined;
			payload?: DocumentDto;
		};
	}

	export interface StartProcessJsonRpc2Request extends JsonRpc2Request {
		readonly method: "START_PROCESS";
		readonly params: {
			processDefinition: string;
			businessKey?: string;
			variables?: DocumentDto;
		};
	}
}
