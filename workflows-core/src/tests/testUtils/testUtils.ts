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

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import Path from "node:path";

import { configureStore } from "redux-mock-store";

import { Activity } from "@com.mgmtp.a12.client/client-core/lib/core/activity/index.js";
import { FormEngineActions } from "@com.mgmtp.a12.formengine/formengine-core/lib/client-extensions/index.js";
import { ModelPath } from "@com.mgmtp.a12.base/base-model-api/lib/main/model/index.js";
import { DocumentModel } from "@com.mgmtp.a12.kernel/kernel-md-facade/lib/main/js/api.js";
import { DocumentModelSerializerImpl } from "@com.mgmtp.a12.kernel/kernel-md-facade/lib/main/js/internal/DocumentModelSerializerImpl.js";

import FormEngineEventActions = FormEngineActions.FormEngineEventActions;

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function getStore(middlewares: any, initialState: object) {
	const mockStore = configureStore(middlewares);
	return mockStore(initialState);
}

interface TestActivityProps extends Partial<Activity> {
	readonly id: string;
	readonly activityDataHolder?: Partial<Activity.DataHolder>;
	readonly dirtyStateOfActivityDataHolder?: boolean;
}

export function createActivity(props: TestActivityProps): Activity {
	const {
		id,
		initiatingActivityId,
		descriptor,
		cancelConfirmationRequired,
		dataHolders,
		activationTimestamp,
		lock,
		activityDataHolder,
		dirtyStateOfActivityDataHolder
	} = props;

	const activityDescriptor = descriptor !== undefined ? descriptor : { model: "CRUD" };

	return {
		id,
		descriptor: activityDescriptor,
		activationTimestamp: activationTimestamp || 0,
		dataHolders:
			dataHolders == undefined || dataHolders.length === 0
				? [
						createDataHolder({
							descriptor: activityDescriptor,
							dirty: dirtyStateOfActivityDataHolder,
							...activityDataHolder
						})
					]
				: dataHolders,
		initiatingActivityId,
		cancelConfirmationRequired,
		lock
	};
}

function createDataHolder(options: Partial<Activity.DataHolder>): Activity.DataHolder {
	const { descriptor, data, datasourceActivityId, loadingState, savingState, dirty, busy, error, slices } = options;

	return {
		descriptor: descriptor || { model: "CRUD" },
		data: data || {},
		datasourceActivityId: datasourceActivityId || "someDatasourceActivityId",
		loadingState: loadingState || "loaded",
		savingState: savingState || "not_saved",
		dirty: dirty || false,
		busy: busy || false,
		error: error,
		slices: slices || {}
	};
}

export function createEventButtonPayload(
	activityId: string,
	eventName: string,
	type: string,
	buttonPath: ModelPath = [{ elementName: "" }]
): FormEngineEventActions {
	return {
		engineEvent: {
			type: type,
			payload: {
				name: eventName,
				validate: false,
				buttonPath: buttonPath
			}
		},
		activityId: activityId
	};
}

// Workaround/Hack for the time being, since import.meta.dirname does not work.
const dirname = Path.dirname(fileURLToPath(import.meta.url));
export function readFile(relativeFilePath: string): string {
	return fs.readFileSync(path.resolve(dirname, relativeFilePath), {
		encoding: "utf8"
	});
}

export function deserializeDocumentModel(documentModelString: string): DocumentModel {
	const documentModelSerializerImpl = new DocumentModelSerializerImpl();
	return documentModelSerializerImpl.deserialize(documentModelString);
}
