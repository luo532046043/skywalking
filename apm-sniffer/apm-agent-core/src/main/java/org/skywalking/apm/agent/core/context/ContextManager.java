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

package org.skywalking.apm.agent.core.context;

import org.skywalking.apm.agent.core.boot.BootService;
import org.skywalking.apm.agent.core.boot.ServiceManager;
import org.skywalking.apm.agent.core.conf.Config;
import org.skywalking.apm.agent.core.conf.RemoteDownstreamConfig;
import org.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.skywalking.apm.agent.core.dictionary.DictionaryUtil;
import org.skywalking.apm.agent.core.sampling.SamplingService;
import org.skywalking.apm.agent.core.logging.api.ILog;
import org.skywalking.apm.agent.core.logging.api.LogManager;
import org.skywalking.apm.util.StringUtil;

/**
 * {@link ContextManager} controls the whole context of {@link TraceSegment}. Any {@link TraceSegment} relates to
 * single-thread, so this context use {@link ThreadLocal} to maintain the context, and make sure, since a {@link
 * TraceSegment} starts, all ChildOf spans are in the same context.
 * <p>
 * What is 'ChildOf'? {@see
 * https://github.com/opentracing/specification/blob/master/specification.md#references-between-spans}
 *
 * <p> Also, {@link ContextManager} delegates to all {@link AbstractTracerContext}'s major methods.
 *
 * @author wusheng
 */
public class ContextManager implements TracingContextListener, BootService, IgnoreTracerContextListener {

    private static final ILog logger = LogManager.getLogger(ContextManager.class);

    /**
     * 线程变量，AbstractTracerContext 对象
     */
    private static ThreadLocal<AbstractTracerContext> CONTEXT = new ThreadLocal<AbstractTracerContext>();

    /**
     * 获取 AbstractTracerContext 对象。若不存在，进行创建。
     *
     * @param operationName 操作名
     * @param forceSampling 是否强制收集
     * @return AbstractTracerContext 对象
     */
    private static AbstractTracerContext getOrCreate(String operationName, boolean forceSampling) {
        AbstractTracerContext context = CONTEXT.get();
        if (context == null) {
            // 操作名，创建 IgnoredTracerContext 对象
            if (StringUtil.isEmpty(operationName)) {
                if (logger.isDebugEnable()) {
                    logger.debug("No operation name, ignore this trace.");
                }
                context = new IgnoredTracerContext();
            } else {
                // 应用实例已经注册
                if (RemoteDownstreamConfig.Agent.APPLICATION_ID != DictionaryUtil.nullValue()
                    && RemoteDownstreamConfig.Agent.APPLICATION_INSTANCE_ID != DictionaryUtil.nullValue()
                    ) {
                    // 根据操作名后缀判断是否是忽略的操作，创建 IgnoredTracerContext 对象
                    int suffixIdx = operationName.lastIndexOf(".");
                    if (suffixIdx > -1 && Config.Agent.IGNORE_SUFFIX.contains(operationName.substring(suffixIdx))) { // 忽略的操作名，例如操作名为 .jpg,.jpeg,.js,.css,.png,.bmp,.gif,.ico,.mp3,.mp4,.html,.svg
                        context = new IgnoredTracerContext();
                    } else {
                        // 强制收集或者需要收集，创建 TracingContext 对象
                        SamplingService samplingService = ServiceManager.INSTANCE.findService(SamplingService.class);
                        if (forceSampling || samplingService.trySampling()) {
                            context = new TracingContext();
                        } else {
                        // 无需收集，创建 IgnoredTracerContext
                            context = new IgnoredTracerContext();
                        }
                    }
                // 应用实例未注册，创建 IgnoredTracerContext 对象
                } else {
                    /**
                     * Can't register to collector, no need to trace anything.
                     */
                    context = new IgnoredTracerContext();
                }
            }
            CONTEXT.set(context);
        }
        return context;
    }

    private static AbstractTracerContext get() {
        return CONTEXT.get();
    }

    /**
     * @return the first global trace id if needEnhance. Otherwise, "N/A".
     */
    public static String getGlobalTraceId() {
        AbstractTracerContext segment = CONTEXT.get();
        if (segment == null) {
            return "N/A";
        } else {
            return segment.getReadableGlobalTraceId();
        }
    }

    public static AbstractSpan createEntrySpan(String operationName, ContextCarrier carrier) {
        SamplingService samplingService = ServiceManager.INSTANCE.findService(SamplingService.class);
        AbstractTracerContext context;
        if (carrier != null && carrier.isValid()) {
            // 强制收集，因为传递的链路汇总
            samplingService.forceSampled();
            // 获得 AbstractTracerContext
            context = getOrCreate(operationName, true);
            // 提取 ContextCarrier 到 AbstractTracerContext
            context.extract(carrier);
        } else {
            // 获得 AbstractTracerContext
            context = getOrCreate(operationName, false);
        }
        // 创建 EntrySpan
        return context.createEntrySpan(operationName);
    }

    public static AbstractSpan createLocalSpan(String operationName) {
        AbstractTracerContext context = getOrCreate(operationName, false);
        return context.createLocalSpan(operationName);
    }

    public static AbstractSpan createExitSpan(String operationName, ContextCarrier carrier, String remotePeer) {
        if (carrier == null) {
            throw new IllegalArgumentException("ContextCarrier can't be null.");
        }
        AbstractTracerContext context = getOrCreate(operationName, false);
        AbstractSpan span = context.createExitSpan(operationName, remotePeer);
        context.inject(carrier);
        return span;
    }

    public static AbstractSpan createExitSpan(String operationName, String remotePeer) {
        AbstractTracerContext context = getOrCreate(operationName, false);
        AbstractSpan span = context.createExitSpan(operationName, remotePeer);
        return span;
    }

    public static void inject(ContextCarrier carrier) {
        get().inject(carrier);
    }

    public static void extract(ContextCarrier carrier) {
        if (carrier == null) {
            throw new IllegalArgumentException("ContextCarrier can't be null.");
        }
        if (carrier.isValid()) {
            get().extract(carrier);
        }
    }

    public static ContextSnapshot capture() {
        return get().capture();
    }

    public static void continued(ContextSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("ContextSnapshot can't be null.");
        }
        if (snapshot.isValid() && !snapshot.isFromCurrent()) {
            get().continued(snapshot);
        }
    }

    public static AbstractSpan activeSpan() {
        return get().activeSpan();
    }

    public static void stopSpan() {
        stopSpan(activeSpan());
    }

    public static void stopSpan(AbstractSpan span) {
        get().stopSpan(span);
    }

    @Override
    public void beforeBoot() throws Throwable {

    }

    @Override
    public void boot() {
        TracingContext.ListenerManager.add(this);
        IgnoredTracerContext.ListenerManager.add(this);
    }

    @Override
    public void afterBoot() throws Throwable {

    }

    @Override public void shutdown() throws Throwable {

    }

    @Override
    public void afterFinished(TraceSegment traceSegment) {
        CONTEXT.remove();
    }

    @Override
    public void afterFinished(IgnoredTracerContext traceSegment) {
        CONTEXT.remove();
    }
}
