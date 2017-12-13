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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 组件抽象类
 *
 * A module definition.
 *
 * @author wu-sheng, peng-yongsheng
 */
public abstract class Module {

    private final Logger logger = LoggerFactory.getLogger(Module.class);

    /**
     * 加载的组件服务提供者的数组
     * key ：组件服务提供者的名字
     *
     * 目前一个组件只能有一个服务提供者 {@link #provider()}
     */
    private LinkedList<ModuleProvider> loadedProviders = new LinkedList<>();

    /**
     * @return the module name
     */
    public abstract String name();

    /**
     * @return the {@link Service} provided by this module.
     */
    public abstract Class[] services();

    /**
     * 执行组件准备阶段的逻辑，初始化它的组件服务提供者，并执行组件服务提供者的准备阶段的逻辑
     *
     * Run the prepare stage for the module, including finding all potential providers, and asking them to prepare.
     *
     * @param moduleManager of this module
     * @param configuration of this module
     * @throws ProviderNotFoundException when even don't find a single one providers.
     */
    void prepare(ModuleManager moduleManager,
        ApplicationConfiguration.ModuleConfiguration configuration) throws ProviderNotFoundException, ServiceNotProvidedException {
        // 加载 所有 ModuleProvider 实现类的实例数组
        ServiceLoader<ModuleProvider> moduleProviderLoader = ServiceLoader.load(ModuleProvider.class);
        boolean providerExist = false;
        // 循环 所有 ModuleProvider 实现类的实例数组，添加到 loadedProviders
        for (ModuleProvider provider : moduleProviderLoader) {
            // 跳过不属于自己的 ModuleProvider
            if (!configuration.has(provider.name())) { // 在配置中
                continue;
            }
            // 创建 组件服务提供者
            providerExist = true;
            if (provider.module().equals(getClass())) { // 类型匹配
                ModuleProvider newProvider;
                try {
                    newProvider = provider.getClass().newInstance();
                } catch (InstantiationException e) {
                    throw new ProviderNotFoundException(e);
                } catch (IllegalAccessException e) {
                    throw new ProviderNotFoundException(e);
                }
                newProvider.setManager(moduleManager);
                newProvider.setModule(this);
                // 添加到 loadedProviders
                loadedProviders.add(newProvider);
            }
        }

        // 校验有组件服务提供者初始化，否则抛出异常
        if (!providerExist) {
            throw new ProviderNotFoundException(this.name() + " module no provider exists.");
        }

        // 执行组件服务提供者准备阶段的逻辑
        for (ModuleProvider moduleProvider : loadedProviders) {
            logger.info("Prepare the {} provider in {} module.", moduleProvider.name(), this.name());
            moduleProvider.prepare(configuration.getProviderConfiguration(moduleProvider.name()));
        }
    }

    /**
     * @return providers of this module
     */
    final List<ModuleProvider> providers() {
        return loadedProviders;
    }

    final ModuleProvider provider() throws ProviderNotFoundException, DuplicateProviderException {
        if (loadedProviders.size() > 1) {
            throw new DuplicateProviderException(this.name() + " module exist " + loadedProviders.size() + " providers");
        }

        return loadedProviders.getFirst();
    }

    public final <T extends Service> T getService(Class<T> serviceType) throws ServiceNotProvidedRuntimeException {
        try {
            return provider().getService(serviceType);
        } catch (ProviderNotFoundException | DuplicateProviderException | ServiceNotProvidedException e) {
            throw new ServiceNotProvidedRuntimeException(e.getMessage());
        }
    }
}
