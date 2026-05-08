package com.mgmtp.a12.workflows.service.listener

import org.springframework.context.events.EventListener
import org.springframework.stereotype.Component
import com.mgmtp.a12.workflows.service.event.TaskAfterUpdateEvent

@Component
class TaskAfterUpdateEventListener {

    @EventListener
    fun handleOnAfterUpdateEvent(event: TaskAfterUpdateEvent) {
        logger().info("Document has successfully been updated with data ${event.documentData.merged}")
    }
}
