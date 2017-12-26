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

package org.skywalking.apm.collector.agent.grpc.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.grpc.stub.StreamObserver;
import org.skywalking.apm.collector.agent.stream.AgentStreamModule;
import org.skywalking.apm.collector.agent.stream.service.register.IInstanceIDService;
import org.skywalking.apm.collector.core.module.ModuleManager;
import org.skywalking.apm.collector.core.util.TimeBucketUtils;
import org.skywalking.apm.collector.server.grpc.GRPCHandler;
import org.skywalking.apm.network.proto.ApplicationInstance;
import org.skywalking.apm.network.proto.ApplicationInstanceMapping;
import org.skywalking.apm.network.proto.ApplicationInstanceRecover;
import org.skywalking.apm.network.proto.Downstream;
import org.skywalking.apm.network.proto.InstanceDiscoveryServiceGrpc;
import org.skywalking.apm.network.proto.OSInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 应用实例发现逻辑处理器
 *
 * @author peng-yongsheng
 */
public class InstanceDiscoveryServiceHandler extends InstanceDiscoveryServiceGrpc.InstanceDiscoveryServiceImplBase implements GRPCHandler {

    private final Logger logger = LoggerFactory.getLogger(InstanceDiscoveryServiceHandler.class);

    private final IInstanceIDService instanceIDService;

    public InstanceDiscoveryServiceHandler(ModuleManager moduleManager) {
        this.instanceIDService = moduleManager.find(AgentStreamModule.NAME).getService(IInstanceIDService.class);
    }

    @Override
    public void register(ApplicationInstance request, StreamObserver<ApplicationInstanceMapping> responseObserver) {
        // 将 registerTime 转成 timeBucket
        long timeBucket = TimeBucketUtils.INSTANCE.getSecondTimeBucket(request.getRegisterTime());
        // 获得 应用实例编号
        int instanceId = instanceIDService.getOrCreate(request.getApplicationId(), request.getAgentUUID(), timeBucket, buildOsInfo(request.getOsinfo()));
        ApplicationInstanceMapping.Builder builder = ApplicationInstanceMapping.newBuilder();
        // 添加 应用实例编号 到响应
        builder.setApplicationId(request.getApplicationId());
        builder.setApplicationInstanceId(instanceId);
        // 响应
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void registerRecover(ApplicationInstanceRecover request, StreamObserver<Downstream> responseObserver) {
        // 将 registerTime 转成 timeBucket
        long timeBucket = TimeBucketUtils.INSTANCE.getSecondTimeBucket(request.getRegisterTime());
        // 恢复
        instanceIDService.recover(request.getApplicationInstanceId(), request.getApplicationId(), timeBucket, buildOsInfo(request.getOsinfo()));
        // 响应
        responseObserver.onNext(Downstream.newBuilder().build());
        responseObserver.onCompleted();
    }

    /**
     * 将 OSInfo 转成 JSON 字符串
     *
     * @param osinfo 系统信息对象
     * @return JSON 字符串
     */
    private String buildOsInfo(OSInfo osinfo) {
        JsonObject osInfoJson = new JsonObject();
        osInfoJson.addProperty("osName", osinfo.getOsName());
        osInfoJson.addProperty("hostName", osinfo.getHostname());
        osInfoJson.addProperty("processId", osinfo.getProcessNo());

        JsonArray ipv4Array = new JsonArray();
        for (String ipv4 : osinfo.getIpv4SList()) {
            ipv4Array.add(ipv4);
        }
        osInfoJson.add("ipv4s", ipv4Array);
        return osInfoJson.toString();
    }

}
