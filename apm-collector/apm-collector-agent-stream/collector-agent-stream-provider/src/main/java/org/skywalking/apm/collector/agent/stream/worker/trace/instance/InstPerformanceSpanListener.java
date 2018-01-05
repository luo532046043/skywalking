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

package org.skywalking.apm.collector.agent.stream.worker.trace.instance;

import org.skywalking.apm.collector.agent.stream.graph.TraceStreamGraph;
import org.skywalking.apm.collector.agent.stream.parser.EntrySpanListener;
import org.skywalking.apm.collector.agent.stream.parser.FirstSpanListener;
import org.skywalking.apm.collector.agent.stream.parser.standardization.SpanDecorator;
import org.skywalking.apm.collector.core.graph.Graph;
import org.skywalking.apm.collector.core.graph.GraphManager;
import org.skywalking.apm.collector.core.util.Const;
import org.skywalking.apm.collector.core.util.TimeBucketUtils;
import org.skywalking.apm.collector.storage.table.instance.InstPerformance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * InstPerformance 的 SpanListener
 *
 * @author peng-yongsheng
 */
public class InstPerformanceSpanListener implements EntrySpanListener, FirstSpanListener {

    private final Logger logger = LoggerFactory.getLogger(InstPerformanceSpanListener.class);

    /**
     * 应用编号
     */
    private int applicationId;
    /**
     * 应用实例编号
     */
    private int instanceId;
    /**
     * 消耗时长
     */
    private long cost;
    /**
     * 时间
     */
    private long timeBucket;

    @Override
    public void parseEntry(SpanDecorator spanDecorator, int applicationId, int instanceId,
        String segmentId) {
    }

    @Override
    public void parseFirst(SpanDecorator spanDecorator, int applicationId, int instanceId,
        String segmentId) {
        this.applicationId = applicationId;
        this.instanceId = instanceId;
        this.cost = spanDecorator.getEndTime() - spanDecorator.getStartTime(); // 第一个 Span ，计算消耗时长
        timeBucket = TimeBucketUtils.INSTANCE.getSecondTimeBucket(spanDecorator.getStartTime());
    }

    @Override public void build() {
        // 创建 InstPerformance 对象
        InstPerformance instPerformance = new InstPerformance(timeBucket + Const.ID_SPLIT + instanceId);
        instPerformance.setApplicationId(applicationId);
        instPerformance.setInstanceId(instanceId);
        instPerformance.setCalls(1);
        instPerformance.setCostTotal(cost);
        instPerformance.setTimeBucket(timeBucket);

        // 流式计算
        Graph<InstPerformance> graph = GraphManager.INSTANCE.createIfAbsent(TraceStreamGraph.INST_PERFORMANCE_GRAPH_ID, InstPerformance.class);
        graph.start(instPerformance);
    }

}