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

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.skywalking.apm.agent.core.boot.BootService;
import org.skywalking.apm.agent.core.boot.ServiceManager;
import org.skywalking.apm.agent.core.context.TracingContext;
import org.skywalking.apm.agent.core.context.TracingContextListener;
import org.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.skywalking.apm.agent.core.logging.api.ILog;
import org.skywalking.apm.agent.core.logging.api.LogManager;
import org.skywalking.apm.commons.datacarrier.DataCarrier;
import org.skywalking.apm.commons.datacarrier.buffer.BufferStrategy;
import org.skywalking.apm.commons.datacarrier.consumer.IConsumer;
import org.skywalking.apm.network.proto.Downstream;
import org.skywalking.apm.network.proto.TraceSegmentServiceGrpc;
import org.skywalking.apm.network.proto.UpstreamSegment;

import java.util.List;

import static org.skywalking.apm.agent.core.conf.Config.Buffer.BUFFER_SIZE;
import static org.skywalking.apm.agent.core.conf.Config.Buffer.CHANNEL_SIZE;
import static org.skywalking.apm.agent.core.remote.GRPCChannelStatus.CONNECTED;

/**
 * TraceSegment 发送服务客户端
 *
 * @author wusheng
 */
public class TraceSegmentServiceClient implements BootService, IConsumer<TraceSegment>, TracingContextListener, GRPCChannelListener {

    private static final ILog logger = LogManager.getLogger(TraceSegmentServiceClient.class);

    /**
     * 发送超时时间，单位：毫秒
     */
    private static final int TIMEOUT = 30 * 1000;

    /**
     * 最后打印日志时间
     *
     * {@link #printUplinkStatus()}
     */
    private long lastLogTime;
    /**
     * TraceSegment 发送数量
     */
    private long segmentUplinkedCounter;
    /**
     * TraceSegment 被遗弃数量
     */
    private long segmentAbandonedCounter;
    /**
     * 内存队列
     */
    private volatile DataCarrier<TraceSegment> carrier;
    /**
     * Stub
     */
    private volatile TraceSegmentServiceGrpc.TraceSegmentServiceStub serviceStub;
    /**
     * 连接状态
     */
    private volatile GRPCChannelStatus status = GRPCChannelStatus.DISCONNECT;

    @Override
    public void beforeBoot() throws Throwable {
        ServiceManager.INSTANCE.findService(GRPCChannelManager.class).addChannelListener(this);
    }

    @Override
    public void boot() throws Throwable {
        lastLogTime = System.currentTimeMillis();
        segmentUplinkedCounter = 0;
        segmentAbandonedCounter = 0;
        // 创建 DataCarrier 对象，作为内存队列
        carrier = new DataCarrier<TraceSegment>(CHANNEL_SIZE, BUFFER_SIZE);
        carrier.setBufferStrategy(BufferStrategy.IF_POSSIBLE);
        carrier.consume(this, 1); // 消费者
    }

    @Override
    public void afterBoot() throws Throwable {
        TracingContext.ListenerManager.add(this);
    }

    @Override
    public void shutdown() throws Throwable {
        carrier.shutdownConsumers();
    }

    @Override
    public void init() {

    }

    @Override
    public void consume(List<TraceSegment> data) {
        if (CONNECTED.equals(status)) {
            // 创建 GRPCStreamServiceStatus 对象
            final GRPCStreamServiceStatus status = new GRPCStreamServiceStatus(false);

            // 创建 StreamObserver 对象
            StreamObserver<UpstreamSegment> upstreamSegmentStreamObserver = serviceStub.collect(new StreamObserver<Downstream>() {
                @Override
                public void onNext(Downstream downstream) {

                }

                @Override
                public void onError(Throwable throwable) {
                    status.finished();
                    if (logger.isErrorEnable()) {
                        logger.error(throwable, "Send UpstreamSegment to collector fail with a grpc internal exception.");
                    }
                    ServiceManager.INSTANCE.findService(GRPCChannelManager.class).reportError(throwable);
                }

                @Override
                public void onCompleted() {
                    status.finished(); // 标记处理完成
                }
            });

            // 逐条发送 TraceSegment 请求
            for (TraceSegment segment : data) {
                try {
                    UpstreamSegment upstreamSegment = segment.transform();
                    upstreamSegmentStreamObserver.onNext(upstreamSegment);
                } catch (Throwable t) {
                    logger.error(t, "Transform and send UpstreamSegment to collector fail.");
                }
            }

            // 全部请求发送完成
            upstreamSegmentStreamObserver.onCompleted();

            // 等待处理完成
            if (status.wait4Finish(TIMEOUT)) {
                segmentUplinkedCounter += data.size();
            }
        } else {
            segmentAbandonedCounter += data.size();
        }

        printUplinkStatus();
    }

    /**
     * 每三十秒，打印一次 segmentUplinkedCounter 和 segmentAbandonedCounter 数据。主要用于开发调试。
     */
    private void printUplinkStatus() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - lastLogTime > 30 * 1000) {
            lastLogTime = currentTimeMillis;
            if (segmentUplinkedCounter > 0) {
                logger.debug("{} trace segments have been sent to collector.", segmentUplinkedCounter);
                segmentUplinkedCounter = 0;
            }
            if (segmentAbandonedCounter > 0) {
                logger.debug("{} trace segments have been abandoned, cause by no available channel.", segmentAbandonedCounter);
                segmentAbandonedCounter = 0;
            }
        }
    }

    @Override
    public void onError(List<TraceSegment> data, Throwable t) {
        logger.error(t, "Try to send {} trace segments to collector, with unexpected exception.", data.size());
    }

    @Override
    public void onExit() {

    }

    @Override
    public void afterFinished(TraceSegment traceSegment) {
        if (traceSegment.isIgnore()) {
            return;
        }
        // 提交 TraceSegment 到内存队列
        if (!carrier.produce(traceSegment)) {
            if (logger.isDebugEnable()) {
                logger.debug("One trace segment has been abandoned, cause by buffer is full.");
            }
        }
    }

    @Override
    public void statusChanged(GRPCChannelStatus status) {
        // 连接成功，创建 Stub
        if (CONNECTED.equals(status)) {
            ManagedChannel channel = ServiceManager.INSTANCE.findService(GRPCChannelManager.class).getManagedChannel();
            serviceStub = TraceSegmentServiceGrpc.newStub(channel);
        }
        this.status = status;
    }

}
