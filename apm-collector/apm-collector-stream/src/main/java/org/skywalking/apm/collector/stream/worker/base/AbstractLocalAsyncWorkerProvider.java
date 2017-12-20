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

import org.skywalking.apm.collector.core.module.ModuleManager;
import org.skywalking.apm.collector.queue.base.QueueEventHandler;
import org.skywalking.apm.collector.queue.service.QueueCreatorService;

/**
 * LocalAsyncWorker 供应者抽象类
 *
 * @author peng-yongsheng
 */
public abstract class AbstractLocalAsyncWorkerProvider<INPUT, OUTPUT, WORKER_TYPE extends AbstractLocalAsyncWorker<INPUT, OUTPUT>> extends AbstractWorkerProvider<INPUT, OUTPUT, WORKER_TYPE> {

    /**
     * @return 队列大小
     */
    public abstract int queueSize();

    /**
     * 队列创建服务
     */
    private final QueueCreatorService<INPUT> queueCreatorService;

    public AbstractLocalAsyncWorkerProvider(ModuleManager moduleManager,
        QueueCreatorService<INPUT> queueCreatorService) {
        super(moduleManager);
        this.queueCreatorService = queueCreatorService;
    }

    @Override
    public final WorkerRef create(WorkerCreateListener workerCreateListener) {
        // 创建 AbstractLocalAsyncWorker 对象
        WORKER_TYPE localAsyncWorker = workerInstance(getModuleManager());

        // 添加 AbstractLocalAsyncWorker 到 作业创建监听器
        workerCreateListener.addWorker(localAsyncWorker);

        // 创建 LocalAsyncWorkerRef
        LocalAsyncWorkerRef<INPUT, OUTPUT> localAsyncWorkerRef = new LocalAsyncWorkerRef<>(localAsyncWorker);

        // 创建 QueueEventHandler ，并设置 LocalAsyncWorkerRef 为其执行器
        QueueEventHandler<INPUT> queueEventHandler = queueCreatorService.create(queueSize(), localAsyncWorkerRef);

        // 设置 LocalAsyncWorkerRef 的 QueueEventHandler
        localAsyncWorkerRef.setQueueEventHandler(queueEventHandler);
        return localAsyncWorkerRef;
    }
}
