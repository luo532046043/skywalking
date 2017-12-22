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

package org.skywalking.apm.collector.core.define;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * 定义加载器
 *
 * @author peng-yongsheng
 */
public class DefinitionLoader<D> implements Iterable<D> {

    private final Logger logger = LoggerFactory.getLogger(DefinitionLoader.class);

    private final Class<D> definition;
    /**
     * 定义文件
     */
    private final DefinitionFile definitionFile;

    protected DefinitionLoader(Class<D> svc, DefinitionFile definitionFile) {
        this.definition = Objects.requireNonNull(svc, "definition interface cannot be null");
        this.definitionFile = definitionFile;
    }

    public static <D> DefinitionLoader<D> load(Class<D> definition, DefinitionFile definitionFile) {
        return new DefinitionLoader(definition, definitionFile);
    }

    @Override public final Iterator<D> iterator() {
        logger.info("load definition file: {}", definitionFile.get());
        List<String> definitionList = new LinkedList<>();
        try {
            Enumeration<URL> urlEnumeration = this.getClass().getClassLoader().getResources(definitionFile.get());
            // 循环查找到的定义文件数组。目前 collector-storage-es-provider 和 collector-storage-h2-provider 各有一个定义文件
            while (urlEnumeration.hasMoreElements()) {
                // 读取 文件
                URL definitionFileURL = urlEnumeration.nextElement();
                logger.info("definition file url: {}", definitionFileURL.getPath());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(definitionFileURL.openStream()));
                Properties properties = new Properties();
                properties.load(bufferedReader);

                // 读取 文件里的每个类
                Enumeration defineItem = properties.propertyNames();
                while (defineItem.hasMoreElements()) {
                    String fullNameClass = (String)defineItem.nextElement();
                    definitionList.add(fullNameClass);
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        // 创建 Iterator
        Iterator<String> moduleDefineIterator = definitionList.iterator();
        return new Iterator<D>() {
            @Override public boolean hasNext() {
                return moduleDefineIterator.hasNext();
            }

            @Override public D next() {
                String definitionClass = moduleDefineIterator.next();
                logger.info("definitionClass: {}", definitionClass);
                // 创建 定义对象
                try {
                    Class c = Class.forName(definitionClass);
                    return (D)c.newInstance();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                return null;
            }
        };
    }
}
