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

import org.skywalking.apm.collector.core.module.ModuleManager;
import org.skywalking.apm.collector.core.util.Const;
import org.skywalking.apm.collector.core.util.StringUtils;
import org.skywalking.apm.collector.core.util.TimeBucketUtils;
import org.skywalking.apm.network.proto.UpstreamSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * TraceSegment Buffer 管理器
 *
 * @author peng-yongsheng
 */
public enum SegmentBufferManager {

    /**
     * 单例
     */
    INSTANCE;

    private final Logger logger = LoggerFactory.getLogger(SegmentBufferManager.class);

    /**
     * Data 文件的文件名前缀
     */
    public static final String DATA_FILE_PREFIX = "data";

    private FileOutputStream outputStream;

    public synchronized void initialize(ModuleManager moduleManager) {
        logger.info("segment buffer initialize");
        try {
            // 初始化 OffsetManager
            OffsetManager.INSTANCE.initialize();

            // 创建成功，意味着不存在 Buffer 文件夹
            if (new File(BufferFileConfig.BUFFER_PATH).mkdirs()) {
                // 创建 Data 文件
                newDataFile();
            } else {
                // 获得 Offset 正在写入的
                String writeFileName = OffsetManager.INSTANCE.getWriteFileName();
                if (StringUtils.isNotEmpty(writeFileName)) {
                    File dataFile = new File(BufferFileConfig.BUFFER_PATH + writeFileName);
                    if (dataFile.exists()) {
                        outputStream = new FileOutputStream(new File(BufferFileConfig.BUFFER_PATH + writeFileName), true);
                    } else {
                        newDataFile();
                    }
                } else {
                    newDataFile();
                }
            }

            // 初始化 SegmentBufferReader
            SegmentBufferReader.INSTANCE.initialize(moduleManager);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 将 TraceSegment 写入 Data 文件( 包括 flush )
     *
     * @param segment TraceSegment
     */
    public synchronized void writeBuffer(UpstreamSegment segment) {
        try {
            // 将 TraceSegment 写入 Data 文件( 包括 flush )
            segment.writeDelimitedTo(outputStream);
            long position = outputStream.getChannel().position();
            // 超过单文件上限，创建新文件
            if (position > BufferFileConfig.BUFFER_SEGMENT_MAX_FILE_SIZE) {
                newDataFile();
            } else {
            // 未超过文件上限，设置 Offset 写入的偏移
                OffsetManager.INSTANCE.setWriteOffset(position);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 创建新的 Data 文件
     *
     * @throws IOException 当 IO 发生异常时
     */
    private void newDataFile() throws IOException {
        logger.debug("create new segment buffer file");
        // 创建新的 Data 文件
        String timeBucket = String.valueOf(TimeBucketUtils.INSTANCE.getSecondTimeBucket(System.currentTimeMillis()));
        String writeFileName = DATA_FILE_PREFIX + "_" + timeBucket + "." + Const.FILE_SUFFIX;
        File dataFile = new File(BufferFileConfig.BUFFER_PATH + writeFileName);
        dataFile.createNewFile();
        // 设置 Offset 写入的文件名和偏移
        OffsetManager.INSTANCE.setWriteOffset(writeFileName, 0);
        try {
            // 关闭老的 Data 文件的 outputStream
            if (outputStream != null) {
                outputStream.close();
            }

            // 创建新的 Data 文件的 outputStream
            outputStream = new FileOutputStream(dataFile);
            outputStream.getChannel().position(0);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * flush 方法为空，因为 {@link #writeBuffer(UpstreamSegment)} 已经 flush
     */
    public synchronized void flush() {
    }
}
