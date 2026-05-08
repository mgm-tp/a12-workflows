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

import jsonPath from "jsonpath";

import { Activity } from "@com.mgmtp.a12.client/client-core/lib/core/activity/index.js";
import { DocumentModel } from "@com.mgmtp.a12.kernel/kernel-md-facade/lib/main/js/api.js";
import { DocumentModelSearchServiceImpl } from "@com.mgmtp.a12.kernel/kernel-md-facade/lib/main/js/internal/DocumentModelSearchServiceImpl.js";

import { WorkflowsConstants } from "../../constants.js";

export function isTaskOverview(activity: Activity) {
	return activity.descriptor.view === WorkflowsConstants.TASK_LIST_VIEW;
}

export function findA12wfObject(object: object): object {
	return jsonPath.value(object, "$..A12WF");
}

export function findPathToA12wfGroup(documentModel: DocumentModel): string | undefined {
	const a12wfGroupId = findA12wfGroupId(documentModel.content.modelRoot);
	if (!a12wfGroupId) {
		return undefined;
	}

	const documentModelSearchService = new DocumentModelSearchServiceImpl(documentModel);

	const modelPathToA12wfGroup = documentModelSearchService.getPathById(a12wfGroupId);
	if (!modelPathToA12wfGroup) {
		return undefined;
	}

	let pathToA12wfGroup = "";
	modelPathToA12wfGroup.forEach((element) => {
		pathToA12wfGroup = pathToA12wfGroup + `.${element.elementName}`;
	});
	// Remove first dot in path
	return pathToA12wfGroup.substring(1);
}

function findA12wfGroupId(element: DocumentModel.Group): string | undefined {
	const a12wfGroupElement = element.elements.find((element) => element.name === "A12WF");
	if (a12wfGroupElement) {
		return a12wfGroupElement.id;
	}

	for (const child of element.elements) {
		if (child.type === "Group") {
			const a12wfGroupId = findA12wfGroupId(child);
			return a12wfGroupId;
		}
	}
	return undefined;
}
