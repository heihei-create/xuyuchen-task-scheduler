package com.xuyuchen.scheduler.task;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "scheduler.worker-messaging", havingValue = "rabbit")
public class RabbitWorkerCommandPublisher implements WorkerCommandPublisher {
    private final RabbitTemplate rabbitTemplate;
    public RabbitWorkerCommandPublisher(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }
    @Override public void dispatch(WorkerCommand command) {
        rabbitTemplate.convertAndSend("scheduler.worker.commands", command.workerId(), command, message -> {
            message.getMessageProperties().setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setHeader("tenantId", command.tenantId());
            return message;
        });
    }
}
