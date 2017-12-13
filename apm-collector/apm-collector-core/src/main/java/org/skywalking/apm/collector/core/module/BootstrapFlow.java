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

import org.skywalking.apm.collector.core.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 组件启动流程
 *
 * @author wu-sheng
 */
public class BootstrapFlow {
    private final Logger logger = LoggerFactory.getLogger(BootstrapFlow.class);

    private Map<String, Module> loadedModules;
    private ApplicationConfiguration applicationConfiguration;
    private List<ModuleProvider> startupSequence;

    public BootstrapFlow(Map<String, Module> loadedModules,
        ApplicationConfiguration applicationConfiguration) throws CycleDependencyException {
        this.loadedModules = loadedModules;
        this.applicationConfiguration = applicationConfiguration;
        startupSequence = new LinkedList<>();

        makeSequence();
    }

    void start(ModuleManager moduleManager,
        ApplicationConfiguration configuration) throws ProviderNotFoundException, ModuleNotFoundException, ServiceNotProvidedException {
        for (ModuleProvider provider : startupSequence) {
            // 校验 依赖Module 已经都存在
            String[] requiredModules = provider.requiredModules();
            if (requiredModules != null) {
                for (String module : requiredModules) {
                    if (!moduleManager.has(module)) {
                        throw new ModuleNotFoundException(module + " is required by " + provider.getModuleName()
                            + "." + provider.name() + ", but not found.");
                    }
                }
            }

            // 校验 ModuleProvider 包含的 Service 们都创建成功。
            logger.info("start the provider {} in {} module.", provider.name(), provider.getModuleName());
            provider.requiredCheck(provider.getModule().services());

            // 执行 ModuleProvider 启动阶段逻辑
            provider.start(configuration.getModuleConfiguration(provider.getModuleName()).getProviderConfiguration(provider.name()));
        }
    }

    void notifyAfterCompleted() throws ProviderNotFoundException, ModuleNotFoundException, ServiceNotProvidedException {
        for (ModuleProvider provider : startupSequence) {
            // 执行 ModuleProvider 启动完成阶段的逻辑
            provider.notifyAfterCompleted();
        }
    }

    /**
     * 生成 ModuleProvider 初始化顺序数组。并判断是否存在循环依赖。
     *
     * @throws CycleDependencyException 循环依赖异常
     */
    private void makeSequence() throws CycleDependencyException {
        // 获得 ModuleProvider 数组
        List<ModuleProvider> allProviders = new ArrayList<>();
        loadedModules.forEach((moduleName, module) -> {
            module.providers().forEach(provider -> {
                allProviders.add(provider);
            });
        });

        // 不断循环，直到全部移除
        while (true) {
            // 记录当前循环时，剩余 ModuleProvider 数组数量
            int numOfToBeSequenced = allProviders.size();
            for (int i = 0; i < allProviders.size(); i++) {
                ModuleProvider provider = allProviders.get(i);
                String[] requiredModules = provider.requiredModules();
                if (CollectionUtils.isNotEmpty(requiredModules)) {
                    boolean isAllRequiredModuleStarted = true;
                    for (String module : requiredModules) {
                        // find module in all ready existed startupSequence
                        boolean exist = false;
                        for (ModuleProvider moduleProvider : startupSequence) {
                            if (moduleProvider.getModuleName().equals(module)) {
                                exist = true;
                                break;
                            }
                        }
                        if (!exist) {
                            isAllRequiredModuleStarted = false;
                            break;
                        }
                    }

                    if (isAllRequiredModuleStarted) { // 依赖存在，移除
                        startupSequence.add(provider);
                        allProviders.remove(i);
                        i--;
                    }
                } else { // 无依赖，移除
                    startupSequence.add(provider);
                    allProviders.remove(i);
                    i--;
                }
            }

            // 当前循环无移除，说明存在循环依赖
            if (numOfToBeSequenced == allProviders.size()) {
                StringBuilder unsequencedProviders = new StringBuilder();
                allProviders.forEach(provider -> {
                    unsequencedProviders.append(provider.getModuleName()).append("[provider=").append(provider.getClass().getName()).append("]\n");
                });
                throw new CycleDependencyException("Exist cycle module dependencies in \n" + unsequencedProviders.substring(0, unsequencedProviders.length() - 1));
            }

            // 剩余 ModuleProvider 数组已经全部移除，结束循环
            if (allProviders.size() == 0) {
                break;
            }
        }
    }
}
