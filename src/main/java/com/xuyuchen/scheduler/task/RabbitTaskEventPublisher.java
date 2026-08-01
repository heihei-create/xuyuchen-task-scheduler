package com.xuyuchen.scheduler.task;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "scheduler.messaging", havingValue = "rabbit")
public class RabbitTaskEventPublisher implements TaskEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    public RabbitTaskEventPublisher(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }
    @Override public void publish(TaskEvent event) {
        rabbitTemplate.convertAndSend("scheduler.events", event.type().name().toLowerCase(), event);
    }
}
