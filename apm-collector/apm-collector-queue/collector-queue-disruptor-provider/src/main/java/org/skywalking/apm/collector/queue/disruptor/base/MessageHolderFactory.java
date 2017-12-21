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

package org.skywalking.apm.collector.queue.disruptor.base;

import com.lmax.disruptor.EventFactory;
import org.skywalking.apm.collector.queue.base.MessageHolder;

/**
 * 消息持有者工厂
 *
 * @author peng-yongsheng
 */
public class MessageHolderFactory implements EventFactory<MessageHolder> {

    /**
     * 单例
     */
    public static MessageHolderFactory INSTANCE = new MessageHolderFactory();

    /**
     * 创建消息持有者
     *
     * @return MessageHolder 对象
     */
    public MessageHolder newInstance() {
        return new MessageHolder();
    }

}
