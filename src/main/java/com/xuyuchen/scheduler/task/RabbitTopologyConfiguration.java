package com.xuyuchen.scheduler.task;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "scheduler.messaging", havingValue = "rabbit")
public class RabbitTopologyConfiguration {
    @Bean
    TopicExchange schedulerEventsExchange() { return new TopicExchange("scheduler.events", true, false); }

    @Bean
    Queue schedulerEventsQueue() { return new Queue("scheduler.events.v1", true, false, false); }

    @Bean
    Binding schedulerEventsBinding(Queue schedulerEventsQueue, TopicExchange schedulerEventsExchange) {
        return BindingBuilder.bind(schedulerEventsQueue).to(schedulerEventsExchange).with("#");
    }

    @Bean
    TopicExchange workerCommandsExchange() { return new TopicExchange("scheduler.worker.commands", true, false); }

    @Bean
    Queue workerCommandsQueue() { return new Queue("scheduler.worker.commands.v1", true, false, false); }

    @Bean
    Binding workerCommandsBinding(Queue workerCommandsQueue, TopicExchange workerCommandsExchange) {
        return BindingBuilder.bind(workerCommandsQueue).to(workerCommandsExchange).with("#");
    }
}
