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

package org.skywalking.apm.collector.ui.jetty.handler.application;

import com.google.gson.JsonElement;
import org.skywalking.apm.collector.core.module.ModuleManager;
import org.skywalking.apm.collector.server.jetty.ArgumentsParseException;
import org.skywalking.apm.collector.server.jetty.JettyHandler;
import org.skywalking.apm.collector.ui.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * 获得应用列表逻辑处理器
 *
 * @author peng-yongsheng
 */
public class ApplicationsGetHandler extends JettyHandler {

    private final Logger logger = LoggerFactory.getLogger(ApplicationsGetHandler.class);

    @Override public String pathSpec() {
        return "/applications";
    }

    private final ApplicationService applicationService;

    public ApplicationsGetHandler(ModuleManager moduleManager) {
        this.applicationService = new ApplicationService(moduleManager);
    }

    @Override protected JsonElement doGet(HttpServletRequest req) throws ArgumentsParseException {
        if (!req.getParameterMap().containsKey("startTime") || !req.getParameterMap().containsKey("endTime")) {
            throw new ArgumentsParseException("must contains startTime. endTime parameter");
        }

        String startTimeStr = req.getParameter("startTime");
        String endTimeStr = req.getParameter("endTime");
        logger.debug("applications get start time: {}, end time: {}", startTimeStr, endTimeStr);

        // 解析 开始时间参数
        long startTime;
        try {
            startTime = Long.parseLong(startTimeStr);
        } catch (NumberFormatException e) {
            throw new ArgumentsParseException("start time must be long");
        }

        // 解析 结束时间参数
        long endTime;
        try {
            endTime = Long.parseLong(endTimeStr);
        } catch (NumberFormatException e) {
            throw new ArgumentsParseException("end time must be long");
        }

        // 以应用编号为聚合，获得应用实例数量数组
        return applicationService.getApplications(startTime, endTime);
    }

    @Override protected JsonElement doPost(HttpServletRequest req) throws ArgumentsParseException {
        throw new UnsupportedOperationException();
    }
}
