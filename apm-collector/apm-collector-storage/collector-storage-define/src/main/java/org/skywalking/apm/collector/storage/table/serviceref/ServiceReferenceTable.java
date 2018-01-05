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

package org.skywalking.apm.collector.storage.table.serviceref;

import org.skywalking.apm.collector.core.data.CommonTable;

/**
 * 操作调用统计 Table
 *
 * @author peng-yongsheng
 */
public class ServiceReferenceTable extends CommonTable {

    public static final String TABLE = "service_reference";

    /**
     * 入口操作编号
     */
    public static final String COLUMN_ENTRY_SERVICE_ID = "entry_service_id";
    /**
     * 服务消费者操作编号
     */
    public static final String COLUMN_FRONT_SERVICE_ID = "front_service_id";
    /**
     * 服务提供者操作编号
     */
    public static final String COLUMN_BEHIND_SERVICE_ID = "behind_service_id";
    /**
     * (0, 1000ms] 的次数
     */
    public static final String COLUMN_S1_LTE = "s1_lte";
    /**
     * (1000, 3000ms] 的次数
     */
    public static final String COLUMN_S3_LTE = "s3_lte";
    /**
     * (3000, 5000ms] 的次数
     */
    public static final String COLUMN_S5_LTE = "s5_lte";
    /**
     * (5000ms, 无穷] 的次数
     */
    public static final String COLUMN_S5_GT = "s5_gt";
    /**
     * 总共的次数
     */
    public static final String COLUMN_SUMMARY = "summary";
    /**
     * 总共的花费时间
     */
    public static final String COLUMN_COST_SUMMARY = "cost_summary";
    /**
     * 错误的次数
     */
    public static final String COLUMN_ERROR = "error";

}
