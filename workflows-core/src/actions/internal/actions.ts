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

import { actionCreatorFactory, AnyAction } from "typescript-fsa";

import { Activity } from "@com.mgmtp.a12.client/client-core/lib/core/activity/index.js";

import { DocumentDto } from "../../task/index.js";

const factory = actionCreatorFactory("Workflows");

export namespace WorkflowsActions {
	export interface AssignTaskPayload {
		docRef: string;
		assignee: string;
	}

	export const assignTask = factory.async<AssignTaskPayload, Record<string, never>, Error>("ASSIGN_TASK");

	export interface CompleteTaskPayload {
		activityId: string;
		document: Activity.Data.Document;
		docRef: string;
	}

	export const completeTask = factory.async<CompleteTaskPayload, Record<string, never>, Error>("COMPLETE_TASK");

	export interface SendMessagePayload {
		eventName: string;
		businessKey: string;
		processInstanceId?: string;
		payload?: DocumentDto;
	}

	export const sendMessage = factory.async<SendMessagePayload, Record<string, never>, Error>("SEND_MESSAGE");

	export interface StartProcessPayload {
		processDefinition: string;
		businessKey: string;
		variables?: DocumentDto;
	}

	export const startProcess = factory.async<StartProcessPayload, Record<string, never>, Error>("START_PROCESS");

	export const isWorkflowsOutcomeAction = (action: AnyAction) => {
		return (
			startProcess.done.match(action) ||
			startProcess.failed.match(action) ||
			assignTask.done.match(action) ||
			assignTask.failed.match(action) ||
			completeTask.done.match(action) ||
			completeTask.failed.match(action) ||
			sendMessage.done.match(action) ||
			sendMessage.failed.match(action) ||
			claimRequested.match(action)
		);
	};

	export const adaptAppModel = factory<AdaptAppModelPayload>("ADAPT_APP_MODEL");

	export interface AdaptAppModelPayload {
		readonly activityId: string;
		readonly flowName: string;
		readonly formComponentName: string;
	}

	export interface ClaimPayload {
		readonly docRef: string;
	}

	export const claimRequested = factory<ClaimPayload>("CLAIM");
}
