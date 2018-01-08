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
import org.skywalking.apm.collector.cache.service.ServiceNameCacheService;
import org.skywalking.apm.collector.core.module.ModuleManager;
import org.skywalking.apm.collector.core.util.Const;
import org.skywalking.apm.collector.core.util.StringUtils;
import org.skywalking.apm.collector.storage.StorageModule;
import org.skywalking.apm.collector.storage.dao.ISegmentUIDAO;
import org.skywalking.apm.network.proto.KeyWithStringValue;
import org.skywalking.apm.network.proto.LogMessage;
import org.skywalking.apm.network.proto.SpanObject;
import org.skywalking.apm.network.proto.TraceSegmentObject;
import org.skywalking.apm.network.trace.component.ComponentsDefine;

import java.util.List;

/**
 * @author peng-yongsheng
 */
public class SpanService {

    private final ISegmentUIDAO segmentDAO;
    private final ServiceNameCacheService serviceNameCacheService;
    private final ApplicationCacheService applicationCacheService;

    public SpanService(ModuleManager moduleManager) {
        this.segmentDAO = moduleManager.find(StorageModule.NAME).getService(ISegmentUIDAO.class);
        this.serviceNameCacheService = moduleManager.find(CacheModule.NAME).getService(ServiceNameCacheService.class);
        this.applicationCacheService = moduleManager.find(CacheModule.NAME).getService(ApplicationCacheService.class);
    }

    public JsonObject load(String segmentId, int spanId) {
        // 加载 TraceSegment
        TraceSegmentObject segmentObject = segmentDAO.load(segmentId);

        JsonObject spanJson = new JsonObject();
        List<SpanObject> spans = segmentObject.getSpansList();
        for (SpanObject spanObject : spans) {
            if (spanId == spanObject.getSpanId()) {
                // 操作名
                String operationName = spanObject.getOperationName();
                if (spanObject.getOperationNameId() != 0) {
                    String serviceName = serviceNameCacheService.get(spanObject.getOperationNameId());
                    if (StringUtils.isNotEmpty(serviceName)) {
                        operationName = serviceName.split(Const.ID_SPLIT)[1];
                    }
                }
                spanJson.addProperty("operationName", operationName);

                // 开始时间与结束时间
                spanJson.addProperty("startTime", spanObject.getStartTime());
                spanJson.addProperty("endTime", spanObject.getEndTime());

                // loggers
                JsonArray logsArray = new JsonArray();
                List<LogMessage> logs = spanObject.getLogsList();
                for (LogMessage logMessage : logs) {
                    JsonObject logJson = new JsonObject();
                    logJson.addProperty("time", logMessage.getTime());

                    JsonArray logInfoArray = new JsonArray();
                    for (KeyWithStringValue value : logMessage.getDataList()) {
                        JsonObject valueJson = new JsonObject();
                        valueJson.addProperty("key", value.getKey());
                        valueJson.addProperty("value", value.getValue());
                        logInfoArray.add(valueJson);
                    }
                    logJson.add("logInfo", logInfoArray);
                    logsArray.add(logJson);
                }
                spanJson.add("logMessage", logsArray);

                JsonArray tagsArray = new JsonArray();

                // 【tags】span type ，Entry / Local / Exit 三种
                JsonObject spanTypeJson = new JsonObject();
                spanTypeJson.addProperty("key", "span type");
                spanTypeJson.addProperty("value", spanObject.getSpanType().name());
                tagsArray.add(spanTypeJson);

                // 【tags】component
                JsonObject componentJson = new JsonObject();
                componentJson.addProperty("key", "component");
                if (spanObject.getComponentId() == 0) {
                    componentJson.addProperty("value", spanObject.getComponent());
                } else {
                    componentJson.addProperty("value", ComponentsDefine.getInstance().getComponentName(spanObject.getComponentId()));
                }
                tagsArray.add(componentJson);

                // 【tags】peer 服务地址，例如：mongodb 的服务地址
                JsonObject peerJson = new JsonObject();
                peerJson.addProperty("key", "peer");
                if (spanObject.getPeerId() == 0) {
                    peerJson.addProperty("value", spanObject.getPeer());
                } else {
                    peerJson.addProperty("value", applicationCacheService.get(spanObject.getPeerId()));
                }
                tagsArray.add(peerJson);

                // 【tags】Span 的标签键值对
                for (KeyWithStringValue tagValue : spanObject.getTagsList()) {
                    JsonObject tagJson = new JsonObject();
                    tagJson.addProperty("key", tagValue.getKey());
                    tagJson.addProperty("value", tagValue.getValue());
                    tagsArray.add(tagJson);
                }

                // 【tags】isError ，是否发生错误
                JsonObject isErrorJson = new JsonObject();
                isErrorJson.addProperty("key", "is error");
                isErrorJson.addProperty("value", spanObject.getIsError());
                tagsArray.add(isErrorJson);

                spanJson.add("tags", tagsArray);
            }
        }

        return spanJson;
    }
}