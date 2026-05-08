package com.mgmtp.a12.workflows.service.listener

import com.mgmtp.a12.workflows.service.events.TaskBeforeCompleteEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class TaskBeforeCompleteEventListener {

    @EventListener
    fun handleOnCompleteEvent(event: TaskBeforeCompleteEvent) {
        logger().info("Task with ID ${event.taskId} is about to be completed with data ${event.documentData.merged}")
    }
}
