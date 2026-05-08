/*
 * LICENSE
 * SPDX-License-Identifier: EUPL-1.2 OR LicenseRef-commercial
 *
 * mgm A12 Platform – https://www.mgm-tp.com
 * Copyright (c) 2012–2026 mgm technology partners GmbH
 *
 * This software is developed by mgm technology partners GmbH and
 * is available under a choice of two different licenses:
 *
 * 1. Open-Source License – EUPL v1.2
 *    You may redistribute and/or modify this software under the
 *    terms of the European Union Public Licence, version 1.2.
 *    The license text is available at https://eupl.eu/
 *    and included in this distribution as:
 *        licenses/EUPL-1.2.txt
 *
 * 2. Commercial License
 *    Alternatively, you may obtain a commercial license from
 *    mgm technology partners GmbH, that permits use of this
 *    software under different terms (including support and
 *    maintenance options). For commercial licensing, please contact:
 *        a12-license@mgm-tp.com
 *
 * You must select and comply with exactly one of the above
 * licensing options.
 *
 * Third-Party Components
 * ----------------------
 * This software includes third-party components.
 * Their licenses are provided in:
 *    THIRD_PARTY_NOTICES
 *    licenses/   (folder with full license texts)
 *
 * Trademarks
 * ----------
 * “mgm A12” is a registered trademark of mgm technology partners GmbH.
 * The A12 logo is protected by copyright.
 *
 * Warranty Disclaimer (applies to either option)
 * ----------------------------------------------
 * THIS SOFTWARE IS PROVIDED “AS IS” AND WITHOUT WARRANTY OF ANY KIND,
 * WHETHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT,
 * EXCEPT WHERE SUCH DISCLAIMERS ARE NOT PERMITTED BY APPLICABLE LAW.
 * SEE THE RESPECTIVE LICENSE TEXT FOR DETAILS.
 *
 * End of LICENSE.
 */
const {
  is
} = require('bpmnlint-utils');


/**
 * Rule that reports user tasks which do not have an input or output (required for A12WF) defined
 */
module.exports = function() {

  function check(node, reporter) {
    if (is(node, 'bpmn:UserTask')) {

      const docRefInputName = 'documentReference'
      const docRefOutputValue = '${newDocRef}'
      let docRefInputFound = false
      let docRefOutputFound = false
      let inputParameters = []
      let outputParameters = []

      if(node.extensionElements !== undefined) {

          const extensionElements = node.extensionElements.values || []

          // extensionElements in a node can be parameters, extension properties or camunda form refs without specific order
          extensionElements.forEach(
              extensionElement => {

                var extensionValues = extensionElement.values || []

                // if extensionValues are given, the current extensionElement would be extension properties, not parameters
                if (extensionValues.length === 0) {
                    inputParameters = extensionElement.inputParameters || []
                    outputParameters = extensionElement.outputParameters || []
                }
              }
          )

          inputParameters.forEach(
              inputParameter => {

                if(inputParameter.name === docRefInputName) {
                  docRefInputFound = true
                }
              }
          )

          outputParameters.forEach(
              outputParameter => {

                if(outputParameter.value === docRefOutputValue) {
                  docRefOutputFound = true
                }
              }
          )
      }

      if(!docRefInputFound && !docRefOutputFound) {
          reporter.report(node.id, 'User tasks for A12WF should define either ' + docRefInputName + ' as input or an output for ' + docRefOutputValue)
      }
      else if(docRefInputFound && docRefOutputFound) {
          reporter.report(node.id, 'User tasks for A12WF should not define both input for ' + docRefInputName + ' and output for ' + docRefOutputValue)
      }
    }
  }

  return {
    check: check
  };
};
