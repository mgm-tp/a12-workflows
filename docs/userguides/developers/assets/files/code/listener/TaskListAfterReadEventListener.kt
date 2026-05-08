package com.mgmtp.a12.workflows.service.listener

import com.mgmtp.a12.workflows.service.events.TaskListAfterReadEvent
import com.mgmtp.a12.workflows.utils.logger
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class TaskListAfterReadEventListener {

    @EventListener
    fun handleTaskListAfterReadEvent(event: TaskListAfterReadEvent) {
        logger().info("Successfully read list of task documents")
    }
}
