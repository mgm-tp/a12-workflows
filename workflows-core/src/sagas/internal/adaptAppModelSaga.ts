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

import { put, type SagaGenerator, select } from "typed-redux-saga";

import type { Action } from "@com.mgmtp.a12.client/typescript-fsa-redux-5-compat";
import {
	Activity,
	ActivityActions,
	ActivitySelectors,
	type ApplicationSaga,
	ApplicationModel,
	ModelActions,
	ModelSelectors
} from "@com.mgmtp.a12.client/client-core";
import { OverviewActivity } from "@com.mgmtp.a12.overviewengine/overviewengine-core";
import { LoggerFactory } from "@com.mgmtp.a12.utils/utils-logging";

import { WorkflowsActions } from "../../actions/index.js";
import { findFlow } from "../../utils/internal/appmodel/appmodelUtils.js";
import { findA12wfObject } from "../../utils/internal/common/commonUtils.js";

type Flow = ApplicationModel.Flow;
import DocumentListData = OverviewActivity.Data.DocumentListData;
import adaptAppModel = WorkflowsActions.adaptAppModel;

type AdaptAppModelPayload = WorkflowsActions.AdaptAppModelPayload;
type PushPayload = ActivityActions.PushPayload;
type Scene = ApplicationModel.Scene;

const logger = LoggerFactory.getLogger("adaptAppModelSaga");

export const adaptAppModelSaga: ApplicationSaga.Descriptor = {
	canHandle: (descriptor: Activity.Descriptor, action: Action<PushPayload>) => {
		return adaptAppModel.match(action);
	},
	handle: (action: Action<AdaptAppModelPayload>) => {
		logger.info("Handling matched Workflows/ADAPT_APP_MODEL action");
		logger.log(`Triggered by action: `, action);
		const result = adaptAppModelFun(
			action.payload.activityId,
			action.payload.flowName,
			action.payload.formComponentName
		);
		logger.log("Finished handling Workflows/ADAPT_APP_MODEL action");
		return result;
	}
};

export function getTaskForm(
	taskDocument: Activity.Data.Document | undefined
): { documentModelId: string; formKey: string } | undefined {
	if (!taskDocument) {
		return;
	}
	const A12WF = findA12wfObject(taskDocument) as { TaskDefinition: { FormKey: string } };
	return { documentModelId: taskDocument.modelId, formKey: A12WF.TaskDefinition.FormKey };
}

function* adaptAppModelFun(activityId: string, flowName: string, formComponentName: string): SagaGenerator<void> {
	const activity = yield* select(ActivitySelectors.activityById(activityId));
	if (!activity) {
		return;
	}
	const data = (yield* select(ActivitySelectors.data(activity.id))) as DocumentListData;
	const formKeys = data.documents.filter((s) => s != undefined).map(getTaskForm);
	const applicationModel = yield* select(ModelSelectors.applicationModel());
	const scenes = new Array<Scene>();
	for (const taskForm of formKeys) {
		if (taskForm) {
			const newScene = yield* buildSceneFor(
				taskForm.formKey,
				taskForm.documentModelId,
				flowName,
				formComponentName,
				applicationModel
			);
			if (newScene) {
				scenes.push(newScene);
			}
		}
	}
	if (scenes.length > 0) {
		yield* updateAppModelInState(applicationModel);
	}
}

// eslint-disable-next-line require-yield
function* buildSceneFor(
	taskForm: string,
	documentModel: string,
	flowName: string,
	formComponentName: string,
	applicationModel: ApplicationModel
): SagaGenerator<Scene | undefined> {
	const generatedSceneName = `_generated_${taskForm}Scene`;

	const existingScene = findScene(applicationModel, generatedSceneName);

	if (existingScene === undefined) {
		const newScene = buildScene(generatedSceneName, taskForm, documentModel, formComponentName);
		const sceneSuccessfullyAdded = addSceneToAppModel(newScene, applicationModel, flowName);
		if (sceneSuccessfullyAdded) {
			return newScene;
		}
	}
	return;
}

function* updateAppModelInState(updatedAppModel: ApplicationModel): SagaGenerator<void> {
	yield* put(
		ModelActions.setApplicationModel({
			model: updatedAppModel
		})
	);
}

function findScene(appModel: ApplicationModel, sceneName: string): Scene | undefined {
	for (const module of appModel.content.modules) {
		for (const flow of module.flows) {
			for (const scene of flow.scenes) {
				if (scene.name === sceneName) {
					return scene;
				}
			}
		}
	}
	return undefined;
}

function buildScene(name: string, formKey: string, documentModel: string, formComponentName: string): Scene {
	return {
		name: name,
		description: `Scene for ${formKey}`,
		matchConditions: [
			{
				key: "model",
				mustEqual: documentModel
			},
			{
				key: "instance",
				isSet: true
			},
			{
				key: "formKey",
				mustEqual: formKey
			}
		],
		sceneChange: {
			onEnter: [
				{
					type: "VIEW_ADD",
					name: formComponentName,
					constraints: {
						type: "MasterDetail"
					},
					models: [
						{
							modelType: "form",
							name: formKey,
							documentModel: documentModel
						}
					]
				}
			]
		}
	};
}

type MutableFlow = {
	-readonly [K in keyof Flow]: Flow[K];
};

function addSceneToAppModel(newScene: Scene, appModel: ApplicationModel, flowName: string): boolean {
	const taskFlow: MutableFlow | undefined = findFlow(appModel, flowName);
	if (taskFlow && taskFlow.scenes) {
		taskFlow.scenes = [...taskFlow.scenes, newScene];
		return true;
	}
	return false;
}
