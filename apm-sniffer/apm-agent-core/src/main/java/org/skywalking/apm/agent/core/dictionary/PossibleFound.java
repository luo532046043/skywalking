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

package org.skywalking.apm.agent.core.dictionary;

/**
 * The <code>PossibleFound</code> represents a value, which may needEnhance or not.
 *
 * @author wusheng
 */
public abstract class PossibleFound {

    /**
     * 是否找到
     */
    private boolean found;
    /**
     * 找到的结果
     */
    private int value;

    PossibleFound(int value) {
        this.found = true;
        this.value = value;
    }

    PossibleFound() {
        this.found = false;
    }

    public void doInCondition(Found condition1, NotFound condition2) {
        if (found) {
            condition1.doProcess(value);
        } else {
            condition2.doProcess();
        }
    }

    /**
     * 根据查找结果，执行不同的逻辑
     *
     * @param condition1 找到的处理逻辑
     * @param condition2 找不到时的处理逻辑
     * @return 结果
     */
    public Object doInCondition(FoundAndObtain condition1, NotFoundAndObtain condition2) {
        if (found) {
            return condition1.doProcess(value);
        } else {
            return condition2.doProcess();
        }
    }

    /**
     * Found 时的处理逻辑接口
     */
    public interface Found {
        void doProcess(int value);
    }

    /**
     * NotFound 时的处理逻辑接口
     */
    public interface NotFound {
        void doProcess();
    }

    /**
     * Found 时的处理并返回结果的逻辑接口
     */
    public interface FoundAndObtain {
        Object doProcess(int value);
    }

    /**
     * NotFound 时的处理并返回结果的逻辑接口
     */
    public interface NotFoundAndObtain {
        Object doProcess();
    }
}
