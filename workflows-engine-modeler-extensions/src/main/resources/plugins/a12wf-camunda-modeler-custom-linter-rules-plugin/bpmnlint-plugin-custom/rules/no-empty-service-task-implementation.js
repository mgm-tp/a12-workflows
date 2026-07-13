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
 * Rule that reports service tasks with empty topic or delegate expression
 */
module.exports = function() {

  function check(node, reporter) {
    if (is(node, 'bpmn:ServiceTask')) {

      if((node.type === 'external' && node.topic === '')) {
        reporter.report(node.id, 'Service Task implementation is missing topic');
      } else if(node.delegateExpression === '') {
        reporter.report(node.id, 'Service Task implementation is missing delegate expression');
      }
    }
  }

  return {
    check: check
  };
};
