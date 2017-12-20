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

package org.skywalking.apm.collector.stream.worker.base;

import org.skywalking.apm.collector.core.data.Data;
import org.skywalking.apm.collector.remote.service.RemoteSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 远程 Worker 引用
 *
 * @author peng-yongsheng
 */
public class RemoteWorkerRef<INPUT extends Data, OUTPUT extends Data> extends WorkerRef<INPUT, OUTPUT> {

    private final Logger logger = LoggerFactory.getLogger(RemoteWorkerRef.class);

    /**
     * 远程 Worker
     */
    private final AbstractRemoteWorker<INPUT, OUTPUT> remoteWorker;
    /**
     * 远程发送服务
     */
    private final RemoteSenderService remoteSenderService;
    private final int graphId;

    RemoteWorkerRef(AbstractRemoteWorker<INPUT, OUTPUT> remoteWorker, RemoteSenderService remoteSenderService,
        int graphId) {
        super(remoteWorker);
        this.remoteWorker = remoteWorker;
        this.remoteSenderService = remoteSenderService;
        this.graphId = graphId;
    }

    @Override protected void in(INPUT message) {
        try {
            // 发送数据给远程 Worker
            RemoteSenderService.Mode mode = remoteSenderService.send(this.graphId, this.remoteWorker.id(), message, this.remoteWorker.selector());
            // 如果发射结果是本地，交给本地的 Worker
            if (mode.equals(RemoteSenderService.Mode.Local)) {
                out(message);
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override protected void out(INPUT input) {
        super.out(input);
    }

}
