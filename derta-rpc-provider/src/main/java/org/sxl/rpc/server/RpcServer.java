package org.sxl.rpc.server;

import com.sxl.common.core.bean.RpcRequest;
import com.sxl.common.core.bean.RpcResponse;
import com.sxl.common.core.coder.RpcDecoder;
import com.sxl.common.core.coder.RpcEncoder;
import com.sxl.common.register.zookeeper.ZooKeeperServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.sxl.rpc.container.LocalHandlerMap;

import java.util.Optional;

/**
 * @Author: shenxl
 * @Date: 2019/9/30 14:32
 * @Version 1.0
 * @description：RPC 服务器（用于发布 RPC 服务）
 */
@Slf4j
public class RpcServer {

    private ZooKeeperServiceRegistry serviceRegistry;

    private Integer port;

    private final LocalHandlerMap localHandlerMap;

    public RpcServer(LocalHandlerMap localHandlerMap, Integer port, ZooKeeperServiceRegistry serviceRegistry) {
        this.serviceRegistry=serviceRegistry;
        this.localHandlerMap= localHandlerMap;
        this.port=port;
    }


    public void action() {

        log.info("另起一个rpc服务线程...");

        Thread rpcServer=new Thread(this::startServer);
        //设置为守护线程
        rpcServer.setDaemon(true);
        //启动服务器
        rpcServer.start();

    }

    private void startServer(){
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 创建并初始化 Netty 服务端 Bootstrap 对象
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 1024)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel channel) {
                    ChannelPipeline pipeline = channel.pipeline();
                    pipeline.addLast(new RpcDecoder(RpcRequest.class)); // 解码 RPC 请求
                    pipeline.addLast(new RpcEncoder(RpcResponse.class)); // 编码 RPC 响应
                    pipeline.addLast(new RpcServerHandler(localHandlerMap)); // 处理 RPC 请求
                }
            });

            //String ip = InetAddress.getLocalHost().getHostAddress();
            String ip = "127.0.0.1";
            String serviceAddress=ip+":"+port;
            // 启动 RPC 服务器
            ChannelFuture future = bootstrap.bind(ip, port).sync();
            // 注册 RPC 服务地址
            Optional.ofNullable(serviceRegistry).ifPresent((t)->
                localHandlerMap.getHandlers().keySet().parallelStream().forEach((s)->{
                t.register(s, serviceAddress);
                log.info("注册：register service: {} => {}", s, serviceAddress);
            }));

            log.info("服务已启动：server started on port {}", port);
            // 关闭 RPC 服务器
            future.channel().closeFuture().sync();

        } catch (Exception e){
            log.error("RPC服务端启动异常，监听{}端口", e);
        }finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }



























   /* public void start() {
        log.info("RPC服务端正在启动...");

        // 接收客户端的链接
        EventLoopGroup bossGroup = new NioEventLoopGroup();

        // 处理已被接收的链接
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 解码
                            ch.pipeline().addLast(new RpcDecoder(RpcRequest.class));

                            // 编码
                            ch.pipeline().addLast(new RpcEncoder(RpcResponse.class));

                            // 收发消息
                            ch.pipeline().addLast(new RpcServerHandler(localHandlerMap));
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);


            // 获取 RPC 服务器的 IP 地址与端口号
            // 启动 RPC 服务器  绑定端口，开始接收进来的链接
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();


            String serviceAddress="127.0.0.1:"+port;
            // 注册 RPC 服务地址
            if (serviceRegistry != null) {
                for (String interfaceName : localHandlerMap.getHandlers().keySet()) {
                    serviceRegistry.register(interfaceName, serviceAddress);
                    log.debug("register service: {} => {}", interfaceName, serviceAddress);
                }
            }
            log.info("RPC服务端启动完成，监听【" + port + "】端口");

            // 等待服务器关闭
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("RPC服务端启动异常，监听{}端口", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }*/

}
