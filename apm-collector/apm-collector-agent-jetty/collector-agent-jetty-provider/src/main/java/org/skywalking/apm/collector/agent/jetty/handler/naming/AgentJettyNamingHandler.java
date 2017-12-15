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

package org.skywalking.apm.collector.agent.jetty.handler.naming;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.skywalking.apm.collector.server.jetty.ArgumentsParseException;
import org.skywalking.apm.collector.server.jetty.JettyHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

/**
 * 基于 Jetty 的命名处理器
 *
 * @author peng-yongsheng
 */
public class AgentJettyNamingHandler extends JettyHandler {

    private final AgentJettyNamingListener namingListener;

    public AgentJettyNamingHandler(AgentJettyNamingListener namingListener) {
        this.namingListener = namingListener;
    }

    @Override public String pathSpec() {
        return "/agent/jetty";
    }

    @Override protected JsonElement doGet(HttpServletRequest req) throws ArgumentsParseException {
        Set<String> servers = namingListener.getAddresses();
        JsonArray serverArray = new JsonArray();
        servers.forEach(serverArray::add);
        return serverArray;
    }

    @Override protected JsonElement doPost(HttpServletRequest req) throws ArgumentsParseException {
        throw new UnsupportedOperationException();
    }
}
