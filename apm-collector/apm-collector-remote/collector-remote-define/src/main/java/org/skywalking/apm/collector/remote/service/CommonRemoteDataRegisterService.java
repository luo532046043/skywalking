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

import org.skywalking.apm.collector.core.data.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用远程数据注册服务
 *
 * @author peng-yongsheng
 */
public class CommonRemoteDataRegisterService implements RemoteDataRegisterService, RemoteDataIDGetter, RemoteDataInstanceCreatorGetter {

    private final Logger logger = LoggerFactory.getLogger(CommonRemoteDataRegisterService.class);

    /**
     * 数据协议自增编号
     */
    private Integer id;
    /**
     * 数据类型与数据协议编号的映射
     */
    private final Map<Class<? extends Data>, Integer> dataClassMapping;
    /**
     * 数据协议编号与数据对象创建器的映射
     */
    private final Map<Integer, RemoteDataInstanceCreator> dataInstanceCreatorMapping;

    public CommonRemoteDataRegisterService() {
        this.id = 1;
        this.dataClassMapping = new HashMap<>();
        this.dataInstanceCreatorMapping = new HashMap<>();
    }

    @Override public void register(Class<? extends Data> dataClass, RemoteDataInstanceCreator instanceCreator) {
        if (!dataClassMapping.containsKey(dataClass)) {
            dataClassMapping.put(dataClass, this.id);
            dataInstanceCreatorMapping.put(this.id, instanceCreator);
            this.id++;
        } else {
            logger.warn("The data class {} was registered.", dataClass.getName());
        }
    }

    @Override
    public Integer getRemoteDataId(Class<? extends Data> dataClass) throws RemoteDataMappingIdNotFoundException {
        if (dataClassMapping.containsKey(dataClass)) {
            return dataClassMapping.get(dataClass);
        } else {
            throw new RemoteDataMappingIdNotFoundException("Could not found the id of remote data class " + dataClass.getName());
        }
    }

    @Override public RemoteDataInstanceCreator getInstanceCreator(
        Integer remoteDataId) throws RemoteDataInstanceCreatorNotFoundException {
        if (dataInstanceCreatorMapping.containsKey(remoteDataId)) {
            return dataInstanceCreatorMapping.get(remoteDataId);
        } else {
            throw new RemoteDataInstanceCreatorNotFoundException("Could not found the instance creator of remote data id " + remoteDataId);
        }
    }

}
