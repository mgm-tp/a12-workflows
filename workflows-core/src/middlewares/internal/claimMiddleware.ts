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

import { Middleware } from "redux";

import { LoggerFactory } from "@com.mgmtp.a12.utils/utils-logging/index.js";
import { UaaSelectors } from "@com.mgmtp.a12.uaa/uaa-authentication-client/lib/index.js";

import { WorkflowsActions } from "../../actions/index.js";

const logger = LoggerFactory.getLogger("claimMiddleware");

/**
 * Maps any claim action to an assignTask action in order to assign the current user
 */
export const claimMiddleware: Middleware = (api) => (next) => (action) => {
	if (WorkflowsActions.claimRequested.match(action)) {
		logger.info("Handling matched Workflows/CLAIM_REQUESTED action");
		logger.log(`Triggered by action:`, action);

		const username = UaaSelectors.username(api.getState());
		if (!username) {
			throw Error("Could not extract username from UAA user found in the store.");
		}

		logger.log(`Found current username: ${username}`);

		api.dispatch(
			WorkflowsActions.assignTask.started({
				docRef: action.payload.docRef,
				assignee: username
			})
		);
		logger.log("Finished handling Workflows/CLAIM_REQUESTED action");
	}
	return next(action);
};
