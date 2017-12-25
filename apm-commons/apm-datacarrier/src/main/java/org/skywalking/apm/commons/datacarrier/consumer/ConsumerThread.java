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

import java.util.LinkedList;
import java.util.List;

/**
 * 消费线程
 *
 * Created by wusheng on 2016/10/25.
 */
public class ConsumerThread<T> extends Thread {

    /**
     * 是否运行中
     */
    private volatile boolean running;
    /**
     * 消费者
     */
    private IConsumer<T> consumer;
    /**
     * 数据源数组
     */
    private List<DataSource> dataSources;

    ConsumerThread(String threadName, IConsumer<T> consumer) {
        super(threadName);
        this.consumer = consumer;
        running = false;
        dataSources = new LinkedList<DataSource>();
    }

    /**
     * add partition of buffer to consume
     *
     * @param sourceBuffer
     * @param start
     * @param end
     */
    void addDataSource(Buffer<T> sourceBuffer, int start, int end) {
        this.dataSources.add(new DataSource(sourceBuffer, start, end));
    }

    /**
     * add whole buffer to consume
     *
     * @param sourceBuffer
     */
    void addDataSource(Buffer<T> sourceBuffer) {
        this.dataSources.add(new DataSource(sourceBuffer, 0, sourceBuffer.getBufferSize()));
    }

    @Override
    public void run() {
        running = true;

        // 不断消费，直到关闭
        while (running) {
            // 消费
            boolean hasData = consume();

            if (!hasData) { // 无数据，sleep 等待
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                }
            }
        }

        // 消费剩余部分
        // consumer thread is going to stop
        // consume the last time
        consume();

        consumer.onExit();
    }

    /**
     * 消费
     *
     * @return 是否消费了数据
     */
    private boolean consume() {
        boolean hasData = false;

        // 获得数据
        LinkedList<T> consumeList = new LinkedList<T>();
        for (DataSource dataSource : dataSources) {
            LinkedList<T> data = dataSource.obtain();
            if (data.size() == 0) {
                continue;
            }
            for (T element : data) {
                consumeList.add(element);
            }
            hasData = true;
        }

        // 若有数据，进行消费
        if (consumeList.size() > 0) {
            try {
                consumer.consume(consumeList);
            } catch (Throwable t) {
                consumer.onError(consumeList, t);
            }
        }
        return hasData;
    }

    void shutdown() {
        running = false;
    }

    /**
     * 数据源
     *
     * DataSource is a refer to {@link Buffer}.
     */
    class DataSource {
        private Buffer<T> sourceBuffer;
        private int start;
        private int end;

        DataSource(Buffer<T> sourceBuffer, int start, int end) {
            this.sourceBuffer = sourceBuffer;
            this.start = start;
            this.end = end;
        }

        LinkedList<T> obtain() {
            return sourceBuffer.obtain(start, end);
        }
    }

}
