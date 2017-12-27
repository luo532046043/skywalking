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

package org.skywalking.apm.collector.agent.stream.service.register;

import org.skywalking.apm.collector.core.module.Service;

/**
 * 应用实例服务接口
 *
 * @author peng-yongsheng
 */
public interface IInstanceIDService extends Service {

    /**
     * 根据应用编号 + AgentUUID，获取或创建应用实例，并获得应用编号
     *
     * @param applicationId 应用编号
     * @param agentUUID AgentUUID ，作为 Agent 唯一标识
     * @param registerTime 注册时间
     * @param osInfo 系统信息字符串，一般为 JSON 格式
     * @return 应用实例编号
     */
    int getOrCreate(int applicationId, String agentUUID, long registerTime, String osInfo);

    /**
     * 恢复注册应用实例
     *
     * @param instanceId 应用实例编号
     * @param applicationId 应用编号
     * @param registerTime 注册时间
     * @param osInfo 系统信息字符串，一般为 JSON 格式
     */
    void recover(int instanceId, int applicationId, long registerTime, String osInfo);

}
