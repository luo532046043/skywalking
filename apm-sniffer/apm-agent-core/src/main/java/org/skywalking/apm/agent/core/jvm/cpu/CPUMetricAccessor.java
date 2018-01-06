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

package org.skywalking.apm.agent.core.jvm.cpu;

import org.skywalking.apm.network.proto.CPU;

/**
 * CPU 指标访问器
 *
 * @author wusheng
 */
public abstract class CPUMetricAccessor {

    /**
     * 获得进程占用 CPU 时长，单位：纳秒
     */
    private long lastCPUTimeNs;
    /**
     * 最后取样时间，单位：纳秒
     */
    private long lastSampleTimeNs;
    /**
     * CPU 数量
     */
    private final int cpuCoreNum;

    public CPUMetricAccessor(int cpuCoreNum) {
        this.cpuCoreNum = cpuCoreNum;
    }

    protected void init() {
        lastCPUTimeNs = this.getCpuTime();
        this.lastSampleTimeNs = System.nanoTime();
    }

    /**
     * @return CPU 时间
     */
    protected abstract long getCpuTime();

    public CPU getCPUMetric() {
        long cpuTime = this.getCpuTime();
        long cpuCost = cpuTime - lastCPUTimeNs;
        long now = System.nanoTime();

        // CPU 占用率 = CPU 占用时间 / ( 过去时间 * CPU 数量 )
        CPU.Builder cpuBuilder = CPU.newBuilder();
        return cpuBuilder.setUsagePercent(cpuCost * 1.0d / ((now - lastSampleTimeNs) * cpuCoreNum)).build();
    }

}
