package com.github.kgraphql.publisher

import org.springframework.context.ApplicationListener
import org.springframework.util.ReflectionUtils
import reactor.core.publisher.FluxSink
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.function.Consumer

class EventCreatorPublisher(private var executor: Executor) : ApplicationListener<ApplicationCreatedEvent>,
        Consumer<FluxSink<ApplicationCreatedEvent>> {

    private val queue = LinkedBlockingQueue<ApplicationCreatedEvent>()

    override fun onApplicationEvent(event: ApplicationCreatedEvent) {
        this.queue.offer(event)
    }

    override fun accept(sink: FluxSink<ApplicationCreatedEvent>) {
        this.executor.execute {
            while (true)
                try {
                    val event = queue.take()
                    sink.next(event)
                } catch (e: InterruptedException) {
                    ReflectionUtils.rethrowRuntimeException(e)
                }
        }
    }
}