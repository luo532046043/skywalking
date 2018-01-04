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

import com.google.protobuf.CodedOutputStream;
import org.skywalking.apm.collector.agent.stream.parser.SegmentParse;
import org.skywalking.apm.collector.core.module.ModuleManager;
import org.skywalking.apm.collector.core.util.CollectionUtils;
import org.skywalking.apm.collector.core.util.Const;
import org.skywalking.apm.collector.core.util.StringUtils;
import org.skywalking.apm.network.proto.UpstreamSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * TraceSegment Buffer 读取器
 *
 * @author peng-yongsheng
 */
public enum SegmentBufferReader {

    /**
     * 单例
     */
    INSTANCE;

    private final Logger logger = LoggerFactory.getLogger(SegmentBufferReader.class);

    private InputStream inputStream;
    private ModuleManager moduleManager;

    public void initialize(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;

        // 创建定时任务，读取 Data 文件，提交存储
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::preRead, 3, 3, TimeUnit.SECONDS);
    }

    private void preRead() {
        String readFileName = OffsetManager.INSTANCE.getReadFileName();
        if (StringUtils.isNotEmpty(readFileName)) {
            File readFile = new File(BufferFileConfig.BUFFER_PATH + readFileName);
            if (readFile.exists()) {
                // 删除比指定文件早创建的 Data 文件
                deleteTheDataFilesBeforeReadFile(readFileName);
                // 读取 Data 文件，提交存储
                long readFileOffset = OffsetManager.INSTANCE.getReadFileOffset();
                read(readFile, readFileOffset);
                // 循环顺序读取 Data 文件，直到有一个没读完
                readEarliestCreateDataFile();
            } else {
                // 删除比指定文件早创建的 Data 文件
                deleteTheDataFilesBeforeReadFile(readFileName);
                // 循环顺序读取 Data 文件，直到有一个没读完
                readEarliestCreateDataFile();
            }
        } else {
            // 循环顺序读取 Data 文件，直到有一个没读完
            readEarliestCreateDataFile();
        }
    }

    /**
     * 删除比指定文件早创建的 Data 文件
     *
     * @param readFileName 文件名字
     */
    private void deleteTheDataFilesBeforeReadFile(String readFileName) {
        File[] dataFiles = new File(BufferFileConfig.BUFFER_PATH).listFiles(new PrefixFileNameFilter());
        long readFileCreateTime = getFileCreateTime(readFileName);
        for (File dataFile : dataFiles) {
            long fileCreateTime = getFileCreateTime(dataFile.getName());
            if (fileCreateTime < readFileCreateTime) {
                dataFile.delete();
            } else if (fileCreateTime == readFileCreateTime) {
                break;
            }
        }
    }

    private long getFileCreateTime(String fileName) {
        fileName = fileName.replace(SegmentBufferManager.DATA_FILE_PREFIX + "_", Const.EMPTY_STRING);
        fileName = fileName.replace("." + Const.FILE_SUFFIX, Const.EMPTY_STRING);
        return Long.valueOf(fileName);
    }

    /**
     * 循环顺序读取 Data 文件，直到有一个没读完
     */
    private void readEarliestCreateDataFile() {
        // 若第一个 Data 文件和 Offset 读取的文件相同，返回。说明，在上一次 `#read()` 方法里，没有读完。
        String readFileName = OffsetManager.INSTANCE.getReadFileName();
        File[] dataFiles = new File(BufferFileConfig.BUFFER_PATH).listFiles(new PrefixFileNameFilter());
        if (CollectionUtils.isNotEmpty(dataFiles)) {
            if (dataFiles[0].getName().equals(readFileName)) {
                return;
            }
        }

        // 循环顺序读取 Data 文件，直到有一个没读完
        for (File dataFile : dataFiles) {
            logger.debug("Reading segment buffer data file, file name: {}", dataFile.getAbsolutePath());
            OffsetManager.INSTANCE.setReadOffset(dataFile.getName(), 0);
            if (!read(dataFile, 0)) { // 没读完，结束
                break;
            }
        }
    }

    private boolean read(File readFile, long readFileOffset) {
        try {
            // 创建 FileInputStream 对象，并跳转到读取位置
            inputStream = new FileInputStream(readFile);
            inputStream.skip(readFileOffset);

            // 获取读取结束的位置
            String writeFileName = OffsetManager.INSTANCE.getWriteFileName();
            long endPoint = readFile.length();
            if (writeFileName.equals(readFile.getName())) {
                endPoint = OffsetManager.INSTANCE.getWriteFileOffset();
            }

            // 循环处理，直到到达读取文件上限位置
            while (readFile.length() > readFileOffset && readFileOffset < endPoint) {
                // 读取一条 TraceSegment
                UpstreamSegment upstreamSegment = UpstreamSegment.parser().parseDelimitedFrom(inputStream);

                // 提交 SegmentParse 解析，提交存储。若处理失败，结束循环，等待下次读取
                SegmentParse parse = new SegmentParse(moduleManager);
                if (!parse.parse(upstreamSegment, SegmentParse.Source.Buffer)) {
                    return false;
                }

                // 设置读取偏移到 Offset
                final int serialized = upstreamSegment.getSerializedSize();
                readFileOffset = readFileOffset + CodedOutputStream.computeUInt32SizeNoTag(serialized) + serialized;
                logger.debug("read segment buffer from file: {}, offset: {}, file length: {}", readFile.getName(), readFileOffset, readFile.length());
                OffsetManager.INSTANCE.setReadOffset(readFileOffset);
            }

            // 全部读取完成，关闭 inputStream ，同时删除读取的 Data 文件
            inputStream.close();
            if (!writeFileName.equals(readFile.getName())) {
                readFile.delete();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    class PrefixFileNameFilter implements FilenameFilter {
        @Override public boolean accept(File dir, String name) {
            return name.startsWith(SegmentBufferManager.DATA_FILE_PREFIX);
        }
    }
}
