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

package org.skywalking.apm.collector.server;

/**
 * Server 接口
 *
 * @author peng-yongsheng
 */
public interface Server {

    /**
     * @return 地址
     */
    String hostPort();

    /**
     * @return 服务器分类
     */
    String serverClassify();

    /**
     * 初始化服务器
     *
     * @throws ServerException 服务器异常
     */
    void initialize() throws ServerException;

    /**
     * 启动服务器
     *
     * @throws ServerException 服务器异常
     */
    void start() throws ServerException;

    /**
     * 添加请求处理器
     *
     * @param handler 处理器
     */
    void addHandler(ServerHandler handler);
}
