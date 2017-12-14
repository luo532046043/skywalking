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

package org.skywalking.apm.collector.server.jetty;

import com.google.gson.JsonElement;
import org.skywalking.apm.collector.core.util.ObjectUtils;
import org.skywalking.apm.collector.server.ServerHandler;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

/**
 * Jetty 请求处理器
 *
 * @author peng-yongsheng
 */
public abstract class JettyHandler extends HttpServlet implements ServerHandler {

    /**
     * @return 路径定义
     */
    public abstract String pathSpec();

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            reply(resp, doGet(req));
        } catch (ArgumentsParseException e) {
            replyError(resp, e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * 处理 Get 请求，返回 JSON
     *
     * @param req 请求
     * @return JSON
     * @throws ArgumentsParseException 当参数解析异常
     */
    protected abstract JsonElement doGet(HttpServletRequest req) throws ArgumentsParseException;

    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            reply(resp, doPost(req));
        } catch (ArgumentsParseException e) {
            replyError(resp, e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * 处理 Post 请求，返回 JSON
     *
     * @param req 请求
     * @return JSON
     * @throws ArgumentsParseException 当参数解析异常
     */
    protected abstract JsonElement doPost(HttpServletRequest req) throws ArgumentsParseException;

    @Override
    protected final void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doHead(req, resp);
    }

    @Override protected final long getLastModified(HttpServletRequest req) {
        return super.getLastModified(req);
    }

    @Override
    protected final void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPut(req, resp);
    }

    @Override
    protected final void doDelete(HttpServletRequest req,
        HttpServletResponse resp) throws ServletException, IOException {
        super.doDelete(req, resp);
    }

    @Override
    protected final void doOptions(HttpServletRequest req,
        HttpServletResponse resp) throws ServletException, IOException {
        super.doOptions(req, resp);
    }

    @Override
    protected final void doTrace(HttpServletRequest req,
        HttpServletResponse resp) throws ServletException, IOException {
        super.doTrace(req, resp);
    }

    @Override
    protected final void service(HttpServletRequest req,
        HttpServletResponse resp) throws ServletException, IOException {
        super.service(req, resp);
    }

    @Override public final void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        super.service(req, res);
    }

    @Override public final void destroy() {
        super.destroy();
    }

    @Override public final String getInitParameter(String name) {
        return super.getInitParameter(name);
    }

    @Override public final Enumeration<String> getInitParameterNames() {
        return super.getInitParameterNames();
    }

    @Override public final ServletConfig getServletConfig() {
        return super.getServletConfig();
    }

    @Override public final ServletContext getServletContext() {
        return super.getServletContext();
    }

    @Override public final String getServletInfo() {
        return super.getServletInfo();
    }

    @Override public final void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override public final void init() throws ServletException {
        super.init();
    }

    @Override public final void log(String msg) {
        super.log(msg);
    }

    @Override public final void log(String message, Throwable t) {
        super.log(message, t);
    }

    @Override public final String getServletName() {
        return super.getServletName();
    }

    /**
     * 响应成功
     *
     * @param response 响应
     * @param resJson JSON
     * @throws IOException 当 IO 发生异常
     */
    private void reply(HttpServletResponse response, JsonElement resJson) throws IOException {
        response.setContentType("text/json");
        response.setCharacterEncoding("utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter out = response.getWriter();
        if (ObjectUtils.isNotEmpty(resJson)) {
            out.print(resJson);
        }
        out.flush();
        out.close();
    }

    /**
     * 响应错误
     *
     * @param response 响应
     * @param errorMessage 消息
     * @param status 状态码
     * @throws IOException 当 IO 发生异常
     */
    private void replyError(HttpServletResponse response, String errorMessage, int status) throws IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("utf-8");
        response.setStatus(status);
        response.setHeader("error-message", errorMessage);

        PrintWriter out = response.getWriter();
        out.flush();
        out.close();
    }
}