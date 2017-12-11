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

package org.skywalking.apm.agent.core.plugin;

import net.bytebuddy.pool.TypePool;

import java.util.HashMap;
import java.util.Map;

/**
 * The <code>WitnessClassFinder</code> represents a pool of {@link TypePool}s,
 * each {@link TypePool} matches a {@link ClassLoader},
 * which helps to find the class define existed or not.
 *
 * @author wusheng
 */
public enum WitnessClassFinder {

    /**
     * 单例
     */
    INSTANCE;

    /**
     * ClassLoader 与 TypePool 映射
     */
    private Map<ClassLoader, TypePool> poolMap = new HashMap<ClassLoader, TypePool>();

    /**
     * 判断类加载器中，是否存在指定的见证类
     *
     * 基于 byte-buddy 的 TypePool 实现
     *
     * @param witnessClass 指定的鉴证类
     * @param classLoader for finding the witnessClass
     * @return true, if the given witnessClass exists, through the given classLoader.
     */
    public boolean exist(String witnessClass, ClassLoader classLoader) {
        // 获得 classLoader 对应的 TypePool
        ClassLoader mappingKey = classLoader == null ? NullClassLoader.INSTANCE : classLoader;
        if (!poolMap.containsKey(mappingKey)) {
            synchronized (poolMap) {
                if (!poolMap.containsKey(mappingKey)) {
                    TypePool classTypePool = classLoader == null ? TypePool.Default.ofClassPath() : TypePool.Default.of(classLoader);
                    poolMap.put(mappingKey, classTypePool);
                }
            }
        }
        // 判断是否存在
        TypePool typePool = poolMap.get(mappingKey);
        TypePool.Resolution witnessClassResolution = typePool.describe(witnessClass);
        return witnessClassResolution.isResolved();
    }
}

/**
 * 空的类加载器
 */
final class NullClassLoader extends ClassLoader {

    /**
     * 单例
     */
    static NullClassLoader INSTANCE = new NullClassLoader();
}
