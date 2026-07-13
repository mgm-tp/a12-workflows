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
import type { SagaGenerator } from "typed-redux-saga";
import { call } from "redux-saga-test-plan/matchers.js";

import { DocumentModel, DocumentServiceFactory } from "@com.mgmtp.a12.kernel/kernel-md-facade";
import {
	DataOperation,
	DefaultRequestSelectorMap,
	OverviewEngineFactories,
	type OverviewModel,
	type BuildRequestsParams
} from "@com.mgmtp.a12.overviewengine/overviewengine-core";
import { Query, type SupportedRequest } from "@com.mgmtp.a12.dataservices/dataservices-access";
import { ActivitySelectors, ModelSelectors } from "@com.mgmtp.a12.client/client-core";

import { workflowsDataLoader } from "../../data/internal/workflowsDataLoader.js";
import { WorkflowsConstants } from "../../utils/internal/constants.js";

import { deserializeDocumentModel, readFile } from "../testUtils/testUtils.js";

const dataLoader = workflowsDataLoader;

/// Start: Mock data for test cases using document model
const ACTIVITY_ID_FOR_DM = "SHOW_WORKFLOWS_VIEW_FOR_DM";
const DOCUMENT_MODEL_NAME = "Person_DM";

const overviewModelString = readFile("../resources/models/overviewModels/Person_OM.json");
const overviewModel: OverviewModel = JSON.parse(overviewModelString);

const documentModelString = readFile(`../resources/models/documentModels/${DOCUMENT_MODEL_NAME}.json`);
// documentModel must be correctly deserialized as DocumentModel by Kernel to access elements
const documentModel: DocumentModel = deserializeDocumentModel(documentModelString);
/// End: Mock data for test cases using document model

/// Start: Mock data for test cases using document model with nested metadata
const ACTIVITY_ID_FOR_DM_WITH_NESTED_A12WF = "SHOW_WORKFLOWS_VIEW_FOR_DM_WITH_NESTED_A12WF";
const DOCUMENT_MODEL_NAME_WITH_NESTED_A12WF = "PersonWithNestedMetadata_DM";

const overviewModelWithNestedA12WfString = readFile(
	"../resources/models/overviewModels/PersonWithNestedMetadata_OM.json"
);
const overviewModelWithNestedA12Wf: OverviewModel = JSON.parse(overviewModelWithNestedA12WfString);

const documentModelStringWithNestedA12wf = readFile(
	`../resources/models/documentModels/${DOCUMENT_MODEL_NAME_WITH_NESTED_A12WF}.json`
);
const documentModelWithNestedA12wf: DocumentModel = deserializeDocumentModel(documentModelStringWithNestedA12wf);
/// End: Mock data for test cases using document model with nested metadata

/// Start: Mock data for test cases using cdm
const ACTIVITY_ID_FOR_CDM = "SHOW_WORKFLOWS_VIEW_FOR_CDM";
const COMPOSED_DOCUMENT_MODEL_NAME = "TeamEmployee_CDM";
const ROOT_DOCUMENT_MODEL_NAME = "Team_DM";

const cdmOverviewModelString = readFile("../resources/models/cdm_related_models/TeamEmployee_OM.json");
const cdmOverviewModel: OverviewModel = JSON.parse(cdmOverviewModelString);

const cdmString = readFile(`../resources/models/cdm_related_models/${COMPOSED_DOCUMENT_MODEL_NAME}.json`);
const cdm: DocumentModel = deserializeDocumentModel(cdmString);

const rootDocumentModelString = readFile(`../resources/models/cdm_related_models/${ROOT_DOCUMENT_MODEL_NAME}.json`);
const rootDocumentModel: DocumentModel = deserializeDocumentModel(rootDocumentModelString);
/// End: Mock data for test cases using cdm

const buildRequests = dataLoader.buildRequests as (params: BuildRequestsParams) => SagaGenerator<SupportedRequest[]>;

const query: DataOperation.Query = {
	id: "someDataProviderId",
	type: "LIST_DOCUMENTS",
	paging: {
		pageNumbers: [0],
		pageSize: 20
	},
	sort: [
		{
			direction: Query.Direction.ASC,
			field: "/some/Field/Name",
			ignoreCase: true,
			nullHandling: Query.NullHandling.NULLS_LAST
		}
	]
};

function createBuildRequestsParams(
	activityId: string,
	documentModel: DocumentModel,
	overviewModel: OverviewModel,
	inputQuery: DataOperation.Query = query
): BuildRequestsParams {
	return {
		activityId: activityId,
		queries: [inputQuery],
		documentService: new DocumentServiceFactory().getDocumentService(),
		documentModel: documentModel,
		overviewModel: overviewModel,
		requestSelectorMap: DefaultRequestSelectorMap
	};
}

function createTaskOverviewDescriptor(documentModel: DocumentModel) {
	return {
		view: WorkflowsConstants.TASK_LIST_VIEW,
		model: documentModel.header.id,
		instance: "someDocRef"
	};
}

vi.spyOn(ActivitySelectors, "activityPropById").mockImplementation((activityId: string) => {
	if (activityId === ACTIVITY_ID_FOR_DM) {
		return () => createTaskOverviewDescriptor(documentModel);
	} else if (activityId === ACTIVITY_ID_FOR_DM_WITH_NESTED_A12WF) {
		return () => createTaskOverviewDescriptor(documentModelWithNestedA12wf);
	} else {
		return () => createTaskOverviewDescriptor(cdm);
	}
});

vi.spyOn(ModelSelectors, "modelByName").mockImplementation((modelName: string) => {
	if (modelName === DOCUMENT_MODEL_NAME) {
		return () => documentModel;
	} else if (modelName === DOCUMENT_MODEL_NAME_WITH_NESTED_A12WF) {
		return () => documentModelWithNestedA12wf;
	} else if (modelName === COMPOSED_DOCUMENT_MODEL_NAME) {
		return () => cdm;
	} else {
		return () => rootDocumentModel;
	}
});

describe("workflowsDataLoader tests...", () => {
	it.each([
		["DM", ACTIVITY_ID_FOR_DM, documentModel, overviewModel, "/A12WF/Task/ID"],
		["CDM", ACTIVITY_ID_FOR_CDM, cdm, cdmOverviewModel, "/Team/A12WF/Task/ID"]
	])(
		"should set correct constraint for %s",
		async (
			printedValueForDocumentModelType: string,
			activityId: string,
			documentModel: DocumentModel,
			overviewModel: OverviewModel,
			expectedFieldValue: string
		) => {
			// given
			const provideDataParams = createBuildRequestsParams(activityId, documentModel, overviewModel);

			// when/then
			const sagaResult = await expectSaga(buildRequests, provideDataParams)
				.provide([[call.fn(OverviewEngineFactories.dataLoader.buildRequests), []]])
				.run();
			const matchedRpcCallEffects = sagaResult.allEffects
				.filter((effect) => effect.type == "CALL")
				.filter((effect_1) => effect_1.payload.args[0].queries !== undefined);
			expect(matchedRpcCallEffects).toHaveLength(1);
			const query = matchedRpcCallEffects[0].payload.args[0].queries[0];
			expect(query.constraint).toStrictEqual({
				operator: "not",
				operand: {
					operator: "undefined_match",
					field: expectedFieldValue
				}
			});
		}
	);

	it("should set correct constraint if A12WF metadata is nested inside document model", () => {
		// given
		const provideDataParams = createBuildRequestsParams(
			ACTIVITY_ID_FOR_DM_WITH_NESTED_A12WF,
			documentModelWithNestedA12wf,
			overviewModelWithNestedA12Wf
		);

		// when/then
		return expectSaga(buildRequests, provideDataParams)
			.provide([[call.fn(OverviewEngineFactories.dataLoader.buildRequests), []]])
			.run()
			.then((sagaResult) => {
				const matchedRpcCallEffects = sagaResult.allEffects
					.filter((effect) => effect.type == "CALL")
					.filter((effect) => effect.payload.args[0].queries !== undefined);
				expect(matchedRpcCallEffects).toHaveLength(1);
				const query = matchedRpcCallEffects[0].payload.args[0].queries[0];
				expect(query.constraint).toStrictEqual({
					operator: "not",
					operand: {
						operator: "undefined_match",
						field: "/Person/A12WF/Task/ID"
					}
				});
			});
	});

	it("should set correct constraint coming from active ui filter, if set in OverviewEngineDataProvider is set", () => {
		// given
		const filterFieldPath = `Person.PersonalData.FirstName`;
		const filterFieldValue = "someValue";
		const queryWithConstraint: DataOperation.Query = {
			id: query.id,
			type: query.type,
			paging: query.paging,
			sort: query.sort,
			constraint: {
				operator: "exact_match",
				field: `/${filterFieldPath}`.split(".").join("/"),
				value: filterFieldValue,
				caseSensitive: true
			}
		};
		const provideDataParams = createBuildRequestsParams(
			ACTIVITY_ID_FOR_DM,
			documentModel,
			overviewModel,
			queryWithConstraint
		);

		// when/then
		return expectSaga(buildRequests, provideDataParams)
			.provide([[call.fn(OverviewEngineFactories.dataLoader.buildRequests), []]])
			.run()
			.then((sagaResult) => {
				const matchedRpcCallEffects = sagaResult.allEffects
					.filter((effect) => effect.type == "CALL")
					.filter((effect) => effect.payload.args[0].queries !== undefined);
				expect(matchedRpcCallEffects).toHaveLength(1);
				const query = matchedRpcCallEffects[0].payload.args[0].queries[0];
				expect(query.constraint).toStrictEqual({
					operator: "and",
					operands: [
						{
							operator: "exact_match",
							field: `/${filterFieldPath}`.split(".").join("/"),
							value: filterFieldValue,
							caseSensitive: true
						},
						{
							operator: "not",
							operand: {
								operator: "undefined_match",
								field: "/A12WF/Task/ID"
							}
						}
					]
				});
			});
	});

	it("should preserve fields projection for overview", () => {
		// given
		const queryWithProjection: DataOperation.Query = {
			id: query.id,
			type: query.type,
			paging: query.paging,
			sort: query.sort,
			fields: ["/some/Field/Name", "/some/Other/Field/Name"]
		};

		const provideDataParams = createBuildRequestsParams(
			ACTIVITY_ID_FOR_DM,
			documentModel,
			overviewModel,
			queryWithProjection
		);

		const fieldsRequiredByWF = ["/A12WF/Task/ID", "/A12WF/TaskDefinition/FormKey"];

		// when/then
		return expectSaga(buildRequests, provideDataParams)
			.provide([[call.fn(OverviewEngineFactories.dataLoader.buildRequests), []]])
			.run()
			.then((sagaResult) => {
				const matchedRpcCallEffects = sagaResult.allEffects
					.filter((effect) => effect.type == "CALL")
					.filter((effect) => effect.payload.args[0].queries !== undefined);
				expect(matchedRpcCallEffects).toHaveLength(1);
				const query = matchedRpcCallEffects[0].payload.args[0].queries[0] as DataOperation.ListDocuments.Query;
				expect(query.fields).toStrictEqual(["/some/Field/Name", "/some/Other/Field/Name", ...fieldsRequiredByWF]);
			});
	});

	it("should directly not set workflows specific constraint if activityDescriptor.view is not a TaskList", () => {
		// given
		vi.spyOn(ActivitySelectors, "activityPropById").mockReturnValue(() => {
			return {
				engine: "overview",
				module: "AllApplicationsModule"
			};
		});
		const provideDataParams = createBuildRequestsParams("someOverviewEngineActivityId", documentModel, overviewModel);

		// when/then
		return expectSaga(buildRequests, provideDataParams)
			.provide([[call.fn(OverviewEngineFactories.dataLoader.buildRequests), []]])
			.run()
			.then((sagaResult) => {
				const matchedRpcCallEffects = sagaResult.allEffects
					.filter((effect) => effect.type == "CALL")
					.filter((effect) => effect.payload.args[0].queries !== undefined);
				expect(matchedRpcCallEffects).toHaveLength(1);
				const query = matchedRpcCallEffects[0].payload.args[0].queries[0];
				expect(query.constraint).toStrictEqual(undefined);
			});
	});
});
