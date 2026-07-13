/*
 * (c) copyright 2012-2026 mgm technology partners GmbH.
 * This software, the underlying source code and other artifacts are protected by copyright.
 * All rights, in particular the right to use, reproduce, publish and edit are reserved.
 * A simple right of use (license) can be acquired for use, duplication, publication, editing etc..
 * Requests for this can be made at A12-license@mgm-tp.com or other official channels of the copyright holder.
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
