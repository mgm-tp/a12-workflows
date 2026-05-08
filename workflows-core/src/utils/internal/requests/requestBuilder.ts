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

import { DocumentDto, TaskJsonRpc2Request } from "../../../task/index.js";

export const RequestBuilder = {
	startProcess(
		id: string | number | null,
		processDefinition: string,
		businessKey?: string | undefined,
		variables?: DocumentDto | undefined
	): TaskJsonRpc2Request.StartProcessJsonRpc2Request {
		return {
			jsonrpc: "2.0",
			method: "START_PROCESS",
			id,
			params: {
				processDefinition: processDefinition,
				businessKey: businessKey,
				variables: variables
			}
		};
	},

	sendMessage(
		messageName: string,
		businessKey: string,
		processInstanceId: string | undefined,
		id: string | number | null,
		payload?: DocumentDto
	): TaskJsonRpc2Request.SendMessageJsonRpc2Request {
		return {
			jsonrpc: "2.0",
			method: "SEND_MESSAGE",
			id,
			params: {
				messageName: messageName,
				businessKey: businessKey,
				processInstanceId: processInstanceId,
				payload: payload
			}
		};
	},

	assignTask(id: string | number | null, docRef: string, assignee: string): TaskJsonRpc2Request.AssignJsonRpc2Request {
		return {
			jsonrpc: "2.0",
			method: "ASSIGN_TASK",
			id,
			params: {
				docRef: docRef,
				assignee: assignee
			}
		};
	},

	completeTask(id: string | number | null, docRef: string): TaskJsonRpc2Request.CompleteJsonRpc2Request {
		return {
			jsonrpc: "2.0",
			method: "COMPLETE_TASK",
			id,
			params: {
				docRef: docRef
			}
		};
	}
};
