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

package org.skywalking.apm.collector.agent.stream.buffer;

import java.util.Properties;

/**
 * Buffer 文件配置
 *
 * @author peng-yongsheng
 */
public class BufferFileConfig {

    static int BUFFER_OFFSET_MAX_FILE_SIZE = 10 * 1024 * 1024;
    static int BUFFER_SEGMENT_MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * Buffer 文件夹路径默认值
     */
    static String BUFFER_PATH = "../buffer/";

    /**
     * Buffer 文件夹路径配置键
     */
    private static final String BUFFER_PATH_KEY = "buffer_file_path";
    /**
     * Buffer Offset 单文件最大大小
     */
    private static final String BUFFER_OFFSET_MAX_FILE_SIZE_KEY = "buffer_offset_max_file_size";
    /**
     * Buffer Data 单文件最大大小
     */
    private static final String BUFFER_SEGMENT_MAX_FILE_SIZE_KEY = "buffer_segment_max_file_size";

    public static class Parser {

        public void parse(Properties config) {
            // 解析 Buffer 文件夹路径
            if (config.containsKey(BUFFER_PATH_KEY)) {
                BUFFER_PATH = config.getProperty(BUFFER_PATH_KEY);
            }

            // 解析 Buffer Offset 单文件最大大小
            if (config.containsKey(BUFFER_OFFSET_MAX_FILE_SIZE_KEY)) {
                String sizeStr = config.getProperty(BUFFER_OFFSET_MAX_FILE_SIZE_KEY).toUpperCase();
                if (sizeStr.endsWith("K")) {
                    int size = Integer.parseInt(sizeStr.replace("K", ""));
                    BUFFER_OFFSET_MAX_FILE_SIZE = size * 1024;
                } else if (sizeStr.endsWith("KB")) {
                    int size = Integer.parseInt(sizeStr.replace("KB", ""));
                    BUFFER_OFFSET_MAX_FILE_SIZE = size * 1024;
                } else if (sizeStr.endsWith("M")) {
                    int size = Integer.parseInt(sizeStr.replace("M", ""));
                    BUFFER_OFFSET_MAX_FILE_SIZE = size * 1024 * 1024;
                } else if (sizeStr.endsWith("MB")) {
                    int size = Integer.parseInt(sizeStr.replace("MB", ""));
                    BUFFER_OFFSET_MAX_FILE_SIZE = size * 1024 * 1024;
                } else {
                    BUFFER_OFFSET_MAX_FILE_SIZE = 10 * 1024 * 1024;
                }
            } else {
                BUFFER_OFFSET_MAX_FILE_SIZE = 10 * 1024 * 1024;
            }

            // 解析 Buffer Data 单文件最大大小
            if (config.containsKey(BUFFER_SEGMENT_MAX_FILE_SIZE_KEY)) {
                String sizeStr = config.getProperty(BUFFER_SEGMENT_MAX_FILE_SIZE_KEY).toUpperCase();
                if (sizeStr.endsWith("K")) {
                    int size = Integer.parseInt(sizeStr.replace("K", ""));
                    BUFFER_SEGMENT_MAX_FILE_SIZE = size * 1024;
                } else if (sizeStr.endsWith("KB")) {
                    int size = Integer.parseInt(sizeStr.replace("KB", ""));
                    BUFFER_SEGMENT_MAX_FILE_SIZE = size * 1024;
                } else if (sizeStr.endsWith("M")) {
                    int size = Integer.parseInt(sizeStr.replace("M", ""));
                    BUFFER_SEGMENT_MAX_FILE_SIZE = size * 1024 * 1024;
                } else if (sizeStr.endsWith("MB")) {
                    int size = Integer.parseInt(sizeStr.replace("MB", ""));
                    BUFFER_SEGMENT_MAX_FILE_SIZE = size * 1024 * 1024;
                } else {
                    BUFFER_SEGMENT_MAX_FILE_SIZE = 1024 * 1024;
                }
            } else {
                BUFFER_SEGMENT_MAX_FILE_SIZE = 1024 * 1024;
            }
        }
    }
}
