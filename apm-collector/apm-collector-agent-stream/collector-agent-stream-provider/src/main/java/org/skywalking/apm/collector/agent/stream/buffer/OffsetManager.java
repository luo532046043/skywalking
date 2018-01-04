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

import org.skywalking.apm.collector.agent.stream.util.FileUtils;
import org.skywalking.apm.collector.core.util.CollectionUtils;
import org.skywalking.apm.collector.core.util.Const;
import org.skywalking.apm.collector.core.util.TimeBucketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Offset 管理
 *
 * @author peng-yongsheng
 */
public enum OffsetManager {

    /**
     * 单例
     */
    INSTANCE;

    private final Logger logger = LoggerFactory.getLogger(OffsetManager.class);

    /**
     * Offset 文件的文件名前缀
     */
    private static final String OFFSET_FILE_PREFIX = "offset";

    /**
     * Offset 文件
     */
    private File offsetFile;
    /**
     * Offset 对象
     */
    private Offset offset;
    /**
     * 是否初始化
     */
    private boolean initialized = false;
    private RandomAccessFile randomAccessFile = null;
    /**
     * 最后写入的 Offset 序列化字符串
     */
    private String lastOffsetRecord = Const.EMPTY_STRING;

    public synchronized void initialize() throws IOException {
        if (!initialized) {
            this.offset = new Offset();
            File dataPath = new File(BufferFileConfig.BUFFER_PATH);
            // 创建成功，意味着不存在 Buffer 文件夹
            if (dataPath.mkdirs()) {
                createOffsetFile(); // 创建 Offset 文件
            // 创建失败，意味着存在 Offset 文件件
            } else {
                // 获得所有 Offset 文件
                File[] offsetFiles = dataPath.listFiles(new PrefixFileNameFilter());
                // 删除老的 Offset 文件，保留最后一个
                if (CollectionUtils.isNotEmpty(offsetFiles) && offsetFiles.length > 0) {
                    for (int i = 0; i < offsetFiles.length; i++) {
                        if (i != offsetFiles.length - 1) {
                            offsetFiles[i].delete(); // 删除
                        } else {
                            offsetFile = offsetFiles[i];
                        }
                    }
                } else {
                    createOffsetFile(); // 一个都不存在，进行创建
                }
            }

            // 从 Offset 文件的最后一行，读取 Offset
            String offsetRecord = FileUtils.INSTANCE.readLastLine(offsetFile);
            offset.deserialize(offsetRecord);
            initialized = true;

            // 创建定时任务，定时写入 Offset 到 Offset 文件
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::flush, 10, 3, TimeUnit.SECONDS);
        }
    }

    /**
     * 创建 Offset 文件
     *
     * @throws IOException
     */
    private void createOffsetFile() throws IOException {
        // 创建 Offset 文件
        String timeBucket = String.valueOf(TimeBucketUtils.INSTANCE.getSecondTimeBucket(System.currentTimeMillis()));
        String offsetFileName = OFFSET_FILE_PREFIX + "_" + timeBucket + "." + Const.FILE_SUFFIX;
        offsetFile = new File(BufferFileConfig.BUFFER_PATH + offsetFileName);
        // 设置 Offset
        this.offset.getWriteOffset().setWriteFileName(Const.EMPTY_STRING);
        this.offset.getWriteOffset().setWriteFileOffset(0);
        this.offset.getReadOffset().setReadFileName(Const.EMPTY_STRING);
        this.offset.getReadOffset().setReadFileOffset(0);
        // 写入 Offset 到 Offset 文件
        this.flush();
    }

    /**
     * 写入 Offset 到 Offset 文件
     */
    public void flush() {
        // 序列化 Offset 成字符串
        String offsetRecord = offset.serialize();
        // 写入 Offset 到 Offset 文件
        if (!lastOffsetRecord.equals(offsetRecord)) {
            // 超过单文件上限，创建新文件，并删除老文件
            if (offsetFile.length() >= BufferFileConfig.BUFFER_OFFSET_MAX_FILE_SIZE) {
                nextFile();
            }
            // 写入 Offset 序列化的字符串 到 Offset 文件
            FileUtils.INSTANCE.writeAppendToLast(offsetFile, randomAccessFile, offsetRecord);
            // 记录 最后写入的 Offset 序列化字符串
            lastOffsetRecord = offsetRecord;
        }
    }

    /**
     * 创建新文件，并删除老文件
     */
    private void nextFile() {
        // 创建新的 Offset 文件
        String timeBucket = String.valueOf(TimeBucketUtils.INSTANCE.getSecondTimeBucket(System.currentTimeMillis()));
        String offsetFileName = OFFSET_FILE_PREFIX + "_" + timeBucket + "." + Const.FILE_SUFFIX;
        File newOffsetFile = new File(BufferFileConfig.BUFFER_PATH + offsetFileName);
        // 删除老的 Offset 文件
        offsetFile.delete();
        // 修改指向新的 Offset 文件
        offsetFile = newOffsetFile;
        // 写入 Offset 到新的 Offset 文件
        this.flush();
    }

    public String getReadFileName() {
        return offset.getReadOffset().getReadFileName();
    }

    public long getReadFileOffset() {
        return offset.getReadOffset().getReadFileOffset();
    }

    public void setReadOffset(long readFileOffset) {
        offset.getReadOffset().setReadFileOffset(readFileOffset);
    }

    public void setReadOffset(String readFileName, long readFileOffset) {
        offset.getReadOffset().setReadFileName(readFileName);
        offset.getReadOffset().setReadFileOffset(readFileOffset);
    }

    public String getWriteFileName() {
        return offset.getWriteOffset().getWriteFileName();
    }

    public long getWriteFileOffset() {
        return offset.getWriteOffset().getWriteFileOffset();
    }

    public void setWriteOffset(String writeFileName, long writeFileOffset) {
        offset.getWriteOffset().setWriteFileName(writeFileName);
        offset.getWriteOffset().setWriteFileOffset(writeFileOffset);
    }

    public void setWriteOffset(long writeFileOffset) {
        offset.getWriteOffset().setWriteFileOffset(writeFileOffset);
    }

    class PrefixFileNameFilter implements FilenameFilter {
        @Override public boolean accept(File dir, String name) {
            return name.startsWith(OFFSET_FILE_PREFIX);
        }
    }
}
