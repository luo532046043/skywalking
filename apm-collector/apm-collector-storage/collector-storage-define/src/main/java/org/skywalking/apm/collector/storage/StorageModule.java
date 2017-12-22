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

import org.skywalking.apm.collector.core.module.Module;
import org.skywalking.apm.collector.storage.base.dao.IBatchDAO;
import org.skywalking.apm.collector.storage.dao.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 存储组件
 *
 * @author peng-yongsheng
 */
public class StorageModule extends Module {

    public static final String NAME = "storage";

    @Override public String name() {
        return NAME;
    }

    @Override public Class[] services() {
        List<Class> classes = new ArrayList<>();
        classes.add(IBatchDAO.class);

        addCacheDAO(classes);
        addRegisterDAO(classes);
        addPersistenceDAO(classes);
        addUiDAO(classes);

        return classes.toArray(new Class[] {});
    }

    private void addCacheDAO(List<Class> classes) {
        classes.add(IApplicationCacheDAO.class); // Application
        classes.add(IInstanceCacheDAO.class); // Instance
        classes.add(IServiceNameCacheDAO.class); // ServiceName
    }

    private void addRegisterDAO(List<Class> classes) {
        classes.add(IApplicationRegisterDAO.class); // Application
        classes.add(IInstanceRegisterDAO.class); // Instance
        classes.add(IServiceNameRegisterDAO.class); // ServiceName
    }

    private void addPersistenceDAO(List<Class> classes) {
        classes.add(ICpuMetricPersistenceDAO.class); // CpuMetric
        classes.add(IGCMetricPersistenceDAO.class); // CMetric
        classes.add(IMemoryMetricPersistenceDAO.class); // MemoryMetric
        classes.add(IMemoryPoolMetricPersistenceDAO.class); // MemoryPoolMetric

        classes.add(IGlobalTracePersistenceDAO.class); // GlobalTrace
        classes.add(IInstPerformancePersistenceDAO.class); // InstPerformance
        classes.add(INodeComponentPersistenceDAO.class); // NodeComponent
        classes.add(INodeMappingPersistenceDAO.class); // NodeMapping
        classes.add(INodeReferencePersistenceDAO.class); // NodeReference
        classes.add(ISegmentCostPersistenceDAO.class); // SegmentCost
        classes.add(ISegmentPersistenceDAO.class); // Segment
        classes.add(IServiceEntryPersistenceDAO.class); // ServiceEntry
        classes.add(IServiceReferencePersistenceDAO.class); // ServiceReference

        classes.add(IInstanceHeartBeatPersistenceDAO.class); // Instance
    }

    private void addUiDAO(List<Class> classes) {
        classes.add(IInstanceUIDAO.class); // Instance

        classes.add(ICpuMetricUIDAO.class); // CpuMetric
        classes.add(IGCMetricUIDAO.class); // CMetric
        classes.add(IMemoryMetricUIDAO.class); // MemoryMetric
        classes.add(IMemoryPoolMetricUIDAO.class); // MemoryPoolMetric

        classes.add(IGlobalTraceUIDAO.class); // GlobalTrace
        classes.add(IInstPerformanceUIDAO.class); // InstPerformance
        classes.add(INodeComponentUIDAO.class); // NodeComponent
        classes.add(INodeMappingUIDAO.class); // NodeMapping
        classes.add(INodeReferenceUIDAO.class); // NodeReference
        classes.add(ISegmentCostUIDAO.class); // SegmentCost
        classes.add(ISegmentUIDAO.class); // Segment
        classes.add(IServiceEntryUIDAO.class); // ServiceEntry
        classes.add(IServiceReferenceUIDAO.class); // ServiceReference
    }
}
