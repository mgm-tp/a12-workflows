package com.mgmtp.a12.workflows.service.listener

import com.mgmtp.a12.workflows.service.event.TaskAfterCompleteEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class TaskAfterCompleteEventListener {

    @EventListener
    fun handleAfterCompleteEvent(event: TaskAfterCompleteEvent) {
        logger().info("Task has successfully been completed with data ${event.documentData.merged}")
    }
}
