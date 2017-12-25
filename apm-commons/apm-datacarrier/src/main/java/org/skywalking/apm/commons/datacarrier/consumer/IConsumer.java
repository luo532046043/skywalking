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

import java.util.List;

/**
 * 消费者接口
 *
 * Created by wusheng on 2016/10/25.
 */
public interface IConsumer<T> {

    /**
     * 初始化
     */
    void init();

    /**
     * 批量消费消息
     *
     * @param data 消息数组
     */
    void consume(List<T> data);

    /**
     * 处理当消费发生异常
     *
     * @param data 消息
     * @param t 异常
     */
    void onError(List<T> data, Throwable t);

    /**
     * 处理当消费结束。此处的结束时，ConsumerThread 关闭。
     */
    void onExit();

}
