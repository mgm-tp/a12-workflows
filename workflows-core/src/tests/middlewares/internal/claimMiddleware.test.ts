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

import { AuthenticationState, AuthenticationType, UaaSelectors } from "@com.mgmtp.a12.uaa/uaa-authentication-client";

import { claimMiddleware } from "../../../middlewares/internal/claimMiddleware.js";
import { WorkflowsActions } from "../../../actions/index.js";
import { getStore } from "../../testUtils/testUtils.js";

const MIDDLEWARES = [claimMiddleware];
const username = "someUserName";

const INITIAL_STATE_WITH_UNDEFINED_USER = {
	name: "getState"
};

const INITIAL_STATE_WITH_EMPTY_USERNAME = {
	name: "getState",
	uaa: {
		user: {
			username: "",
			displayName: ""
		},
		state: AuthenticationState.AUTHENTICATED,
		authenticationType: AuthenticationType.LOCAL
	} satisfies UaaSelectors.UaaLoggedInSlice
};

const INITIAL_STATE_WITH_UAA_SLICE = {
	name: "getState",
	uaa: {
		user: {
			username: username,
			displayName: username
		},
		state: AuthenticationState.AUTHENTICATED,
		authenticationType: AuthenticationType.LOCAL
	} satisfies UaaSelectors.UaaLoggedInSlice
};

const claimRequestAction = WorkflowsActions.claimRequested({ docRef: "someDocRef" });

describe(`claimMiddleware tests`, () => {
	it(`should dispatch ASSIGN_TASK action`, () => {
		const store = getStore(MIDDLEWARES, INITIAL_STATE_WITH_UAA_SLICE);

		store.dispatch(claimRequestAction);

		const actions = store.getActions();
		expect(actions).toHaveLength(2);
		expect(actions[0].type).toStrictEqual(WorkflowsActions.assignTask.started.type);
		expect(actions[1].type).toStrictEqual(WorkflowsActions.claimRequested.type);
	});
	it.each([
		[undefined, INITIAL_STATE_WITH_UNDEFINED_USER],
		["an empty string", INITIAL_STATE_WITH_EMPTY_USERNAME]
	])(
		`should not dispatch ASSIGN_TASK action if username is %s`,
		(printedValueForUserName: string | undefined, INITIAL_STATE: object) => {
			const store = getStore(MIDDLEWARES, INITIAL_STATE);

			expect(() => {
				store.dispatch(claimRequestAction);
			}).toThrow();
			const actions = store.getActions();
			expect(actions).toHaveLength(0);
		}
	);
});
