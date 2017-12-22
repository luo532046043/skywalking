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

package org.skywalking.apm.collector.core.data;

/**
 * 通用表
 *
 * @author peng-yongsheng
 */
public class CommonTable {

    /**
     * 表类型
     *
     * 目前只有 es 在使用，用于文档元数据的 _type 字段，参见：http://geosmart.github.io/2016/07/22/Elasticsearch%E5%AD%A6%E4%B9%A0%E7%AC%94%E8%AE%B0/#%E6%96%87%E6%A1%A3%E5%85%83%E6%95%B0%E6%8D%AE
     */
    public static final String TABLE_TYPE = "type";

    // ========== 字段名 ==========

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_AGG = "agg";
    public static final String COLUMN_TIME_BUCKET = "time_bucket";
}
