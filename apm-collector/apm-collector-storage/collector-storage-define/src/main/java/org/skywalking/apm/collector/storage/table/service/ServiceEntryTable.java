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

package org.skywalking.apm.collector.storage.table.service;

import org.skywalking.apm.collector.core.data.CommonTable;

/**
 * 入口操作表
 *
 * @author peng-yongsheng
 */
public class ServiceEntryTable extends CommonTable {

    public static final String TABLE = "service_entry";

    /**
     * 应用编号
     */
    public static final String COLUMN_APPLICATION_ID = "application_id";
    /**
     * 操作编号
     */
    public static final String COLUMN_ENTRY_SERVICE_ID = "entry_service_id";
    /**
     * 操作名
     */
    public static final String COLUMN_ENTRY_SERVICE_NAME = "entry_service_name";
    /**
     * 注册时间
     */
    public static final String COLUMN_REGISTER_TIME = "register_time";
    /**
     * 最后调用时间
     */
    public static final String COLUMN_NEWEST_TIME = "newest_time";

}