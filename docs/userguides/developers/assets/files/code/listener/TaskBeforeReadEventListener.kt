package com.mgmtp.a12.workflows.service.listener

import com.mgmtp.a12.workflows.service.events.TaskBeforeReadEvent
import com.mgmtp.a12.workflows.utils.logger
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class TaskBeforeReadEventListener {

    @EventListener
    fun handleTaskBeforeReadEvent(event: TaskBeforeReadEvent) {
        val documentModelName = event.documentData.model.header.id
        logger().info("About to read task document of model $documentModelName")
    }
}
