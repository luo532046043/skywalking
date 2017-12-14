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

package org.skywalking.apm.collector.cluster.zookeeper;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;
import org.skywalking.apm.collector.client.Client;
import org.skywalking.apm.collector.client.ClientException;
import org.skywalking.apm.collector.client.zookeeper.ZookeeperClient;
import org.skywalking.apm.collector.client.zookeeper.ZookeeperClientException;
import org.skywalking.apm.collector.cluster.ClusterModuleListener;
import org.skywalking.apm.collector.cluster.ClusterNodeExistException;
import org.skywalking.apm.collector.cluster.DataMonitor;
import org.skywalking.apm.collector.cluster.ModuleRegistration;
import org.skywalking.apm.collector.core.CollectorException;
import org.skywalking.apm.collector.core.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 基于 Zookeeper 的数据监视器实现类
 *
 * @author peng-yongsheng
 */
public class ClusterZKDataMonitor implements DataMonitor, Watcher {

    private final Logger logger = LoggerFactory.getLogger(ClusterZKDataMonitor.class);

    private ZookeeperClient client;

    private Map<String, ClusterModuleListener> listeners;
    private Map<String, ModuleRegistration> registrations;

    public ClusterZKDataMonitor() {
        listeners = new LinkedHashMap<>();
        registrations = new LinkedHashMap<>();
    }

    /**
     * 处理有 Collector 节点的组件加入或下线。
     * 总体逻辑是，从 Zookeeper 获取变更的路径下的地址数组，和本地的地址( `ClusterModuleListener.addresses` )比较，处理加入或移除逻辑的地址。
     *
     * @param event 变更事件
     */
    @Override public synchronized void process(WatchedEvent event) { // 同步方法
        logger.info("changed path {}, event type: {}", event.getPath(), event.getType().name());
        if (listeners.containsKey(event.getPath())) {
            List<String> paths;
            try {
                // 获得子路径数组，重新添加 Zookeeper Watch
                paths = client.getChildren(event.getPath(), true);

                ClusterModuleListener listener = listeners.get(event.getPath());
                Set<String> remoteNodes = new HashSet<>();
                Set<String> notifiedNodes = listener.getAddresses();

                // 处理是否有新增
                if (CollectionUtils.isNotEmpty(paths)) {
                    for (String serverPath : paths) {
                        // 获得 addressValue
                        Stat stat = new Stat();
                        byte[] data = client.getData(event.getPath() + "/" + serverPath, true, stat);
                        String dataStr = new String(data);
                        String addressValue = serverPath + dataStr;
                        // 添加到 remoteNodes ，用于下面判断是否有移除
                        remoteNodes.add(addressValue);
                        // 不存在，说明有新增
                        if (!notifiedNodes.contains(addressValue)) {
                            logger.info("path children has been created, path: {}, data: {}", event.getPath() + "/" + serverPath, dataStr);
                            listener.addAddress(addressValue);
                            listener.serverJoinNotify(addressValue);
                        }
                    }
                }

                // 处理是否有移除
                String[] notifiedNodeArray = notifiedNodes.toArray(new String[notifiedNodes.size()]);
                for (int i = notifiedNodeArray.length - 1; i >= 0; i--) {
                    String address = notifiedNodeArray[i];
                    // 不存在，说明被移除
                    if (remoteNodes.isEmpty() || !remoteNodes.contains(address)) {
                        logger.info("path children has been remove, path and data: {}", event.getPath() + "/" + address);
                        listener.removeAddress(address);
                        listener.serverQuitNotify(address);
                    }
                }
            } catch (ZookeeperClientException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override public void setClient(Client client) {
        this.client = (ZookeeperClient)client;
    }

    /**
     * 启动 ClusterZKDataMonitor ，将组件注册信息( registrations ) 写到 Zookeeper 中。
     *
     * @throws CollectorException
     */
    public void start() throws CollectorException {
        Iterator<Map.Entry<String, ModuleRegistration>> entryIterator = registrations.entrySet().iterator();
        while (entryIterator.hasNext()) {
            // 创建 Zookeeper 路径
            Map.Entry<String, ModuleRegistration> next = entryIterator.next();
            createPath(next.getKey());

            // 生成 contextPath
            ModuleRegistration.Value value = next.getValue().buildValue();
            String contextPath = value.getContextPath() == null ? "" : value.getContextPath();

            // 添加 Zookeeper Watch
            client.getChildren(next.getKey(), true);

            // 创建 serverPath
            String serverPath = next.getKey() + "/" + value.getHostPort();

            // 若 Zookeeper serverPath 路径已经存在，进行删除
            Stat stat = client.exists(serverPath, false);
            if (stat != null) {
                client.delete(serverPath, stat.getVersion());
            }

            // 判断是否删除成功？成功，设置值；失败，说明可能存在并发的情况，抛出异常。
            stat = client.exists(serverPath, false);
            if (stat == null) {
                setData(serverPath, contextPath);
            } else {
                client.delete(serverPath, stat.getVersion());
                throw new ClusterNodeExistException("current address: " + value.getHostPort() + " has been registered, check the host and port configuration or wait a moment.");
            }
        }
    }

    @Override public void addListener(ClusterModuleListener listener) {
        String path = BASE_CATALOG + listener.path();
        logger.info("listener path: {}", path);
        listeners.put(path, listener);
    }

    @Override public void register(String path, ModuleRegistration registration) {
        registrations.put(BASE_CATALOG + path, registration);
    }

    @Override public ClusterModuleListener getListener(String path) {
        path = BASE_CATALOG + path;
        return listeners.get(path);
    }

    @Override public void createPath(String path) throws ClientException {
        // 顺着目录，向下创建
        String[] paths = path.replaceFirst("/", "").split("/");
        StringBuilder pathBuilder = new StringBuilder();
        for (String subPath : paths) {
            pathBuilder.append("/").append(subPath); // 逐层拼接目录
            if (client.exists(pathBuilder.toString(), false) == null) {
                client.create(pathBuilder.toString(), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        }
    }

    @Override public void setData(String path, String value) throws ClientException {
        if (client.exists(path, false) == null) {
            client.create(path, value.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL); // 临时节点
        } else {
            client.setData(path, value.getBytes(), -1);
        }
    }
}
