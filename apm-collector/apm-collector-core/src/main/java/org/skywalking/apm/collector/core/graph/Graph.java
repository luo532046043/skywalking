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

package org.skywalking.apm.collector.core.graph;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 定义了一个数据在各个 Node 的处理拓扑图
 *
 * @author peng-yongsheng, wu-sheng
 */
public final class Graph<INPUT> {

    /**
     * Graph 编号
     */
    private int id;
    /**
     * 【首个】提交数据给 Node 的方式
     */
    private WayToNode entryWay;
    /**
     * 处理器编号与 Node 的映射
     * {@link NodeProcessor#id()}
     */
    private ConcurrentHashMap<Integer, Node> nodeIndex = new ConcurrentHashMap<>();

    Graph(int id) {
        this.id = id;
    }

    public void start(INPUT input) {
        entryWay.in(input);
    }

    public <OUTPUT> Node<INPUT, OUTPUT> addNode(NodeProcessor<INPUT, OUTPUT> nodeProcessor) {
        return addNode(new DirectWay(nodeProcessor));
    }

    public <OUTPUT> Node<INPUT, OUTPUT> addNode(WayToNode<INPUT, OUTPUT> entryWay) {
        synchronized (this) {
            // 赋值
            this.entryWay = entryWay;
            // 创建 Node ，并返回该 Node 对象
            this.entryWay.buildDestination(this);
            return entryWay.getDestination();
        }
    }

    /**
     * 校验 Node 的 NodeProcessor 在 Graph 里，编号唯一
     *
     * @param node Node
     */
    void checkForNewNode(Node node) {
        int nodeId = node.getHandler().id();
        if (nodeIndex.containsKey(nodeId)) {
            throw new PotentialCyclicGraphException("handler="
                + node.getHandler().getClass().getName()
                + " already exists in graph[" + id + "]");
        }
        nodeIndex.put(nodeId, node);
    }

    public GraphNodeFinder toFinder() {
        return new GraphNodeFinder(this);
    }

    ConcurrentHashMap<Integer, Node> getNodeIndex() {
        return nodeIndex;
    }

    int getId() {
        return id;
    }
}
