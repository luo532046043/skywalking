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

package org.skywalking.apm.collector.cache.service;

import org.skywalking.apm.collector.core.module.Service;
import org.skywalking.apm.collector.storage.table.register.Instance;

/**
 * 应用实例数据缓存服务接口
 *
 * {@link org.skywalking.apm.collector.storage.table.register.Instance}
 *
 * @author peng-yongsheng
 */
public interface InstanceCacheService extends Service {

    /**
     * 获得应用编号 {@link Instance#getApplicationId()} ()}
     *
     * @param applicationInstanceId 应用实例编号
     * @return 应用编号
     */
    int get(int applicationInstanceId);

    /**
     * 获得应用实例编号  {@link Instance#getId()}
     *
     * @param applicationId 应用编号
     * @param agentUUID Agent UUID
     * @return 应用实例编号
     */
    int getInstanceId(int applicationId, String agentUUID);

}
