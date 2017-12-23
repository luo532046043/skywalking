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
import org.skywalking.apm.collector.core.util.ObjectUtils;
import org.skywalking.apm.collector.storage.StorageModule;
import org.skywalking.apm.collector.storage.base.dao.IBatchDAO;
import org.skywalking.apm.collector.storage.base.dao.IPersistenceDAO;
import org.skywalking.apm.collector.stream.worker.base.AbstractLocalAsyncWorker;
import org.skywalking.apm.collector.stream.worker.base.WorkerException;
import org.skywalking.apm.collector.stream.worker.impl.data.DataCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 异步批量存储 Worker
 *
 * @author peng-yongsheng
 */
public abstract class PersistenceWorker<INPUT extends Data, OUTPUT extends Data> extends AbstractLocalAsyncWorker<INPUT, OUTPUT> {

    private final Logger logger = LoggerFactory.getLogger(PersistenceWorker.class);

    /**
     * 数据存储
     */
    private final DataCache dataCache;
    /**
     * 批量操作 DAO
     */
    private final IBatchDAO batchDAO;

    public PersistenceWorker(ModuleManager moduleManager) {
        super(moduleManager);
        this.dataCache = new DataCache();
        this.batchDAO = moduleManager.find(StorageModule.NAME).getService(IBatchDAO.class);
    }

    public final void flushAndSwitch() {
        try {
            if (dataCache.trySwitchPointer()) {
                dataCache.switchPointer();
            }
        } finally {
            dataCache.trySwitchPointerFinally();
        }
    }

    @Override protected final void onWork(INPUT message) throws WorkerException {
        if (dataCache.currentCollectionSize() >= 5000) {
            try {
                //
                if (dataCache.trySwitchPointer()) {
                    // 切换数据指针，并标记原指向正在读取中
                    dataCache.switchPointer();

                    // 准备批量操作集合
                    List<?> collection = buildBatchCollection();

                    // 执行批量操作
                    batchDAO.batchPersistence(collection);
                }
            } finally {
                dataCache.trySwitchPointerFinally();
            }
        }

        // 聚合数据
        aggregate(message);
    }

    /**
     * 创建批量操作对象数组
     *
     * @return 批量操作对象数组
     * @throws WorkerException
     */
    public final List<?> buildBatchCollection() throws WorkerException {
        List<?> batchCollection = new LinkedList<>();
        try {
            // 等待原指向不在读取中
            while (dataCache.getLast().isWriting()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    logger.warn("thread wake up");
                }
            }
            // 准备批量操作集合
            if (dataCache.getLast().collection() != null) {
                batchCollection = prepareBatch(dataCache.getLast().collection());
            }
        } finally {
            dataCache.finishReadingLast();
        }
        return batchCollection;
    }

    protected final List<Object> prepareBatch(Map<String, Data> dataMap) {
        List<Object> insertBatchCollection = new LinkedList<>();
        List<Object> updateBatchCollection = new LinkedList<>();
        dataMap.forEach((id, data) -> {
            if (needMergeDBData()) { // 需要合并
                Data dbData = persistenceDAO().get(id);
                // 存在，则更新操作
                if (ObjectUtils.isNotEmpty(dbData)) {
                    dbData.mergeData(data);
                    try {
                        updateBatchCollection.add(persistenceDAO().prepareBatchUpdate(dbData));
                    } catch (Throwable t) {
                        logger.error(t.getMessage(), t);
                    }
                // 不存在，则新增操作
                } else {
                    try {
                        insertBatchCollection.add(persistenceDAO().prepareBatchInsert(data));
                    } catch (Throwable t) {
                        logger.error(t.getMessage(), t);
                    }
                }
            } else {
                // 新增操作
                try {
                    insertBatchCollection.add(persistenceDAO().prepareBatchInsert(data));
                } catch (Throwable t) {
                    logger.error(t.getMessage(), t);
                }
            }
        });

        insertBatchCollection.addAll(updateBatchCollection);
        return insertBatchCollection;
    }

    /**
     * 聚合消息到数据
     *
     * 逻辑同 {@link AggregationWorker#aggregate(Data)}
     *
     * @param message 消息
     */
    private void aggregate(Object message) {
        // 标记数据指针正在写入中
        dataCache.writing();
        Data newData = (Data)message;
        // 写入
        if (dataCache.containsKey(newData.getId())) {
            dataCache.get(newData.getId()).mergeData(newData);
        } else {
            dataCache.put(newData.getId(), newData);
        }
        // 标记数据指针完成写入
        dataCache.finishWriting();
    }

    /**
     * @return 持久化 DAO 接口
     */
    protected abstract IPersistenceDAO persistenceDAO();

    /**
     * @return 是否需要合并数据
     */
    protected abstract boolean needMergeDBData();
}
