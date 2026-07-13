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

import type { Middleware } from "redux";

import {
	FormEngineActions,
	Events,
	findElementByFormModelPath,
	isFormModel,
	isFormModelEventButton
} from "@com.mgmtp.a12.formengine/formengine-core";
import { LoggerFactory } from "@com.mgmtp.a12.utils/utils-logging";
import { Activity, ActivitySelectors, ModelSelectors } from "@com.mgmtp.a12.client/client-core";

import { WorkflowsEvents } from "../../events/index.js";
import { WorkflowsActions } from "../../actions/index.js";
import { findA12wfObject } from "../../utils/internal/common/commonUtils.js";

const logger = LoggerFactory.getLogger("sendMessageFromFormMiddleware");

export const sendMessageFromFormMiddleware: Middleware = (api) => (next) => (action) => {
	if (
		FormEngineActions.event.match(action) &&
		Events.eventButton.match(action.payload.engineEvent) &&
		WorkflowsEvents.SEND_MESSAGE === action.payload.engineEvent.payload.name
	) {
		logger.info(`Handling matched SEND_MESSAGE event.`);
		logger.log(`Triggered by action: ${action}`);

		// === 1) Find the message name ===
		const formModel = ModelSelectors.modelInScene(
			{ activityId: action.payload.activityId, modelType: "form" },
			isFormModel
		)(api.getState());

		if (formModel === undefined) {
			logger.error(`Could not find form model in scene of activity with id ${action.payload.activityId}`);
			return next(action);
		}

		const buttonPath = action.payload.engineEvent.payload.buttonPath;

		logger.log(`Attempting to find form model element based on path ${buttonPath}`);
		const modelElement = findElementByFormModelPath(formModel, buttonPath);
		logger.log(`Found form model element that triggered this event:`, modelElement);

		if (modelElement === undefined) {
			logger.error(`Form model element at ${buttonPath} is undefined.`);
			return next(action);
		}

		if (!isFormModelEventButton(modelElement)) {
			logger.error(`Form model element at ${buttonPath} is not an event button.`);
			return next(action);
		}

		const messageName = modelElement.annotations?.filter((a) => a.name === "messageName")[0].value;

		if (messageName === undefined) {
			logger.error(`Event button is missing the 'messageName' annotation as payload.`);
			return next(action);
		}

		// === 2) Find the processInstanceId ===
		const taskFormActivity = ActivitySelectors.activityById(action.payload.activityId)(api.getState());
		logger.log(`Found task form activity:`, taskFormActivity);

		const defaultDataHolder = taskFormActivity?.dataHolders?.find(
			Activity.DataHolder.hasDescriptor(taskFormActivity.descriptor)
		);
		logger.log(`Found task form data holder:`, defaultDataHolder);

		if (defaultDataHolder === undefined) {
			logger.error(`Found data holder is undefined.`);
			return next(action);
		}

		if (defaultDataHolder.data === undefined) {
			logger.error(`Found data inside data holder is undefined.`);
			return next(action);
		}

		const A12WF = findA12wfObject(defaultDataHolder.data) as {
			Process: { ID: string; BusinessKey: string };
		};

		const ProcessInstanceId = A12WF?.Process?.ID;
		const BusinessKey = A12WF?.Process?.BusinessKey;

		if (ProcessInstanceId === undefined && BusinessKey === undefined) {
			logger.error(
				`Both processInstanceId and businessKey for activity ${taskFormActivity?.id} are undefined. Please define at least one of them.`
			);
			return next(action);
		}

		logger.log(
			`Found processInstanceId ${ProcessInstanceId} and businessKey ${BusinessKey} for task form activity with id ${taskFormActivity?.id}`
		);

		const sendMessageAction = WorkflowsActions.sendMessage.started({
			processInstanceId: ProcessInstanceId,
			businessKey: BusinessKey,
			eventName: messageName
		});

		logger.log(`Dispatching sendMessage.started action:`, sendMessageAction);
		api.dispatch(sendMessageAction);
		logger.log(`Successfully dispatched sendMessage.started action:`, sendMessageAction);
		logger.log("Finished handling SEND_MESSAGE event");
		return next(action);
	} else {
		return next(action);
	}
};
