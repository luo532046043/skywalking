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

import org.skywalking.apm.agent.core.logging.api.ILog;
import org.skywalking.apm.agent.core.logging.api.LogManager;
import org.skywalking.apm.agent.core.plugin.exception.IllegalPluginDefineException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 插件定义配置
 */
public enum PluginCfg {

    /**
     * 单例
     */
    INSTANCE;

    private static final ILog logger = LogManager.getLogger(PluginCfg.class);

    /**
     * 插件定义数组
     */
    private List<PluginDefine> pluginClassList = new ArrayList<PluginDefine>();

    /**
     * 读取 skywalking-plugin.def 文件，添加到 pluginClassList
     *
     * @param input 输入流
     * @throws IOException 当读取异常
     */
    void load(InputStream input) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String pluginDefine = null;
            while ((pluginDefine = reader.readLine()) != null) { // 换行
                try {
                    if (pluginDefine == null || pluginDefine.trim().length() == 0) {
                        continue;
                    }
                    // 解析字符串，创建插件定义
                    PluginDefine plugin = PluginDefine.build(pluginDefine);
                    pluginClassList.add(plugin);
                } catch (IllegalPluginDefineException e) {
                    logger.error(e, "Failed to format plugin({}) define.", pluginDefine);
                }
            }
        } finally {
            input.close();
        }
    }

    public List<PluginDefine> getPluginClassList() {
        return pluginClassList;
    }

}
