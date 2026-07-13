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

import {
	ApplicationModel,
	ModelActions,
	ModelSelectors,
	Activity,
	ActivitySelectors
} from "@com.mgmtp.a12.client/client-core";

import { adaptAppModelSaga } from "../../sagas/internal/adaptAppModelSaga.js";
import { WorkflowsActions } from "../../actions/index.js";

import { createActivity, readFile } from "../testUtils/testUtils.js";
import { testCanHandle } from "../testUtils/commonTests.js";

type LoadingState = Activity.LoadingState;

const saga = adaptAppModelSaga;

const ACTIVITY_ID = "SHOW_WORKFLOWS_VIEW";

const adaptAppModelAction = WorkflowsActions.adaptAppModel(
	{ activityId: ACTIVITY_ID, flowName: "someFlow", formComponentName: "someFormComponent" },
	{}
);

const adaptAppModelDataHolder = { loadingState: "loaded" as LoadingState, data: {} };

const applicationModelMockString = readFile("../resources/models/appmodel-mock.json");
const applicationModelMockObject: ApplicationModel = JSON.parse(applicationModelMockString);

function createDocumentsData(numberOfDocuments: number) {
	const documents = new Array<Activity.Data.Document>();
	for (let i = 0; i < numberOfDocuments; i++) {
		const document = {
			id: `someId${i}`,
			modelId: `someModelId${i}`,
			A12WF: { TaskDefinition: { FormKey: `someFormKey${i}` } }
		};
		documents.push(document);
	}
	return { documents: documents };
}

describe("Adapt app model saga ...", () => {
	describe("canHandle...", () => {
		testCanHandle(saga, [adaptAppModelAction]);
	});

	describe("handle", () => {
		const adaptAppModelActivity = createActivity({
			id: ACTIVITY_ID,
			descriptor: {
				view: "AppModelView",
				assignmentState: "ALL",
				processes: "FirstProcess,SecondProcess"
			},
			initiatingActivityId: "1234",
			activityDataHolder: adaptAppModelDataHolder
		});
		vi.spyOn(ActivitySelectors, "activityById").mockReturnValue(() => adaptAppModelActivity);
		vi.spyOn(ModelSelectors, "applicationModel").mockReturnValue(() => applicationModelMockObject);

		it("dispatches single SET_APPLICATION_MODEL action", () => {
			const documentsData = createDocumentsData(2);
			vi.spyOn(ActivitySelectors, "data").mockReturnValue(() => documentsData);
			return expectSaga(saga.handle, adaptAppModelAction)
				.put.actionType(ModelActions.setApplicationModel.type)
				.run()
				.then((sagaResult) => {
					const { allEffects } = sagaResult;
					const putEffects = allEffects.filter((effect) => effect.type == "PUT");
					expect(putEffects.filter((x) => x.payload.action.type == "Model/SET_APPLICATION_MODEL")).toHaveLength(1);
				});
		});
		it(`dispatches SET_APPLICATION action with correct payload for nested A12WF metadata?`, () => {
			const formKey = "someFormKey";
			const documentDataWithNestedMetadata = {
				documents: [
					{
						id: `someId`,
						modelId: `someModelId`,
						person: {
							name: "someName",
							A12WF: { TaskDefinition: { FormKey: formKey } }
						}
					}
				]
			};
			vi.spyOn(ActivitySelectors, "data").mockReturnValue(() => documentDataWithNestedMetadata);
			return expectSaga(saga.handle, adaptAppModelAction)
				.put.actionType(ModelActions.setApplicationModel.type)
				.run()
				.then((sagaResult) => {
					const { allEffects } = sagaResult;
					const putEffects = allEffects.filter((effect) => effect.type == "PUT");
					expect(putEffects.filter((x) => x.payload.action.type == "Model/SET_APPLICATION_MODEL")).toHaveLength(1);
					expect(putEffects[0].payload.action.payload.model.content.modules[0].flows[0].scenes[2].name).toContain(
						formKey
					);
				});
		});
		it("dispatches no SET_APPLICATION_MODEL action", () => {
			const emptyDocumentsData = createDocumentsData(0);
			vi.spyOn(ActivitySelectors, "data").mockReturnValue(() => emptyDocumentsData);
			return expectSaga(saga.handle, adaptAppModelAction)
				.not.put.actionType(ModelActions.setApplicationModel.type)
				.run();
		});
	});
});
