/*
 *
 *  * Copyright - TrianaCloud
 *  * Copyright (C) 2012. Kieran Evans. All Rights Reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU General Public License
 *  * as published by the Free Software Foundation; either version 2
 *  * of the License, or (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.trianacode.TrianaCloud.Worker;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.io.File;
import java.net.URL;

/*
 * The worker looks for tasks, and executes them. Easy.
 */

public class Worker {
    private static final String RPC_QUEUE_NAME = "task_dispatch";

    public static void main(String[] argv) {
        Connection connection = null;
        Channel channel;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("s-vmc.cs.cf.ac.uk");
            factory.setPort(7000);
            factory.setUsername("trianacloud");
            factory.setPassword("trianacloud");

            connection = factory.newConnection();
            channel = connection.createChannel();

            //Use tc_exchange (TrianaCloud_exchange) in the topic config.
            ///TODO:Grab this from argv or config file
            channel.exchangeDeclare("tc_exchange", "topic");

            /*
            "dart.triana" is the queue name. If the task is covered by multiple topic bindings, it's duplicated. If
            there are multiple queues for a topic binding, it's duplicated. This could be useful for debugging, logging
            and auditing. This could also be used to implement validation, which inherently ensures that the two
            (or more) duplicate tasks to compare and validate aren't executed on the same machine. But for a standard
            single worker, single task config, follow the rule of thumb.

            Rule of thumb: if the topic binding is the same, the queue name should be the same.
            If you're using wildcards in a topic name, MAKE SURE other topics/queues don't collide.

            e.g.:
                BAD:
                    dart.triana on worker1
                    #.triana on worker2

                GOOD:
                    dart.eddie.triana   (this will grab those with this exact topic)
                    *.triana            (* substitutes exactly one word. dart.eddie.triana ISN'T caught)
            */

            String routingKey = "*.triana";

            System.out.println(" [x] Routing Key: " + routingKey);

            ///TODO:Grab from argv or config file
            String queueName = channel.queueDeclare(routingKey, true, false, true, null).getQueue();
            channel.queueBind(queueName, "tc_exchange", routingKey);

            //Makes sure tasks are shared properly, this tells rabbit to only grab one message at a time.
            channel.basicQos(1);

            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(queueName, false, consumer);

            System.out.println(" [x] Loading Plugins");

            ClassLoader classLoader = Worker.class.getClassLoader();

            ///TODO:Make sure there's not a better way to do this
            URL[] urls = new URL[1];
            ///TODO:Grab a plugin dir from the config file
            String workingDir = System.getProperty("user.dir");
            File f = new File(workingDir);
            urls[0] = f.toURI().toURL();
            //Load plugins using the fancy-pants loader hacked together using the code from iharvey and the intarwebs
            TaskExecutorLoader tel = new TaskExecutorLoader(urls, classLoader);

            System.out.println(" [x] Awaiting RPC requests");

            while (true) {
                String response = "";

                QueueingConsumer.Delivery delivery = consumer.nextDelivery();

                BasicProperties props = delivery.getProperties();
                BasicProperties replyProps = new BasicProperties
                        .Builder()
                        .correlationId(props.getCorrelationId())
                        .build();

                try {
                    ///TODO: Use metadata to figure out which Executor to use.
                    ///TODO: Figure out what wire-protocol to use. Something simple like ASN.1? Or a subset of it?

                    //String message = new String(delivery.getBody());
                    byte[] message = delivery.getBody();
                    TaskExecutor ex = tel.getExecutor("org.trianacode.TrianaCloud.TrianaTaskExecutor.Executor");

                    ex.setData(message);
                    response = new String(ex.executeTask());
                } catch (Exception e) {
                    ///TODO: filter the errors. Worker errors should skip the Ack, and allow the task to be redone.
                    ///TODO: Two new exeptions for the task executor, one to indicate that the execution failed due to
                    ///     the data, one to indicate some other error. The former would be ack'ed and sent back, as
                    ///     it's a user error (i.e. the data is bad). The latter would indicate any other errors (bad
                    ///     config, random error, missile strike).
                    System.out.println(" [.] " + e.toString());
                    e.printStackTrace();
                    response = "";
                } finally {
                    channel.basicPublish("", props.getReplyTo(), replyProps, response.getBytes("UTF-8"));
                    //Acknowledge that the task has been received. If a crash happens before here, then Rabbit automagically
                    //sticks the message back in the queue.
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignore) {
                }
            }
        }
    }
}
