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

package org.skywalking.apm.collector.boot.config;

import org.skywalking.apm.collector.core.module.ApplicationConfiguration;
import org.skywalking.apm.collector.core.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;

/**
 * {@link ApplicationConfiguration} 加载器
 *
 * @author peng-yongsheng
 */
public class ApplicationConfigLoader implements ConfigLoader<ApplicationConfiguration> {

    private final Logger logger = LoggerFactory.getLogger(ApplicationConfigLoader.class);

    private final Yaml yaml = new Yaml();

    @Override public ApplicationConfiguration load() throws ConfigFileNotFoundException {
        // 创建 Collector配置对象
        ApplicationConfiguration configuration = new ApplicationConfiguration();
        // 加载 自定义配置
        this.loadConfig(configuration);
        // 加载 默认配置
        this.loadDefaultConfig(configuration);
        return configuration;
    }

    private void loadConfig(ApplicationConfiguration configuration) throws ConfigFileNotFoundException {
        try {
            // 从 YAML 配置，读取模块配置映射
            Reader applicationReader = ResourceUtils.read("application.yml");
            Map<String, Map<String, Map<String, ?>>> moduleConfig = yaml.loadAs(applicationReader, Map.class);
            // 循环 模块配置映射 ，添加 模块配置对象 到 Collector配置对象
            moduleConfig.forEach((moduleName, providerConfig) -> {
                if (providerConfig.size() > 0) {
                    logger.info("Get a module define from application.yml, module name: {}", moduleName);
                    // 创建 模块配置对象
                    ApplicationConfiguration.ModuleConfiguration moduleConfiguration = configuration.addModule(moduleName);
                    // 循环 模块服务提供者配置映射
                    providerConfig.forEach((name, propertiesConfig) -> {
                        logger.info("Get a provider define belong to {} module, provider name: {}", moduleName, name);
                        Properties properties = new Properties();
                        if (propertiesConfig != null) {
                            propertiesConfig.forEach((key, value) -> {
                                properties.put(key, value);
                                logger.info("The property with key: {}, value: {}, in {} provider", key, value, name);
                            });
                        }
                        // 添加 模块服务提供者配置对象
                        moduleConfiguration.addProviderConfiguration(name, properties);
                    });
                } else {
                    logger.warn("Get a module define from application.yml, but no provider define, use default, module name: {}", moduleName);
                }
            });
        } catch (FileNotFoundException e) {
            throw new ConfigFileNotFoundException(e.getMessage(), e);
        }
    }

    private void loadDefaultConfig(ApplicationConfiguration configuration) throws ConfigFileNotFoundException {
        try {
            // 从 YAML 配置，读取模块配置映射
            Reader applicationReader = ResourceUtils.read("application-default.yml");
            Map<String, Map<String, Map<String, ?>>> moduleConfig = yaml.loadAs(applicationReader, Map.class);
            // 循环 模块配置映射 ，添加 模块配置对象 到 Collector配置对象
            moduleConfig.forEach((moduleName, providerConfig) -> {
                if (!configuration.has(moduleName)) { // 😈 模块配置不存在，使用默认配置
                    logger.warn("The {} module did't define in application.yml, use default", moduleName);
                    // 创建 模块配置对象
                    ApplicationConfiguration.ModuleConfiguration moduleConfiguration = configuration.addModule(moduleName);
                    providerConfig.forEach((name, propertiesConfig) -> {
                        Properties properties = new Properties();
                        if (propertiesConfig != null) {
                            propertiesConfig.forEach(properties::put);
                        }
                        // 添加 模块服务提供者配置对象
                        moduleConfiguration.addProviderConfiguration(name, properties);
                    });
                }
            });
        } catch (FileNotFoundException e) {
            throw new ConfigFileNotFoundException(e.getMessage(), e);
        }
    }
}
