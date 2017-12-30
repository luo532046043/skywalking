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

package org.skywalking.apm.agent.core.context.trace;

import org.skywalking.apm.agent.core.dictionary.DictionaryUtil;
import org.skywalking.apm.network.trace.component.Component;

/**
 * 入口 Span
 * The <code>EntrySpan</code> represents a service provider point, such as Tomcat server entrance.
 *
 * It is a start point of {@link TraceSegment}, even in a complex application, there maybe have multi-layer entry point,
 * the <code>EntrySpan</code> only represents the first one.
 *
 * But with the last <code>EntrySpan</code>'s tags and logs, which have more details about a service provider.
 *
 * Such as: Tomcat Embed -> Dubbox
 * The <code>EntrySpan</code> represents the Dubbox span.
 *
 * @author wusheng
 */
public class EntrySpan extends StackBasedTracingSpan {

    /**
     * 当前栈最大深度
     */
    private int currentMaxDepth;

    public EntrySpan(int spanId, int parentSpanId, String operationName) {
        super(spanId, parentSpanId, operationName);
        this.currentMaxDepth = 0;
    }

    public EntrySpan(int spanId, int parentSpanId, int operationId) {
        super(spanId, parentSpanId, operationId);
        this.currentMaxDepth = 0;
    }

    /**
     * Set the {@link #startTime}, when the first start, which means the first service provided.
     */
    @Override
    public EntrySpan start() {
        // 只有首次启动时，设置开始时间
        if ((currentMaxDepth = ++stackDepth) == 1) {
            super.start();
        }
        // 再次启动时，清空信息
        clearWhenRestart();
        return this;
    }

    @Override
    public EntrySpan tag(String key, String value) {
        // 只有最深的栈，才设置，否则 `#clearWhenRestart()` 没有意思。
        if (stackDepth == currentMaxDepth) {
            super.tag(key, value);
        }
        return this;
    }

    @Override
    public AbstractTracingSpan setLayer(SpanLayer layer) {
        // 只有最深的栈，才设置，否则 `#clearWhenRestart()` 没有意思。
        if (stackDepth == currentMaxDepth) {
            return super.setLayer(layer);
        } else {
            return this;
        }
    }

    @Override
    public AbstractTracingSpan setComponent(Component component) {
        // 只有最深的栈，才设置，否则 `#clearWhenRestart()` 没有意思。
        if (stackDepth == currentMaxDepth) {
            return super.setComponent(component);
        } else {
            return this;
        }
    }

    @Override
    public AbstractTracingSpan setComponent(String componentName) {
        // 只有最深的栈，才设置，否则 `#clearWhenRestart()` 没有意思。
        if (stackDepth == currentMaxDepth) {
            return super.setComponent(componentName);
        } else {
            return this;
        }
    }

    @Override
    public AbstractTracingSpan setOperationName(String operationName) {
        // 只有最深的栈，才设置，否则 `#clearWhenRestart()` 没有意思。
        if (stackDepth == currentMaxDepth) {
            return super.setOperationName(operationName);
        } else {
            return this;
        }
    }

    @Override
    public AbstractTracingSpan setOperationId(int operationId) {
        // 只有最深的栈，才设置，否则 `#clearWhenRestart()` 没有意思。
        if (stackDepth == currentMaxDepth) {
            return super.setOperationId(operationId);
        } else {
            return this;
        }
    }

    @Override
    public EntrySpan log(Throwable t) {
        super.log(t);
        return this;
    }

    @Override public boolean isEntry() {
        return true;
    }

    @Override public boolean isExit() {
        return false;
    }

    private void clearWhenRestart() {
        this.componentId = DictionaryUtil.nullValue();
        this.componentName = null;
        this.layer = null;
        this.logs = null;
        this.tags = null;
    }

}
