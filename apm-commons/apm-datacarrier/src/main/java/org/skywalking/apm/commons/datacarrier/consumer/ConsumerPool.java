/*
 * Copyright 2017, OpenSkywalking Organization All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project repository: https://github.com/OpenSkywalking/skywalking
 */

package org.skywalking.apm.commons.datacarrier.consumer;

import org.skywalking.apm.commons.datacarrier.buffer.Buffer;
import org.skywalking.apm.commons.datacarrier.buffer.Channels;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 消费者池
 *
 * Pool of consumers
 * <p>
 * Created by wusheng on 2016/10/25.
 */
public class ConsumerPool<T> {

    /**
     * 是否运行中
     */
    private boolean running;
    /**
     * 线程数组
     */
    private ConsumerThread[] consumerThreads;
    /**
     * 通道
     */
    private Channels<T> channels;
    /**
     * 锁
     */
    private ReentrantLock lock;

    public ConsumerPool(Channels<T> channels, Class<? extends IConsumer<T>> consumerClass, int num) {
        this(channels, num);
        for (int i = 0; i < num; i++) {
            consumerThreads[i] = new ConsumerThread("DataCarrier.Consumser." + i + ".Thread", getNewConsumerInstance(consumerClass));
            consumerThreads[i].setDaemon(true);
        }
    }

    public ConsumerPool(Channels<T> channels, IConsumer<T> prototype, int num) {
        this(channels, num);
        prototype.init();
        for (int i = 0; i < num; i++) {
            consumerThreads[i] = new ConsumerThread("DataCarrier.Consumser." + i + ".Thread", prototype);
            consumerThreads[i].setDaemon(true);
        }

    }

    private ConsumerPool(Channels<T> channels, int num) {
        running = false;
        this.channels = channels;
        consumerThreads = new ConsumerThread[num];
        lock = new ReentrantLock();
    }

    /**
     * 创建消费者对象，并初始化
     *
     * @param consumerClass 消费者类
     * @return 消费者对象
     */
    private IConsumer<T> getNewConsumerInstance(Class<? extends IConsumer<T>> consumerClass) {
        try {
            IConsumer<T> inst = consumerClass.newInstance();
            inst.init();
            return inst;
        } catch (InstantiationException e) {
            throw new ConsumerCannotBeCreatedException(e);
        } catch (IllegalAccessException e) {
            throw new ConsumerCannotBeCreatedException(e);
        }
    }

    public void begin() {
        if (running) {
            return;
        }
        try {
            lock.lock();

            // 分配 Buffer 给 Thread
            this.allocateBuffer2Thread();

            // 启动消费线程
            for (ConsumerThread consumerThread : consumerThreads) {
                consumerThread.start();
            }

            // 标记 运行中
            running = true;
        } finally {
            lock.unlock();
        }
    }

    public boolean isRunning() {
        return running;
    }

    private void allocateBuffer2Thread() {
        int channelSize = this.channels.getChannelSize();
        if (channelSize < consumerThreads.length) {
            /**
             * if consumerThreads.length > channelSize
             * each channel will be process by several consumers.
             */
            ArrayList<Integer>[] threadAllocation = new ArrayList[channelSize];
            for (int threadIndex = 0; threadIndex < consumerThreads.length; threadIndex++) {
                int index = threadIndex % channelSize;
                if (threadAllocation[index] == null) {
                    threadAllocation[index] = new ArrayList<Integer>();
                }
                threadAllocation[index].add(threadIndex);
            }

            for (int channelIndex = 0; channelIndex < channelSize; channelIndex++) {
                ArrayList<Integer> threadAllocationPerChannel = threadAllocation[channelIndex];
                Buffer<T> channel = this.channels.getBuffer(channelIndex);
                int bufferSize = channel.getBufferSize();
                int step = bufferSize / threadAllocationPerChannel.size();
                for (int i = 0; i < threadAllocationPerChannel.size(); i++) {
                    int threadIndex = threadAllocationPerChannel.get(i);
                    int start = i * step;
                    int end = i == threadAllocationPerChannel.size() - 1 ? bufferSize : (i + 1) * step;
                    consumerThreads[threadIndex].addDataSource(channel, start, end);
                }
            }
        } else {
            /**
             * if consumerThreads.length < channelSize
             * each consumer will process several channels.
             *
             * if consumerThreads.length == channelSize
             * each consumer will process one channel.
             */
            for (int channelIndex = 0; channelIndex < channelSize; channelIndex++) {
                int consumerIndex = channelIndex % consumerThreads.length;
                consumerThreads[consumerIndex].addDataSource(channels.getBuffer(channelIndex));
            }
        }

    }

    public void close() {
        try {
            lock.lock();
            this.running = false;
            for (ConsumerThread consumerThread : consumerThreads) {
                consumerThread.shutdown();
            }
        } finally {
            lock.unlock();
        }
    }
}
