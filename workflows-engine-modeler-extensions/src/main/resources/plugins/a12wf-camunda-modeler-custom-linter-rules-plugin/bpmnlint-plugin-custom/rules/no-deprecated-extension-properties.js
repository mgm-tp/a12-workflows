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
 * Rule that reports task/process having deprecated extension properties for old architecture
 */
module.exports = function() {

  function check(node, reporter) {
    if (is(node, 'bpmn:Task') || is(node, 'bpmn:Process')) {

      // names of extension properties required for old architecture
      const oldExtensionPropertyNames = ['taskDocument', 'inputDocumentModel', 'documentModel', 'documentModelName']

      let hasOldExtensionProperty = false

      if (node.extensionElements !== undefined) {
        const extensionElements = node.extensionElements.values || []

        // extensionElements in a node can be parameters, extension properties or camunda form refs without specific order
        extensionElements.forEach(
            extensionElement => {

              var extensionValues = extensionElement.values || []

              // if extensionValues are given, the current extensionElement is extension properties
              if (extensionValues.length > 0) {

                extensionValues.forEach(
                    extensionValue => {

                      if(oldExtensionPropertyNames.includes(extensionValue.name)) {
                        hasOldExtensionProperty = true
                      }
                    }
                )
              }
            }
        )

        if (hasOldExtensionProperty) {
          reporter.report(node.id, 'Deprecated extension properties for old A12WF architecture found');
        }
      }
    }
  }

  return {
    check: check
  };
};
