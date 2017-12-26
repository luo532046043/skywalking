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

package org.skywalking.apm.collector.agent.stream;

/**
 * ID 自增器
 *
 * @author peng-yongsheng
 */
public enum IdAutoIncrement {
    INSTANCE;

    /**
     * 双向均匀自增
     *
     * @param min 最小值
     * @param max 最大值
     * @return 自增值
     */
    public int increment(int min, int max) {
        int instanceId;
        if (min == max) { // 起点
            instanceId = -1;
        } else if (min + max == 0) { // max 过小
            instanceId = max + 1;
        } else if (min + max > 0) { // max 过大
            instanceId = min - 1;
        } else if (max < 0) { // max 小于 0
            instanceId = 1;
        } else { // max 过小
            instanceId = max + 1;
        }
        return instanceId;
    }

}
