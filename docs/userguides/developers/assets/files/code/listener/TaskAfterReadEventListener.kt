package com.mgmtp.a12.workflows.service.listener

import com.mgmtp.a12.workflows.service.events.TaskAfterReadEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class TaskAfterReadEventListener {

    @EventListener
    fun handleTaskAfterReadEvent(event: TaskAfterReadEvent) {
        val documentModelName = event.documentData.model.header.id
        logger().info("Successfully read task document of model $documentModelName")
    }
}
