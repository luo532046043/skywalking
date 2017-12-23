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

package org.skywalking.apm.collector.core.cache;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 窗口抽象类
 *
 * @author peng-yongsheng
 */
public abstract class Window<WINDOW_COLLECTION extends Collection> {

    /**
     * 窗口切换计数
     */
    private AtomicInteger windowSwitch = new AtomicInteger(0);

    /**
     * 数据指向
     * {@link #windowDataA} or {@link #windowDataB}
     */
    private WINDOW_COLLECTION pointer;

    /**
     * 窗口数据A
     */
    private WINDOW_COLLECTION windowDataA;
    /**
     * 窗口数据B
     */
    private WINDOW_COLLECTION windowDataB;

    protected Window() {
        this.windowDataA = collectionInstance();
        this.windowDataB = collectionInstance();
        this.pointer = windowDataA;
    }

    /**
     * 创建窗口数据对象
     *
     * @return 窗口数据
     */
    public abstract WINDOW_COLLECTION collectionInstance();

    /**
     * 尝试切换数据指向
     *
     * @return 转换是否成功
     */
    public boolean trySwitchPointer() {
        return windowSwitch.incrementAndGet() == 1 && !getLast().isReading();
    }

    /**
     * 释放切换计数
     */
    public void trySwitchPointerFinally() {
        windowSwitch.addAndGet(-1);
    }

    /**
     * 切换数据指向
     */
    public void switchPointer() {
        // 切换数据指向
        if (pointer == windowDataA) {
            pointer = windowDataB;
        } else {
            pointer = windowDataA;
        }
        // 标记原数据指向正在读取中
        getLast().reading();
    }

    /**
     * 获得现数据指向，并标记正在写入中
     *
     * @return 现数据指向
     */
    protected WINDOW_COLLECTION getCurrentAndWriting() {
        if (pointer == windowDataA) {
            windowDataA.writing();
            return windowDataA;
        } else {
            windowDataB.writing();
            return windowDataB;
        }
    }

    /**
     * @return 现数据指向
     */
    protected WINDOW_COLLECTION getCurrent() {
        return pointer;
    }

    /**
     * @return 原数据指向
     */
    public WINDOW_COLLECTION getLast() {
        if (pointer == windowDataA) {
            return windowDataB;
        } else {
            return windowDataA;
        }
    }

    /**
     * 清空原数据指向，并标记原数据指向完成正在读取中
     */
    public void finishReadingLast() {
        // 清空原数据指向
        getLast().clear();
        // 标记原数据指向完成正在读取中
        getLast().finishReading();
    }

}
