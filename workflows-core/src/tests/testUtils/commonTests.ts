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

import { actionCreatorFactory, Action, AnyAction, Failure, Success } from "typescript-fsa/lib/index.js";
import { expectSaga } from "redux-saga-test-plan";
import { StaticProvider, throwError } from "redux-saga-test-plan/providers.js";
import { call } from "redux-saga-test-plan/matchers.js";

import { ApplicationSaga } from "@com.mgmtp.a12.client/client-core/lib/core/application/index.js";

import { dispatchJsonRpcRequest, dispatchJsonRpcRequests } from "../../utils/internal/requests/requestUtils.js";

const dummyAction = actionCreatorFactory().async("DUMMY");
const positiveResponse = {
	ok: true
};

export function testCanHandle(saga: ApplicationSaga.Descriptor, correctActions: Array<AnyAction>) {
	for (const correctAction of correctActions) {
		it(`returns true for ${correctAction.type}`, () => {
			expect(saga.canHandle({}, correctAction)).toBe(true);
		});
	}
	it("returns false for other actions", () => {
		expect(saga.canHandle({}, dummyAction)).toBe(false);
	});
}

export function testPositiveResponse(
	saga: ApplicationSaga.Descriptor,
	action: Action<object>,
	expectedDispatch: Action<Success<object, object>>,
	additionalProviders: Array<StaticProvider> = [],
	additionalActions: Array<{ type: string }> = []
) {
	it(`dispatches ${expectedDispatch.type} if response is positive`, () => {
		const expectedSaga = expectSaga(saga.handle, action);

		// Before running the saga to be tested, additional actions the saga expects are dispatched to take them in the order it takes them.
		// Internally, expectSaga will queue the actions and dispatch as needed on occurring take effects.
		// https://redux-saga-test-plan.jeremyfairbank.com/integration-testing/dispatching.html
		additionalActions.forEach(expectedSaga.dispatch);

		return expectedSaga
			.provide([
				[call.fn(dispatchJsonRpcRequest), positiveResponse],
				[call.fn(dispatchJsonRpcRequests), positiveResponse],
				...additionalProviders
			])
			.put(expectedDispatch)
			.run();
	});
}

export function testNegativeResponse(
	saga: ApplicationSaga.Descriptor,
	action: Action<object>,
	expectedDispatch: Action<Failure<object, object>>,
	additionalProviders: Array<StaticProvider> = [],
	additionalActions: Array<{ type: string }> = []
) {
	it(`dispatches ${expectedDispatch.type} if response is negative`, () => {
		const expectedSaga = expectSaga(saga.handle, action);

		additionalActions.forEach(expectedSaga.dispatch);

		return expectedSaga
			.provide([
				[call.fn(dispatchJsonRpcRequest), throwError(new Error(""))],
				[call.fn(dispatchJsonRpcRequests), throwError(new Error(""))],
				...additionalProviders
			])
			.put.actionType(expectedDispatch.type)
			.run();
	});
}
