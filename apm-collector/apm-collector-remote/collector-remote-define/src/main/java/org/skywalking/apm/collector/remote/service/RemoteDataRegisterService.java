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
import org.skywalking.apm.collector.core.module.Service;

/**
 * 远程数据注册服务接口
 *
 * @author peng-yongsheng
 */
public interface RemoteDataRegisterService extends Service {

    /**
     * 注册数据类型对应的远程数据创建器对象
     *
     * @param dataClass 数据类型
     * @param instanceCreator 远程数据创建器对象
     */
    void register(Class<? extends Data> dataClass, RemoteDataInstanceCreator instanceCreator);

    interface RemoteDataInstanceCreator<RemoteData extends Data> {

        /**
         * 创建数据对象
         *
         * @param id 数据编号 {@link Data#getId()}
         * @return 数据对象
         */
        RemoteData createInstance(String id);

    }

}
