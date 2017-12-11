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

package org.skywalking.apm.agent.core.plugin.match;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.skywalking.apm.agent.core.plugin.PluginFinder;

/**
 * All implementations can't direct match the class like {@link NameMatch} did.
 *
 * 间接匹配接口。相比 NameMatch 来说，确实比较 "委婉" 🙂 。
 *
 * @author wusheng
 */
public interface IndirectMatch extends ClassMatch {

    /**
     * 创建 Junction
     *
     * 被 {@link PluginFinder#buildMatch()} 调用
     *
     * @return Junction
     */
    ElementMatcher.Junction buildJunction();

    /**
     * 判断是否匹配
     *
     * 被 {@link PluginFinder#find(TypeDescription, ClassLoader)} 调用
     *
     * @param typeDescription 类型描述
     * @return 是否匹配
     */
    boolean isMatch(TypeDescription typeDescription);
}
