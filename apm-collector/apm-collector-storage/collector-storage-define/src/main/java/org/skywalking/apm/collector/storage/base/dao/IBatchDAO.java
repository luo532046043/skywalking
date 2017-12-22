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
 * 批量操作 DAO 接口
 *
 * 需要配合 {@link IPersistenceDAO#prepareBatchInsert(Data)} 和 {@link IPersistenceDAO#prepareBatchUpdate(Data)} 一起使用
 *
 * @author peng-yongsheng
 */
public interface IBatchDAO extends DAO {

    /**
     * 通过执行批量操作对象数组，实现批量持久化数据
     *
     * @param batchCollection 批量操作对象数组
     */
    void batchPersistence(List<?> batchCollection);

}
