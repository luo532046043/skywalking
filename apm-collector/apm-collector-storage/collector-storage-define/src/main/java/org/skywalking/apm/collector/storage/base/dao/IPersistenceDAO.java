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

package org.skywalking.apm.collector.storage.base.dao;

import org.skywalking.apm.collector.core.data.Data;

import java.util.List;

/**
 * 持久化 DAO 接口
 *
 * @author peng-yongsheng
 */
public interface IPersistenceDAO<Insert, Update, DataImpl extends Data> extends DAO {

    /**
     * 根据 ID 查询一条数据
     *
     * @param id 编号
     * @return 数据
     */
    DataImpl get(String id);

    /**
     * 准备批量插入操作对象
     *
     * 注意：
     *  1. 该方法不会发起具体的 DAO 操作，仅仅是创建插入操作对象，最终的执行在 {@link IBatchDAO#batchPersistence(List)}
     *  2. 该方法创建的是批量插入操作对象们中的一个
     *
     * @param data 数据
     * @return 批量插入对象
     */
    Insert prepareBatchInsert(DataImpl data);

    /**
     * 准备批量更新操作对象
     *
     * 同 {@link #prepareBatchInsert(Data)} 方法
     *
     * @param data 数据
     * @return 批量更新对象
     */
    Update prepareBatchUpdate(DataImpl data);

    /**
     * 删除时间范围内的数据们
     *
     * @param startTimestamp 开始时间
     * @param endTimestamp 结束时间
     */
    void deleteHistory(Long startTimestamp, Long endTimestamp);

}
