package com.mgmtp.a12.workflows.service.listener

import com.mgmtp.a12.workflows.service.events.TaskListBeforeReadEvent
import com.mgmtp.a12.workflows.utils.logger
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class TaskListBeforeReadEventListener {

    @EventListener
    fun handleTaskListBeforeReadEvent(event: TaskListBeforeReadEvent) {
        logger().info("About to read list of task documents")
    }
}
