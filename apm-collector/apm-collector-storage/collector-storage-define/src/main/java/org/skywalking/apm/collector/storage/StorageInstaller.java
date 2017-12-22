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

package org.skywalking.apm.collector.storage;

import org.skywalking.apm.collector.client.Client;
import org.skywalking.apm.collector.core.data.StorageDefineLoader;
import org.skywalking.apm.collector.core.data.TableDefine;
import org.skywalking.apm.collector.core.define.DefineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 存储安装器抽象类，基于 TableDefine ，初始化存储器的表
 *
 * @author peng-yongsheng
 */
public abstract class StorageInstaller {

    private final Logger logger = LoggerFactory.getLogger(StorageInstaller.class);

    public final void install(Client client) throws StorageException {
        // 加载 TableDefine 数组
        StorageDefineLoader defineLoader = new StorageDefineLoader();
        try {
            List<TableDefine> tableDefines = defineLoader.load();

            // 过滤 TableDefine 数组中，非自身需要的
            defineFilter(tableDefines);

            // 创建 表
            Boolean debug = System.getProperty("debug") != null; // 调试模式
            for (TableDefine tableDefine : tableDefines) {
                tableDefine.initialize();
                if (!isExists(client, tableDefine)) {
                    logger.info("table: {} not exists", tableDefine.getName());
                    createTable(client, tableDefine);
                } else if (debug) {
                    logger.info("table: {} exists", tableDefine.getName());
                    deleteTable(client, tableDefine);
                    createTable(client, tableDefine);
                }
            }
        } catch (DefineException e) {
            throw new StorageInstallException(e.getMessage(), e);
        }
    }

    /**
     * 过滤 TableDefine 数组中，非自身需要的
     *
     * @param tableDefines 数组
     */
    protected abstract void defineFilter(List<TableDefine> tableDefines);

    /**
     * 判断表是否存在
     *
     * @param client 存储器客户端
     * @param tableDefine 表定义
     * @return 是否存在
     * @throws StorageException 异常
     */
    protected abstract boolean isExists(Client client, TableDefine tableDefine) throws StorageException;

    /**
     * 删除表
     *
     * @param client 存储器客户端
     * @param tableDefine 表定义
     * @return 是否成功
     * @throws StorageException 异常
     */
    protected abstract boolean deleteTable(Client client, TableDefine tableDefine) throws StorageException;

    /**
     * 创建表
     *
     * @param client 存储器客户端
     * @param tableDefine 表定义
     * @return 是否成功
     * @throws StorageException 异常
     */
    protected abstract boolean createTable(Client client, TableDefine tableDefine) throws StorageException;
}
