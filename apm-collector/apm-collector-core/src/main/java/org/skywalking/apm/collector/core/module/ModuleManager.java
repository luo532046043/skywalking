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

package org.skywalking.apm.collector.core.module;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * 组件管理器
 *
 * The <code>ModuleManager</code> takes charge of all {@link Module}s in collector.
 *
 * @author wu-sheng, peng-yongsheng
 */
public class ModuleManager {

    /**
     * 加载的组件实例的映射
     * key ：组件名
     */
    private Map<String, Module> loadedModules = new HashMap<>();

    /**
     * 初始化组件们
     *
     * Init the given modules
     *
     * @param applicationConfiguration Collector 配置对象
     */
    public void init(
        ApplicationConfiguration applicationConfiguration) throws ModuleNotFoundException, ProviderNotFoundException, ServiceNotProvidedException, CycleDependencyException {
        // 加载 所有 Module 实现类的实例数组
        String[] moduleNames = applicationConfiguration.moduleList();
        ServiceLoader<Module> moduleServiceLoader = ServiceLoader.load(Module.class);
        // 循环 所有 Module 实现类的实例数组，添加到 loadedModules
        LinkedList<String> moduleList = new LinkedList<>(Arrays.asList(moduleNames));
        for (Module module : moduleServiceLoader) {
            for (String moduleName : moduleNames) {
                if (moduleName.equals(module.name())) {
                    // 创建 组件
                    Module newInstance;
                    try {
                        newInstance = module.getClass().newInstance();
                    } catch (InstantiationException e) {
                        throw new ModuleNotFoundException(e);
                    } catch (IllegalAccessException e) {
                        throw new ModuleNotFoundException(e);
                    }
                    // 执行 组件准备阶段的逻辑
                    newInstance.prepare(this, applicationConfiguration.getModuleConfiguration(moduleName));
                    // 添加到 loadedModules
                    loadedModules.put(moduleName, newInstance);
                    moduleList.remove(moduleName);
                }
            }
        }

        // 校验所有组件们都初始化，否则抛出异常
        if (moduleList.size() > 0) {
            throw new ModuleNotFoundException(moduleList.toString() + " missing.");
        }

        BootstrapFlow bootstrapFlow = new BootstrapFlow(loadedModules, applicationConfiguration);
        // 执行 组件启动逻辑
        bootstrapFlow.start(this, applicationConfiguration);
        // 通知 组件服务提供者 组件已经启动完成
        bootstrapFlow.notifyAfterCompleted();
    }

    public boolean has(String moduleName) {
        return loadedModules.get(moduleName) != null;
    }

    public Module find(String moduleName) throws ModuleNotFoundRuntimeException {
        Module module = loadedModules.get(moduleName);
        if (module != null)
            return module;
        throw new ModuleNotFoundRuntimeException(moduleName + " missing.");
    }
}
