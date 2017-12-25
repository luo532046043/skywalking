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

import org.skywalking.apm.commons.datacarrier.common.AtomicRangeInteger;

import java.util.LinkedList;

/**
 * 缓存区
 *
 * Created by wusheng on 2016/10/25.
 */
public class Buffer<T> {

    /**
     * 缓存数组
     */
    private final Object[] buffer;
    /**
     * 缓冲策略
     */
    private BufferStrategy strategy;
    /**
     * 递增位置
     */
    private AtomicRangeInteger index;

    Buffer(int bufferSize, BufferStrategy strategy) {
        buffer = new Object[bufferSize];
        this.strategy = strategy;
        index = new AtomicRangeInteger(0, bufferSize);
    }

    void setStrategy(BufferStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * 保存数据
     *
     * @param data 数据
     * @return 是否保存成功
     */
    boolean save(T data) {
        int i = index.getAndIncrement();
        if (buffer[i] != null) {
            switch (strategy) {
                case BLOCKING:
                    // 等待
                    while (buffer[i] != null) {
                        try {
                            Thread.sleep(1L);
                        } catch (InterruptedException e) {
                        }
                    }
                    break;
                case IF_POSSIBLE:
                    return false;
                case OVERRIDE:
                default:
            }
        }
        buffer[i] = data;
        return true;
    }

    public int getBufferSize() {
        return buffer.length;
    }

    public LinkedList<T> obtain(int start, int end) {
        LinkedList<T> result = new LinkedList<T>();
        for (int i = start; i < end; i++) {
            if (buffer[i] != null) {
                result.add((T)buffer[i]);
                buffer[i] = null;
            }
        }
        return result;
    }

}
