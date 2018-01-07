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

package org.skywalking.apm.collector.ui.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.skywalking.apm.collector.cache.CacheModule;
import org.skywalking.apm.collector.cache.service.ApplicationCacheService;
import org.skywalking.apm.collector.core.module.ModuleManager;
import org.skywalking.apm.collector.core.util.TimeBucketUtils;
import org.skywalking.apm.collector.storage.StorageModule;
import org.skywalking.apm.collector.storage.dao.IGCMetricUIDAO;
import org.skywalking.apm.collector.storage.dao.IInstPerformanceUIDAO;
import org.skywalking.apm.collector.storage.dao.IInstanceUIDAO;
import org.skywalking.apm.collector.storage.table.register.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author peng-yongsheng
 */
public class InstanceHealthService {

    private final Logger logger = LoggerFactory.getLogger(InstanceHealthService.class);

    private final IGCMetricUIDAO gcMetricDAO;
    private final IInstanceUIDAO instanceDAO;
    private final IInstPerformanceUIDAO instPerformanceDAO;
    private final ApplicationCacheService applicationCacheService;

    public InstanceHealthService(ModuleManager moduleManager) {
        this.gcMetricDAO = moduleManager.find(StorageModule.NAME).getService(IGCMetricUIDAO.class);
        this.instanceDAO = moduleManager.find(StorageModule.NAME).getService(IInstanceUIDAO.class);
        this.instPerformanceDAO = moduleManager.find(StorageModule.NAME).getService(IInstPerformanceUIDAO.class);
        this.applicationCacheService = moduleManager.find(CacheModule.NAME).getService(ApplicationCacheService.class);
    }

    /**
     * 查询应用的实例数组
     * [{
     *     applicationId: // 应用编号
     *     applicationCode: // 应用编码
     *     instances: [{
     *         id: // 应用实例编号
     *         tps: // 每秒事务数
     *         avg: // 平均事务耗时
     *         healthLevel: // 性能健康等级
     *         status: // 应用实例是否存活
     *         ygc: // 新生代 gc 次数
     *         ogc: // 老生代 gc 次数
     *     }]
     * }]
     *
     * @param timeBucket 时间
     * @param applicationId 应用编号
     * @return 应用实例数组
     */
    public JsonObject getInstances(long timeBucket, int applicationId) {
        JsonObject response = new JsonObject();

        // 五秒内的时间数组
        long[] timeBuckets = TimeBucketUtils.INSTANCE.getFiveSecondTimeBuckets(timeBucket);

        // 查询半小时内的 Instance 数组
        long halfHourBeforeTimeBucket = TimeBucketUtils.INSTANCE.addSecondForSecondTimeBucket(TimeBucketUtils.TimeBucketType.SECOND.name(), timeBucket, -60 * 30);
        List<Instance> instanceList = instanceDAO.getInstances(applicationId, halfHourBeforeTimeBucket);

        JsonArray instances = new JsonArray();
        response.add("instances", instances);

        // 循环 Instance 数组
        instanceList.forEach(instance -> {
            response.addProperty("applicationCode", applicationCacheService.get(applicationId));
            response.addProperty("applicationId", applicationId);

            JsonObject instanceJson = new JsonObject();
            instanceJson.addProperty("id", instance.getInstanceId());

            // 查询 InstPerformance
            IInstPerformanceUIDAO.InstPerformance performance = instPerformanceDAO.get(timeBuckets, instance.getInstanceId());

            // 基于 InstPerformance ，设置 tps
            if (performance != null) {
                instanceJson.addProperty("tps", performance.getCalls());
            } else {
                instanceJson.addProperty("tps", 0);
            }

            // 基于 InstPerformance ，设置 avg 和 healthLevel
            int avg = 0;
            if (performance != null && performance.getCalls() != 0) {
                avg = (int)(performance.getCostTotal() / performance.getCalls());
            }
            instanceJson.addProperty("avg", avg);
            if (avg > 5000) {
                instanceJson.addProperty("healthLevel", 0);
            } else if (avg > 3000 && avg <= 5000) {
                instanceJson.addProperty("healthLevel", 1);
            } else if (avg > 1000 && avg <= 3000) {
                instanceJson.addProperty("healthLevel", 2);
            } else {
                instanceJson.addProperty("healthLevel", 3);
            }

            // 基于 Instance
            long heartBeatTime = TimeBucketUtils.INSTANCE.changeTimeBucket2TimeStamp(TimeBucketUtils.TimeBucketType.SECOND.name(), instance.getHeartBeatTime());
            long currentTime = TimeBucketUtils.INSTANCE.changeTimeBucket2TimeStamp(TimeBucketUtils.TimeBucketType.SECOND.name(), timeBucket);
            if (currentTime - heartBeatTime < 1000 * 60 * 2) { // 在线
                instanceJson.addProperty("status", 0);
            } else { // 离线
                instanceJson.addProperty("status", 1);
            }

            // 查询 GCCount ，设置 ygc 和 ogc
            IGCMetricUIDAO.GCCount gcCount = gcMetricDAO.getGCCount(timeBuckets, instance.getInstanceId());
            instanceJson.addProperty("ygc", gcCount.getYoung());
            instanceJson.addProperty("ogc", gcCount.getOld());

            instances.add(instanceJson);
        });

        return response;
    }
}
