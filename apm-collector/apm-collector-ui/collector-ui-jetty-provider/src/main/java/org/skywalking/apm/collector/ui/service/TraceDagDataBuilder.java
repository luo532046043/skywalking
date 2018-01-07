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
import org.skywalking.apm.collector.core.util.ColumnNameUtils;
import org.skywalking.apm.collector.core.util.Const;
import org.skywalking.apm.collector.storage.table.node.NodeComponentTable;
import org.skywalking.apm.collector.storage.table.node.NodeMappingTable;
import org.skywalking.apm.collector.storage.table.noderef.NodeReferenceTable;
import org.skywalking.apm.network.trace.component.Component;
import org.skywalking.apm.network.trace.component.ComponentsDefine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 应用拓扑图构建起器
 *
 * @author peng-yongsheng
 */
public class TraceDagDataBuilder {
    private final Logger logger = LoggerFactory.getLogger(TraceDagDataBuilder.class);

    /**
     * 节点编号自增序列
     * 配合 {@link #nodeIdMap} 变量
     * 配合 {@link #findOrCreateNode(String)} 方法
     */
    private Integer nodeId = -1;
    /**
     * 服务提供者网络地址( address )与服务提供者应用编码( applicationCode )的映射
     * {@link #changeMapping2Map(JsonArray)} 使用 NodeMapping 数组
     */
    private Map<String, String> mappingMap = new HashMap<>();
    /**
     * 应用名与组件名的映射
     * {@link #changeNodeComp2Map(JsonArray)} 使用 NodeComponent 数组
     */
    private Map<String, String> nodeCompMap = new HashMap<>();
    /**
     * 节点编号与应用编码的映射
     * 此处的节点编码，是通过 {@link #nodeId} 自增生成的，不是数据库里的
     */
    private Map<String, Integer> nodeIdMap = new HashMap<>();
    /**
     * 节点数组
     * [{
     *     id: // ${nodeId}, 通过 {@link #nodeId} 生成
     *     peer: // 应用编码 {@link org.skywalking.apm.collector.storage.table.register.ApplicationTable#COLUMN_APPLICATION_CODE}
     *     component: // 组件名 {@link Component#getName()}
     * }]
     */
    private JsonArray pointArray = new JsonArray();
    /**
     * 连线数组
     * [{
     *     from: // ${nodeId}, 通过 {@link #nodeId} 生成
     *     to: // ${nodeId}, 通过 {@link #nodeId} 生成
     *     sum: // 次数
     * }]
     */
    private JsonArray lineArray = new JsonArray();
    private final ApplicationCacheService applicationCacheService;

    public TraceDagDataBuilder(ModuleManager moduleManager) {
        this.applicationCacheService = moduleManager.find(CacheModule.NAME).getService(ApplicationCacheService.class);
    }

    public JsonObject build(JsonArray nodeCompArray, JsonArray nodesMappingArray, JsonArray resSumArray) {
        // 构建 nodeCompMap
        changeNodeComp2Map(nodeCompArray);
        // 构建 mappingMap
        changeMapping2Map(nodesMappingArray);
        // 构建 mergedResSumMap
        Map<String, JsonObject> mergedResSumMap = getApplicationCode(resSumArray);

        mergedResSumMap.values().forEach(nodeRefJsonObj -> {
            String front = nodeRefJsonObj.get("front").getAsString();
            String behind = nodeRefJsonObj.get("behind").getAsString();

            // 此处分成两种情况
            // 1.【A 服务】调用【B 服务】，【A 服务】会基于 ExitSpan 记录一条 NodeReference ，【B 服务】会基于 EntrySpan 记录一条 NodeReference
            //      两条是对等的，取其中一条即可
            // 2.【A 服务】调用【MongoDB】，【A 服务】会基于 ExitSpan 记录一条 NodeReference ，【MongoDB】因为没有 SkyWalking Agent 记录 NodeReference ，
            //      所以就一条，也不能存在取不取的问题
            if (hasMapping(behind)) {
                return;
            }

            JsonObject lineJsonObj = new JsonObject();
            lineJsonObj.addProperty("from", findOrCreateNode(front));
            lineJsonObj.addProperty("to", findOrCreateNode(behind));
            lineJsonObj.addProperty("resSum", nodeRefJsonObj.get(NodeReferenceTable.COLUMN_SUMMARY).getAsInt());

            lineArray.add(lineJsonObj);
            logger.debug("line: {}", lineJsonObj);
        });

        // 返回结果
        JsonObject dagJsonObj = new JsonObject();
        dagJsonObj.add("nodes", pointArray);
        dagJsonObj.add("nodeRefs", lineArray);
        return dagJsonObj;
    }

    private Integer findOrCreateNode(String peers) {
        if (nodeIdMap.containsKey(peers) && !peers.equals(Const.USER_CODE)) { // USER_CODE 特殊处理的原因，在于会有多个，例如 USER 请求【A 服务】【B 服务】
            return nodeIdMap.get(peers);
        } else {
            nodeId++;
            JsonObject nodeJsonObj = new JsonObject();
            nodeJsonObj.addProperty("id", nodeId);
            nodeJsonObj.addProperty("peer", peers);
            // 组件名
            if (peers.equals(Const.USER_CODE)) {
                nodeJsonObj.addProperty("component", Const.USER_CODE);
            } else {
                nodeJsonObj.addProperty("component", nodeCompMap.get(peers));
            }
            pointArray.add(nodeJsonObj);

            //
            nodeIdMap.put(peers, nodeId);
            logger.debug("node: {}", nodeJsonObj);
        }
        return nodeId;
    }

    private void changeMapping2Map(JsonArray nodesMappingArray) {
        for (int i = 0; i < nodesMappingArray.size(); i++) {
            JsonObject nodesMappingJsonObj = nodesMappingArray.get(i).getAsJsonObject();
            int applicationId = nodesMappingJsonObj.get(NodeMappingTable.COLUMN_APPLICATION_ID).getAsInt();
            String applicationCode = applicationCacheService.get(applicationId);
            int addressId = nodesMappingJsonObj.get(NodeMappingTable.COLUMN_ADDRESS_ID).getAsInt();
            String address = applicationCacheService.get(addressId);
            mappingMap.put(address, applicationCode);
        }
    }

    private void changeNodeComp2Map(JsonArray nodeCompArray) {
        for (int i = 0; i < nodeCompArray.size(); i++) {
            JsonObject nodesJsonObj = nodeCompArray.get(i).getAsJsonObject();
            logger.debug(nodesJsonObj.toString());
            int componentId = nodesJsonObj.get(NodeComponentTable.COLUMN_COMPONENT_ID).getAsInt();
            String componentName = ComponentsDefine.getInstance().getComponentName(componentId);
            int peerId = nodesJsonObj.get(NodeComponentTable.COLUMN_PEER_ID).getAsInt();
            String peer = applicationCacheService.get(peerId);
            nodeCompMap.put(peer, componentName);
        }
    }

    private boolean hasMapping(String peers) {
        return mappingMap.containsKey(peers);
    }

    private Map<String, JsonObject> getApplicationCode(JsonArray nodeReference) {
        Map<String, JsonObject> mergedRef = new LinkedHashMap<>();
        for (int i = 0; i < nodeReference.size(); i++) {
            JsonObject nodeRefJsonObj = nodeReference.get(i).getAsJsonObject();

            // 应用编号
            int frontApplicationId = nodeRefJsonObj.get(ColumnNameUtils.INSTANCE.rename(NodeReferenceTable.COLUMN_FRONT_APPLICATION_ID)).getAsInt();
            int behindApplicationId = nodeRefJsonObj.get(ColumnNameUtils.INSTANCE.rename(NodeReferenceTable.COLUMN_BEHIND_APPLICATION_ID)).getAsInt();

            // 应用编码
            String front = applicationCacheService.get(frontApplicationId);
            String behind = applicationCacheService.get(behindApplicationId);

            String id = front + Const.ID_SPLIT + behind;
            nodeRefJsonObj.addProperty("front", front);
            nodeRefJsonObj.addProperty("behind", behind);
            mergedRef.put(id, nodeRefJsonObj);
        }

        return mergedRef;
    }
}
