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

package org.skywalking.apm.collector.stream.worker.impl;

import org.skywalking.apm.collector.core.data.Data;
import org.skywalking.apm.collector.core.module.ModuleManager;
import org.skywalking.apm.collector.stream.worker.base.AbstractLocalAsyncWorker;
import org.skywalking.apm.collector.stream.worker.base.WorkerException;
import org.skywalking.apm.collector.stream.worker.impl.data.DataCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author peng-yongsheng
 */
public abstract class AggregationWorker<INPUT extends Data, OUTPUT extends Data> extends AbstractLocalAsyncWorker<INPUT, OUTPUT> {

    private final Logger logger = LoggerFactory.getLogger(AggregationWorker.class);

    private DataCache dataCache;
    private int messageNum;

    public AggregationWorker(ModuleManager moduleManager) {
        super(moduleManager);
        this.dataCache = new DataCache();
    }

    @Override protected final void onWork(INPUT message) throws WorkerException {
        messageNum++;

        // 聚合消息到数据
        aggregate(message);

        // 满足消息到达一定量，提交数据给 Next
        if (messageNum >= 100) {
            sendToNext();
            messageNum = 0;
        }

        // 当消息是批处理的最后一条时，提交数据给 Next
        if (message.isEndOfBatch()) {
            sendToNext();
        }
    }

    private void sendToNext() throws WorkerException {
        // 切换数据指针，并标记原指向正在读取中
        dataCache.switchPointer();
        // 等待原指向不在读取中
        while (dataCache.getLast().isWriting()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new WorkerException(e.getMessage(), e);
            }
        }
        // 提交数据给 Next
        dataCache.getLast().collection().forEach((String id, Data data) -> {
            logger.debug(data.toString());
            onNext((OUTPUT)data);
        });
        // 标记原指向完成读取
        dataCache.finishReadingLast();
    }

    /**
     * 聚合消息到数据
     *
     * 逻辑同 {@link PersistenceWorker#aggregate(Object)}
     *
     * @param message 消息
     */
    private void aggregate(INPUT message) {
        // 标记数据指针正在写入中
        dataCache.writing();
        // 写入
        if (dataCache.containsKey(message.getId())) {
            dataCache.get(message.getId()).mergeData(message);
        } else {
            dataCache.put(message.getId(), message);
        }
        // 标记数据指针完成写入
        dataCache.finishWriting();
    }
}
