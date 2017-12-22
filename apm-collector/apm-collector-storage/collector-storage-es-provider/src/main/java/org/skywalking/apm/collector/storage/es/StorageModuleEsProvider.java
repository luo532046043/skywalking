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

package org.skywalking.apm.collector.storage.es;

import org.skywalking.apm.collector.client.ClientException;
import org.skywalking.apm.collector.client.elasticsearch.ElasticSearchClient;
import org.skywalking.apm.collector.cluster.ClusterModule;
import org.skywalking.apm.collector.cluster.service.ModuleListenerService;
import org.skywalking.apm.collector.cluster.service.ModuleRegisterService;
import org.skywalking.apm.collector.core.module.Module;
import org.skywalking.apm.collector.core.module.ModuleProvider;
import org.skywalking.apm.collector.core.module.ServiceNotProvidedException;
import org.skywalking.apm.collector.storage.StorageException;
import org.skywalking.apm.collector.storage.StorageModule;
import org.skywalking.apm.collector.storage.base.dao.IBatchDAO;
import org.skywalking.apm.collector.storage.dao.*;
import org.skywalking.apm.collector.storage.es.base.dao.BatchEsDAO;
import org.skywalking.apm.collector.storage.es.base.define.ElasticSearchStorageInstaller;
import org.skywalking.apm.collector.storage.es.dao.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.UUID;

/**
 * 基于 ES 的存储组件服务提供者
 *
 * @author peng-yongsheng
 */
public class StorageModuleEsProvider extends ModuleProvider {

    private final Logger logger = LoggerFactory.getLogger(StorageModuleEsProvider.class);

    public static final String NAME = "elasticsearch";
    private static final String CLUSTER_NAME = "cluster_name";
    private static final String CLUSTER_TRANSPORT_SNIFFER = "cluster_transport_sniffer";
    private static final String CLUSTER_NODES = "cluster_nodes";
    private static final String INDEX_SHARDS_NUMBER = "index_shards_number";
    private static final String INDEX_REPLICAS_NUMBER = "index_replicas_number";
    private static final String TIME_TO_LIVE_OF_DATA = "ttl";

    private ElasticSearchClient elasticSearchClient;
    private DataTTLKeeperTimer deleteTimer;

    @Override public String name() {
        return NAME;
    }

    @Override public Class<? extends Module> module() {
        return StorageModule.class;
    }

    @Override public void prepare(Properties config) throws ServiceNotProvidedException {
        // 创建 ElasticSearchClient 对象
        String clusterName = config.getProperty(CLUSTER_NAME);
        Boolean clusterTransportSniffer = (Boolean)config.get(CLUSTER_TRANSPORT_SNIFFER);
        String clusterNodes = config.getProperty(CLUSTER_NODES);
        elasticSearchClient = new ElasticSearchClient(clusterName, clusterTransportSniffer, clusterNodes);

        // 创建并注册 DAO 对象
        this.registerServiceImplementation(IBatchDAO.class, new BatchEsDAO(elasticSearchClient));
        registerCacheDAO();
        registerRegisterDAO();
        registerPersistenceDAO();
        registerUiDAO();
    }

    @Override public void start(Properties config) throws ServiceNotProvidedException {
        Integer indexShardsNumber = (Integer)config.get(INDEX_SHARDS_NUMBER);
        Integer indexReplicasNumber = (Integer)config.get(INDEX_REPLICAS_NUMBER);
        try {
            // 初始化 ElasticSearchClient 对象
            elasticSearchClient.initialize();

            // 创建 ElasticSearchStorageInstaller 对象，初始化存储组件的表
            ElasticSearchStorageInstaller installer = new ElasticSearchStorageInstaller(indexShardsNumber, indexReplicasNumber);
            installer.install(elasticSearchClient);
        } catch (ClientException | StorageException e) {
            logger.error(e.getMessage(), e);
        }

        // 创建 StorageModuleEsRegistration 对象，并注册信息到集群管理
        String uuId = UUID.randomUUID().toString();
        ModuleRegisterService moduleRegisterService = getManager().find(ClusterModule.NAME).getService(ModuleRegisterService.class);
        moduleRegisterService.register(StorageModule.NAME, this.name(), new StorageModuleEsRegistration(uuId, 0));

        // 创建 StorageModuleEsNamingListener 对象，并添加监听器到集群管理
        StorageModuleEsNamingListener namingListener = new StorageModuleEsNamingListener();
        ModuleListenerService moduleListenerService = getManager().find(ClusterModule.NAME).getService(ModuleListenerService.class);
        moduleListenerService.addListener(namingListener);

        // 创建 DataTTLKeeperTimer 对象
        Integer beforeDay = (Integer)config.getOrDefault(TIME_TO_LIVE_OF_DATA, 3);
        deleteTimer = new DataTTLKeeperTimer(getManager(), namingListener, uuId + 0, beforeDay);
    }

    @Override public void notifyAfterCompleted() throws ServiceNotProvidedException {
        // 启动 DataTTLKeeperTimer 对象
        deleteTimer.start();
    }

    @Override public String[] requiredModules() {
        return new String[] {ClusterModule.NAME};
    }

    private void registerCacheDAO() throws ServiceNotProvidedException {
        this.registerServiceImplementation(IApplicationCacheDAO.class, new ApplicationEsCacheDAO(elasticSearchClient));
        this.registerServiceImplementation(IInstanceCacheDAO.class, new InstanceEsCacheDAO(elasticSearchClient));
        this.registerServiceImplementation(IServiceNameCacheDAO.class, new ServiceNameEsCacheDAO(elasticSearchClient));
    }

    private void registerRegisterDAO() throws ServiceNotProvidedException {
        this.registerServiceImplementation(IApplicationRegisterDAO.class, new ApplicationEsRegisterDAO(elasticSearchClient));
        this.registerServiceImplementation(IInstanceRegisterDAO.class, new InstanceEsRegisterDAO(elasticSearchClient));
        this.registerServiceImplementation(IServiceNameRegisterDAO.class, new ServiceNameEsRegisterDAO(elasticSearchClient));
    }

    private void registerPersistenceDAO() throws ServiceNotProvidedException {
        this.registerServiceImplementation(ICpuMetricPersistenceDAO.class, new CpuMetricEsPersistenceDAO(elasticSearchClient));
        this.registerServiceImplementation(IGCMetricPersistenceDAO.class, new GCMetricEsPersistenceDAO(elasticSearchClient));
        this.registerServiceImplementation(IMemoryMetricPersistenceDAO.class, new MemoryMetricEsPersistenceDAO(elasticSearchClient));
        this.registerServiceImplementation(IMemoryPoolMetricPersistenceDAO.class, new MemoryPoolMetricEsPersistenceDAO(elasticSearchClient));

        this.registerServiceImplementation(IGlobalTracePersistenceDAO.class, new GlobalTraceEsPersistenceDAO(elasticSearchClient));
        this.registerServiceImplementation(IInstPerformancePersistenceDAO.class, new InstPerformanceEsPersistenceDAO(elasticSearchClient));
        this.registerServiceImplementation(INodeComponentPersistenceDAO.class, new NodeComponentEsPersistenceDAO(elasticSearchClient));
        this.registerServiceImplementation(INodeMappingPersistenceDAO.class, new NodeMappingEsPersistenceDAO(elasticSearchClient));
        this.registerServiceImplementation(INodeReferencePersistenceDAO.class, new NodeReferenceEsPersistenceDAO(elasticSearchClient));
        this.registerServiceImplementation(ISegmentCostPersistenceDAO.class, new SegmentCostEsPersistenceDAO(elasticSearchClient));
        this.registerServiceImplementation(ISegmentPersistenceDAO.class, new SegmentEsPersistenceDAO(elasticSearchClient));
        this.registerServiceImplementation(IServiceEntryPersistenceDAO.class, new ServiceEntryEsPersistenceDAO(elasticSearchClient));
        this.registerServiceImplementation(IServiceReferencePersistenceDAO.class, new ServiceReferenceEsPersistenceDAO(elasticSearchClient));

        this.registerServiceImplementation(IInstanceHeartBeatPersistenceDAO.class, new InstanceHeartBeatEsPersistenceDAO(elasticSearchClient));
    }

    private void registerUiDAO() throws ServiceNotProvidedException {
        this.registerServiceImplementation(IInstanceUIDAO.class, new InstanceEsUIDAO(elasticSearchClient));

        this.registerServiceImplementation(ICpuMetricUIDAO.class, new CpuMetricEsUIDAO(elasticSearchClient));
        this.registerServiceImplementation(IGCMetricUIDAO.class, new GCMetricEsUIDAO(elasticSearchClient));
        this.registerServiceImplementation(IMemoryMetricUIDAO.class, new MemoryMetricEsUIDAO(elasticSearchClient));
        this.registerServiceImplementation(IMemoryPoolMetricUIDAO.class, new MemoryPoolMetricEsUIDAO(elasticSearchClient));

        this.registerServiceImplementation(IGlobalTraceUIDAO.class, new GlobalTraceEsUIDAO(elasticSearchClient));
        this.registerServiceImplementation(IInstPerformanceUIDAO.class, new InstPerformanceEsUIDAO(elasticSearchClient));
        this.registerServiceImplementation(INodeComponentUIDAO.class, new NodeComponentEsUIDAO(elasticSearchClient));
        this.registerServiceImplementation(INodeMappingUIDAO.class, new NodeMappingEsUIDAO(elasticSearchClient));
        this.registerServiceImplementation(INodeReferenceUIDAO.class, new NodeReferenceEsUIDAO(elasticSearchClient));
        this.registerServiceImplementation(ISegmentCostUIDAO.class, new SegmentCostEsUIDAO(elasticSearchClient));
        this.registerServiceImplementation(ISegmentUIDAO.class, new SegmentEsUIDAO(elasticSearchClient));
        this.registerServiceImplementation(IServiceEntryUIDAO.class, new ServiceEntryEsUIDAO(elasticSearchClient));
        this.registerServiceImplementation(IServiceReferenceUIDAO.class, new ServiceReferenceEsUIDAO(elasticSearchClient));
    }
}
