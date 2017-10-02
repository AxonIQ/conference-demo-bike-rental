package com.example.bikerental;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmqpConfiguration {

    @Bean
    public Exchange eventsExchange() {
        return ExchangeBuilder.fanoutExchange("Events").build();
    }

    @Bean
    public Queue eventsQueue() {
        return QueueBuilder.durable("Events").build();
    }

    @Bean
    public Binding eventsBinding() {
        return BindingBuilder.bind(eventsQueue()).to(eventsExchange()).with("*").noargs();
    }

    @Autowired
    public void configure(AmqpAdmin admin) {
        admin.declareExchange(eventsExchange());
        admin.declareQueue(eventsQueue());
        admin.declareBinding(eventsBinding());
    }


}
