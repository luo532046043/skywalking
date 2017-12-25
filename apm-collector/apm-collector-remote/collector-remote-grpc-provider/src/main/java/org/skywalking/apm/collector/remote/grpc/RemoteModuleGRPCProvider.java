package org.skywalking.apm.collector.remote.grpc;

import org.skywalking.apm.collector.cluster.ClusterModule;
import org.skywalking.apm.collector.cluster.service.ModuleListenerService;
import org.skywalking.apm.collector.cluster.service.ModuleRegisterService;
import org.skywalking.apm.collector.core.module.Module;
import org.skywalking.apm.collector.core.module.ModuleProvider;
import org.skywalking.apm.collector.core.module.ServiceNotProvidedException;
import org.skywalking.apm.collector.grpc.manager.GRPCManagerModule;
import org.skywalking.apm.collector.grpc.manager.service.GRPCManagerService;
import org.skywalking.apm.collector.remote.RemoteModule;
import org.skywalking.apm.collector.remote.grpc.handler.RemoteCommonServiceHandler;
import org.skywalking.apm.collector.remote.grpc.service.GRPCRemoteSenderService;
import org.skywalking.apm.collector.remote.service.CommonRemoteDataRegisterService;
import org.skywalking.apm.collector.remote.service.RemoteDataRegisterService;
import org.skywalking.apm.collector.remote.service.RemoteSenderService;
import org.skywalking.apm.collector.server.Server;

import java.util.Properties;

/**
 * 基于 gRPC 的组件服务提供者实现类
 *
 * @author peng-yongsheng
 */
public class RemoteModuleGRPCProvider extends ModuleProvider {

    public static final String NAME = "gRPC";

    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String CHANNEL_SIZE = "channel_size";
    private static final String BUFFER_SIZE = "buffer_size";

    private GRPCRemoteSenderService remoteSenderService;
    private CommonRemoteDataRegisterService remoteDataRegisterService;

    @Override public String name() {
        return NAME;
    }

    @Override public Class<? extends Module> module() {
        return RemoteModule.class;
    }

    @Override public void prepare(Properties config) throws ServiceNotProvidedException {
        String host = config.getProperty(HOST);
        Integer port = (Integer)config.get(PORT);
        Integer channelSize = (Integer)config.getOrDefault(CHANNEL_SIZE, 5);
        Integer bufferSize = (Integer)config.getOrDefault(BUFFER_SIZE, 1000);

        // 创建并注册 CommonRemoteDataRegisterService 、GRPCRemoteSenderService 对象
        remoteDataRegisterService = new CommonRemoteDataRegisterService();
        remoteSenderService = new GRPCRemoteSenderService(host, port, channelSize, bufferSize, remoteDataRegisterService);
        this.registerServiceImplementation(RemoteSenderService.class, remoteSenderService);
        this.registerServiceImplementation(RemoteDataRegisterService.class, remoteDataRegisterService);
    }

    @Override public void start(Properties config) throws ServiceNotProvidedException {
        String host = config.getProperty(HOST);
        Integer port = (Integer)config.get(PORT);

        // 创建 gRPC Server
        GRPCManagerService managerService = getManager().find(GRPCManagerModule.NAME).getService(GRPCManagerService.class);
        Server gRPCServer = managerService.createIfAbsent(host, port);
        // 创建 RemoteCommonServiceHandler 对象，用于接收 gRPC 请求后的处理
        gRPCServer.addHandler(new RemoteCommonServiceHandler(remoteDataRegisterService));

        // 注册 RemoteModuleGRPCRegistration 到集群管理
        ModuleRegisterService moduleRegisterService = getManager().find(ClusterModule.NAME).getService(ModuleRegisterService.class);
        moduleRegisterService.register(RemoteModule.NAME, this.name(), new RemoteModuleGRPCRegistration(host, port));
        // 添加 GRPCRemoteSenderService 到集群管理，
        ModuleListenerService moduleListenerService = getManager().find(ClusterModule.NAME).getService(ModuleListenerService.class);
        moduleListenerService.addListener(remoteSenderService);
    }

    @Override public void notifyAfterCompleted() throws ServiceNotProvidedException {

    }

    @Override public String[] requiredModules() {
        return new String[] {ClusterModule.NAME, GRPCManagerModule.NAME};
    }
}
