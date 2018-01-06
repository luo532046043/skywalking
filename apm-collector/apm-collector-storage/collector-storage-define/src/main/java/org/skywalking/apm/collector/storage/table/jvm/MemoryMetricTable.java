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

package org.skywalking.apm.collector.storage.table.jvm;

import org.skywalking.apm.collector.core.data.CommonTable;

/**
 * @author peng-yongsheng
 */
public class MemoryMetricTable extends CommonTable {

    public static final String TABLE = "memory_metric";
    /**
     * 应用实例编号
     */
    public static final String COLUMN_INSTANCE_ID = "instance_id";
    /**
     * 是否堆内内存
     */
    public static final String COLUMN_IS_HEAP = "is_heap";
    /**
     * 初始化的内存数量
     */
    public static final String COLUMN_INIT = "init";
    /**
     * 最大的内存数量
     */
    public static final String COLUMN_MAX = "max";
    /**
     * 已使用的内存数量
     */
    public static final String COLUMN_USED = "used";
    /**
     * 可以使用的内存数量
     */
    public static final String COLUMN_COMMITTED = "committed";
}
