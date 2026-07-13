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

import { call, put, type SagaGenerator, select } from "typed-redux-saga";

import { Query, type SupportedRequest } from "@com.mgmtp.a12.dataservices/dataservices-access";
import { DocumentModel } from "@com.mgmtp.a12.kernel/kernel-md-facade";
import { ActivityActions, ActivitySelectors } from "@com.mgmtp.a12.client/client-core";
import {
	type BuildRequestsParams,
	DataOperation,
	type HandleResponsesParams,
	maybeAsyncFnWrapper,
	type OverviewEngineDataLoader,
	OverviewEngineFactories
} from "@com.mgmtp.a12.overviewengine/overviewengine-core";

import { CdmUtils } from "../../utils/internal/common/cdmUtils.js";
import { WorkflowsConstants } from "../../utils/index.js";
import { findPathToA12wfGroup } from "../../utils/internal/common/commonUtils.js";

function* defaultBuildRequests(params: BuildRequestsParams) {
	return yield* call(
		maybeAsyncFnWrapper(OverviewEngineFactories.dataLoader.buildRequests.bind(OverviewEngineFactories.dataLoader)),
		params
	);
}

function defaultHandleResponses(params: HandleResponsesParams): SagaGenerator<DataOperation.ResultSet> {
	return call(
		maybeAsyncFnWrapper(OverviewEngineFactories.dataLoader.handleResponses.bind(OverviewEngineFactories.dataLoader)),
		params
	);
}

/** @internal */
export const workflowsDataLoader: OverviewEngineDataLoader = {
	*buildRequests(params: BuildRequestsParams): SagaGenerator<SupportedRequest[]> {
		const { activityId, queries, documentModel } = params;

		const canHandleActivity = yield* isTaskList(activityId);
		if (!canHandleActivity) {
			return yield* defaultBuildRequests(params);
		}

		const modifiedQueries: DataOperation.Query[] = [];

		for (const query of queries) {
			if (!DataOperation.ListDocuments.Query.isAssignableFrom(query)) {
				modifiedQueries.push(query);
				continue;
			}

			let pathToA12wfGroup: string | undefined;
			const isCdm = CdmUtils.checkIfDocumentModelIsCDM(documentModel as DocumentModel);
			if (isCdm) {
				const rootDocumentModel = yield* CdmUtils.loadRootDocumentModelOfCDM(
					documentModel as DocumentModel,
					activityId
				);
				if (rootDocumentModel) {
					pathToA12wfGroup = findPathToA12wfGroup(rootDocumentModel);
				}
			} else {
				pathToA12wfGroup = findPathToA12wfGroup(documentModel as DocumentModel);
			}

			if (!pathToA12wfGroup) {
				const msg = `Could not find path to A12WF group in document model ${documentModel.header.id}.`;
				yield* put(ActivityActions.error({ operationType: "loading", activityId, error: msg }));
				return [];
			}

			const modifiedQuery = injectTaskIdNotEmptyConstraint(pathToA12wfGroup, query);
			modifiedQueries.push(modifiedQuery);
		}
		return yield* defaultBuildRequests({ ...params, queries: modifiedQueries });
	},
	*handleResponses(params: HandleResponsesParams): SagaGenerator<DataOperation.ResultSet> {
		return yield* defaultHandleResponses(params);
	}
};

function* isTaskList(activityId: string): SagaGenerator<boolean> {
	const activityDescriptor = yield* select(
		ActivitySelectors.activityPropById(activityId, (activity) => activity.descriptor)
	);
	return activityDescriptor !== undefined && activityDescriptor.view === WorkflowsConstants.TASK_LIST_VIEW;
}

function injectTaskIdNotEmptyConstraint(pathToA12wfGroup: string, query: DataOperation.ListDocuments.Query) {
	const pathToA12wfWithSlashes = pathToA12wfGroup.split(".").join("/");

	const taskIdExistsConstraint: Query.Operator = {
		operator: Query.OPERATORS.NOT_OPERATOR,
		operand: {
			operator: Query.OPERATORS.UNDEFINED_MATCH_OPERATOR,
			field: `/${pathToA12wfWithSlashes}/Task/ID`
		}
	};

	const fieldsProjectionForWf = [
		`/${pathToA12wfWithSlashes}/Task/ID`,
		`/${pathToA12wfWithSlashes}/TaskDefinition/FormKey`
	];

	const modifiedQuery: DataOperation.Query = {
		...query,
		fields: query.fields === undefined ? undefined : [...query.fields, ...fieldsProjectionForWf],
		constraint: query.constraint
			? {
					operator: Query.OPERATORS.AND_OPERATOR,
					operands: [query.constraint, taskIdExistsConstraint]
				}
			: taskIdExistsConstraint
	};
	return modifiedQuery;
}
