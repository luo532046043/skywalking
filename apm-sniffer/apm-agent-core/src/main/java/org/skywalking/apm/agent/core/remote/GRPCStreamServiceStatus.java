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

package org.skywalking.apm.agent.core.remote;

/**
 * gRPC 数据流服务状态
 *
 * @author wusheng
 */
public class GRPCStreamServiceStatus {

    /**
     * 是否完成
     */
    private volatile boolean status;

    public GRPCStreamServiceStatus(boolean status) {
        this.status = status;
    }

    public boolean isStatus() {
        return status;
    }

    /**
     * 标记完成
     */
    public void finished() {
        this.status = true;
    }

    /**
     * @param maxTimeout max wait time, milliseconds.
     */
    public boolean wait4Finish(long maxTimeout) {
        long time = 0;
        while (!status) {
            if (time > maxTimeout) {
                break;
            }
            // 沉睡 5 毫秒
            try2Sleep(5);
            time += 5;
        }
        return status;
    }

    /**
     * Try to sleep, and ignore the {@link InterruptedException}
     *
     * @param millis the length of time to sleep in milliseconds
     */
    private void try2Sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {

        }
    }
}
