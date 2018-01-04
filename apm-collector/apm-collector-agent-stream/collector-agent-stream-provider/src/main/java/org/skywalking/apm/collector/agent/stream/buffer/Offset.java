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

/**
 * 偏移
 *
 * @author peng-yongsheng
 */
public class Offset {

    private static final String SPLIT_CHARACTER = ",";

    /**
     * 读取偏移
     */
    private ReadOffset readOffset;
    /**
     * 写入偏移
     */
    private WriteOffset writeOffset;

    public Offset() {
        readOffset = new ReadOffset();
        writeOffset = new WriteOffset();
    }

    /**
     * 序列化读写偏移( readOffset , writeOffset )成字符串，格式为 ${读取文件名},${读取文件偏移量},${写入文件名},${写入文件偏移量}
     *
     * @return 字符串
     */
    public String serialize() {
        return readOffset.getReadFileName() + SPLIT_CHARACTER + String.valueOf(readOffset.getReadFileOffset())
            + SPLIT_CHARACTER + writeOffset.getWriteFileName() + SPLIT_CHARACTER + String.valueOf(writeOffset.getWriteFileOffset());
    }

    /**
     * 反序列化字符串，设置读写偏移( readOffset , writeOffset )
     *
     * @param value 字符串
     */
    public void deserialize(String value) {
        String[] values = value.split(SPLIT_CHARACTER);
        if (values.length == 4) {
            this.readOffset.readFileName = values[0];
            this.readOffset.readFileOffset = Long.parseLong(values[1]);
            this.writeOffset.writeFileName = values[2];
            this.writeOffset.writeFileOffset = Long.parseLong(values[3]);
        }
    }

    public ReadOffset getReadOffset() {
        return readOffset;
    }

    public WriteOffset getWriteOffset() {
        return writeOffset;
    }

    /**
     * 读取偏移类
     */
    public static class ReadOffset {

        /**
         * 读取文件名
         */
        private String readFileName;
        /**
         * 读取文件偏移量
         */
        private long readFileOffset = 0;

        public String getReadFileName() {
            return readFileName;
        }

        public long getReadFileOffset() {
            return readFileOffset;
        }

        public void setReadFileName(String readFileName) {
            this.readFileName = readFileName;
        }

        public void setReadFileOffset(long readFileOffset) {
            this.readFileOffset = readFileOffset;
        }
    }

    /**
     * 写入偏移类
     */
    public static class WriteOffset {

        /**
         * 写入文件名
         */
        private String writeFileName;
        /**
         * 写入文件偏移量
         */
        private long writeFileOffset = 0;

        public String getWriteFileName() {
            return writeFileName;
        }

        public long getWriteFileOffset() {
            return writeFileOffset;
        }

        public void setWriteFileName(String writeFileName) {
            this.writeFileName = writeFileName;
        }

        public void setWriteFileOffset(long writeFileOffset) {
            this.writeFileOffset = writeFileOffset;
        }
    }

}
