/*
 * Copyright 2016 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

package rediverson;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.qpid.jms.JmsConnectionFactory;

/**
 * A simple Camel REST DSL route that implements the greetings service.
 * 
 */
@Component
public class Route extends RouteBuilder {

    @Autowired
    JmsConnectionFactory amqpConnectionFactory;
    @Bean
    public org.apache.camel.component.amqp.AMQPComponent amqpConnection() {
        org.apache.camel.component.amqp.AMQPComponent amqp = new org.apache.camel.component.amqp.AMQPComponent();
        amqp.setConnectionFactory(amqpConnectionFactory);
        return amqp;
    }

    @Override
    public void configure() throws Exception {

        // @formatter:off
        // errorHandler(loggingErrorHandler());
        errorHandler(deadLetterChannel("direct:errorLog"));
        
        from("direct:errorLog")
        .log(LoggingLevel.ERROR, simple("${exception.message}").getText())
        .log(LoggingLevel.ERROR, simple("${exception.stacktrace}").getText());

        from("timer://simpleTimer?period=5000").id("ToAMQP")
        .setBody(simple("Hello from timer at ${header.firedTime}"))
        .log("Sending message: ${body}")
        .to("amqp:myqueue")
        .onException(RuntimeException.class).log("Exception");
        
        from("amqp:myqueue").id("FromAMQP")
        .log("Message body from : ${body}")
        .onException(RuntimeException.class).log("Exception");


    }        
}