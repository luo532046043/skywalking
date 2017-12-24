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

package org.skywalking.apm.collector.remote.service;

/**
 * 远程数据创建器的获取器
 *
 * @author peng-yongsheng
 */
public interface RemoteDataInstanceCreatorGetter {

    /**
     * 根据数据协议编号获得远程数据创建器
     *
     * @param remoteDataId 数据协议编号
     * @return 远程数据创建器
     * @throws RemoteDataInstanceCreatorNotFoundException
     */
    RemoteDataRegisterService.RemoteDataInstanceCreator getInstanceCreator(
        Integer remoteDataId) throws RemoteDataInstanceCreatorNotFoundException;

}
