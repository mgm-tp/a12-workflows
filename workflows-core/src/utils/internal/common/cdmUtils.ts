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

import { call, put, SagaGenerator } from "typed-redux-saga";

import { DocumentModel } from "@com.mgmtp.a12.kernel/kernel-md-facade/lib/main/js/api.js";
import { ActivityActions } from "@com.mgmtp.a12.client/client-core/lib/core/activity/index.js";
import { Selector, StoreSagas } from "@com.mgmtp.a12.client/client-core/lib/core/store/index.js";
import { Model as ModelApi } from "@com.mgmtp.a12.base/base-model-api/lib/main/model/index.js";
import { ModelSelectors } from "@com.mgmtp.a12.client/client-core/lib/core/model/index.js";

export namespace CdmUtils {
	export const CDM_ANNOTATION_NAME = "cdm.queryRoot";

	export function checkIfDocumentModelIsCDM(documentModel: DocumentModel): boolean {
		return documentModel.header.annotations?.some((it) => it.name == CDM_ANNOTATION_NAME && it.value != null) ?? false;
	}

	export function removeCdmNameFromRef(docRef: string): string {
		return docRef.split("/").slice(1).join("/");
	}

	export function* loadRootDocumentModelOfCDM(
		cdm: DocumentModel,
		activityId: string
	): SagaGenerator<DocumentModel | undefined> {
		const rootDocumentModelName = cdm.header.annotations?.find((it) => it.name == CdmUtils.CDM_ANNOTATION_NAME)?.value;

		if (!rootDocumentModelName) {
			const msg = `Failed to load root document model for CDM ${cdm.header.id}.`;
			yield* put(ActivityActions.error({ operationType: "loading", activityId: activityId, error: msg }));
			return undefined;
		}
		const rootDocumentModel = yield* call(StoreSagas.waitForStateChange, modelLoadedByName(rootDocumentModelName));
		return rootDocumentModel as DocumentModel;
	}

	function modelLoadedByName(
		modelName: string
	): Selector<{ stateChanged: boolean; returnValue: ModelApi | undefined }> {
		return (state) => {
			const model = ModelSelectors.modelByName(modelName)(state);
			if (model === undefined) {
				const error = ModelSelectors.modelErrorByName(modelName)(state);
				return { stateChanged: error !== undefined, returnValue: undefined };
			}

			return { stateChanged: true, returnValue: model };
		};
	}
}
