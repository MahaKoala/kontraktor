package org.nustaq.kontraktor.remoting.http.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultChannelPromise;
import io.netty.handler.codec.http.*;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.http.NioHttpServer;
import org.nustaq.kontraktor.remoting.http.NioHttpServerImpl;
import org.nustaq.kontraktor.remoting.http.RequestProcessor;
import org.nustaq.kontraktor.remoting.http.RequestResponse;
import org.nustaq.kontraktor.remoting.http.rest.RestActorServer;
import org.nustaq.netty2go.NettyWSHttpServer;
import org.nustaq.webserver.ClientSession;
import org.nustaq.webserver.WebSocketHttpServer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Created by ruedi on 18.08.14.
 */
public class KontraktorNettyServer extends WebSocketHttpServer implements NioHttpServer {

    RequestProcessor processor;

    public KontraktorNettyServer() {
        super(new File("."));
    }

    @Override
    public void onHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req, NettyWSHttpServer.HttpResponseSender sender) {
        if ( req.getMethod() == HttpMethod.GET || req.getMethod() == HttpMethod.POST ) {
            NettyKontraktorHttpRequest kreq = new NettyKontraktorHttpRequest(req);
            processor.processRequest(kreq, (result,error) -> {
                if ( result == RequestResponse.MSG_200 ) {
                    ctx.write(new DefaultHttpResponse(HTTP_1_0, HttpResponseStatus.OK));
                    return;
                }
                if ( result == RequestResponse.MSG_404 ) {
                    ctx.write(new DefaultHttpResponse(HTTP_1_0, HttpResponseStatus.NOT_FOUND));
                    return;
                }
                if ( result == RequestResponse.MSG_500 ) {
                    ctx.write(new DefaultHttpResponse(HTTP_1_0, HttpResponseStatus.INTERNAL_SERVER_ERROR));
                    return;
                }
                if (error == null || error == RequestProcessor.FINISHED) {
                    try {
                        if (result != null) {
                            ctx.write(Unpooled.copiedBuffer(result.toString(), Charset.forName("UTF-8") ) );
//                                    writeClient(client, ByteBuffer.wrap(result.toString().getBytes()));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (error != null) {
                    try {
                        if (error != RequestProcessor.FINISHED) {
                            ctx.write(Unpooled.copiedBuffer(error.toString(), Charset.forName("UTF-8")) );
//                                    writeClient(client, ByteBuffer.wrap(error.toString().getBytes()));
                        }
                        ChannelFuture f = ctx.writeAndFlush(Unpooled.copiedBuffer("", Charset.forName("UTF-8") ));
                        f.addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) {
//                                future.cause().printStackTrace();
//                                System.out.println(future);
                                future.channel().close();
                            }
                        });
//                                key.cancel();
//                                client.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            sender.sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_0, FORBIDDEN));
        }
    }

    @Override
    protected ClientSession createNewSession() {
        return null;
    }

    NettyWSHttpServer nettyWSHttpServer;

    @Override
    public void $init(int port, RequestProcessor restProcessor) {
        nettyWSHttpServer = new NettyWSHttpServer(port, this);
        processor = restProcessor;
    }

    @Override
    public void $receive() {
        new Thread( () -> {
            try {
                nettyWSHttpServer.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

    @Override
    public Actor getServingActor() {
        return null;
    }

    public static void main(String[] args) throws Exception {


        RestActorServer sv = new RestActorServer().map(RestActorServer.MDesc.class);
        sv.publish("rest",Actors.AsActor(RestActorServer.RESTActor.class,65000));
        KontraktorNettyServer kserver = new KontraktorNettyServer();
        sv.startServer(9999,kserver);

    }

}