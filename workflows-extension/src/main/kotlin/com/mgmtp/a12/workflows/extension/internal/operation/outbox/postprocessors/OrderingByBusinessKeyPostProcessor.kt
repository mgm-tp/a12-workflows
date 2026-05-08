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
package com.mgmtp.a12.workflows.extension.internal.operation.outbox.postprocessors

import com.mgmtp.a12.workflows.consumer.internal.commands.OutboxCommand
import com.mgmtp.a12.workflows.extension.internal.operation.outbox.JmsConstants.JMS_GROUP_HEADER
import com.mgmtp.a12.workflows.extension.internal.operation.outbox.JmsConstants.TIMESTAMP_HEADER
import com.mgmtp.a12.workflows.extension.internal.utils.getNanosSinceEpoch
import com.mgmtp.a12.workflows.shared.logging.utils.logger
import jakarta.jms.Message
import org.springframework.jms.core.MessagePostProcessor

class OrderingByBusinessKeyPostProcessor : MessagePostProcessor {

    override fun postProcessMessage(message: Message): Message {
        val command = message.getBody(OutboxCommand::class.java)

        logger().debug("Started post processing outbox command {}", command)

        // Setting properties is required to assign command for a specific process / business key to the same JMS
        // message group, which guarantees ordering and load balancing: https://activemq.apache.org/message-groups.html
        message.setStringProperty(JMS_GROUP_HEADER, command.businessKey)
        message.setLongProperty(TIMESTAMP_HEADER, getNanosSinceEpoch())

        logger().debug("Successfully finished post processing outbox command {}", command)

        return message
    }
}
