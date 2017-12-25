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

package org.skywalking.apm.commons.datacarrier.buffer;

import org.skywalking.apm.commons.datacarrier.partition.IDataPartitioner;

/**
 * 多 Buffer 的通道
 *
 * Channels of Buffer
 * It contais all buffer data which belongs to this channel.
 * It supports several strategy when buffer is full. The Default is BLOCKING
 * <p>
 * Created by wusheng on 2016/10/25.
 */
public class Channels<T> {

    /**
     * Buffer 数组
     */
    private final Buffer<T>[] bufferChannels;
    private IDataPartitioner<T> dataPartitioner;
    /**
     * 缓冲策略
     */
    private BufferStrategy strategy;

    /**
     * 创建 Channels
     *
     * @param channelSize 通道数量
     * @param bufferSize 缓冲区大小
     * @param partitioner 数据分配者对象
     * @param strategy 缓冲策略
     */
    public Channels(int channelSize, int bufferSize, IDataPartitioner<T> partitioner, BufferStrategy strategy) {
        this.dataPartitioner = partitioner;
        this.strategy = strategy;
        // 创建 Buffer 数组
        bufferChannels = new Buffer[channelSize];
        for (int i = 0; i < channelSize; i++) {
            bufferChannels[i] = new Buffer<T>(bufferSize, strategy);
        }
    }

    public boolean save(T data) {
        int index = dataPartitioner.partition(bufferChannels.length, data);

        // 计算最大重试次数
        int retryCountDown = 1;
        if (BufferStrategy.IF_POSSIBLE.equals(strategy)) {
            int maxRetryCount = dataPartitioner.maxRetryCount();
            if (maxRetryCount > 1) {
                retryCountDown = maxRetryCount;
            }
        }

        // 多次重试，保存数据直到成功
        for (; retryCountDown > 0; retryCountDown--) {
            if (bufferChannels[index].save(data)) {
                return true;
            }
        }
        return false;
    }

    public void setPartitioner(IDataPartitioner<T> dataPartitioner) {
        this.dataPartitioner = dataPartitioner;
    }

    /**
     * override the strategy at runtime. Notice, this will override several channels one by one. So, when running
     * setStrategy, each channel may use different BufferStrategy
     *
     * @param strategy
     */
    public void setStrategy(BufferStrategy strategy) {
        for (Buffer<T> buffer : bufferChannels) {
            buffer.setStrategy(strategy);
        }
    }

    /**
     * get channelSize
     *
     * @return
     */
    public int getChannelSize() {
        return this.bufferChannels.length;
    }

    public Buffer<T> getBuffer(int index) {
        return this.bufferChannels[index];
    }
}
