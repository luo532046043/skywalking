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
 * 字典工具类
 *
 * @author wusheng
 */
public class DictionaryUtil {

    /**
     * @return 空值
     */
    public static int nullValue() {
        return 0;
    }

    /**
     * 判断是否为空值
     *
     * @param id 编号
     * @return 是否为空值
     */
    public static boolean isNull(int id) {
        return id == nullValue();
    }

}
