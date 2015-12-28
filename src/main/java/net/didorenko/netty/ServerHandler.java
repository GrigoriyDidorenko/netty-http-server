
package net.didorenko.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import net.didorenko.netty.data.Data;
import net.didorenko.netty.data.StatusData;

import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * package: net.didorenko.netty
 * project: netty-http-server
 * class:
 *
 * @author: Grigoriy Didorenko
 * @date: 27.12.2015
 */

public class ServerHandler extends SimpleChannelInboundHandler<Object> {

    private HttpRequest request;
    private StatusData statusData = new StatusData();


    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        //TODO
        statusData.setSpeed(System.currentTimeMillis());
        Data.getInstance().addQueryCounter();
        Data.getInstance().addActiveCounter();
        Data.getInstance().addIpCounter(ctx.channel().remoteAddress().toString());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {

        if (msg instanceof HttpRequest) {
            HttpRequest request = this.request = (HttpRequest) msg;

            if (HttpHeaders.is100ContinueExpected(request)) {
                send100Continue(ctx);
            }
        }

        if (msg instanceof HttpContent) {
            if (!writeResponse(ctx)) {
                // If keep-alive is off, close the connection once the content is fully written.
                ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    private boolean writeResponse(ChannelHandlerContext ctx) {

        boolean keepAlive = HttpHeaders.isKeepAlive(request);

        QueryStringDecoder urlDecoder = new QueryStringDecoder(request.getUri());
        //TODO
        statusData.setUri(request.getUri());

        // Logic

        if (urlDecoder.path().equals("/redirect")) {
            String urlParam = urlDecoder.parameters().get("url").get(0);

            // Regex pattern from habra

            if (!urlParam.contains("~^(?:(?:https?|ftp|telnet)" +
                    "://(?:[a-z0-9_-]{1,32}(?::[a-z0-9_-]{1,32})?@)?)?(?:(?:[a-z0-9-]{1,128}\\.)+" +
                    "(?:ru|su|com|net|org|mil|edu|arpa|gov|biz|info|aero|inc|name|[a-z]{2})|(?!0)(?:(?!0[^.]|255)[0-9]{1,3}\\.)" +
                    "{3}(?!0|255)[0-9]{1,3})(?:/[a-z0-9.,_@%&?+=\\~/-]*)?(?:#[^ '\\\"&]*)?$~i"))
                throw new RuntimeException("Incorrect url as GET parameter " + urlParam);
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, MOVED_PERMANENTLY);
            response.headers().set(LOCATION, urlParam);
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            Data.getInstance().addUrlNumberOfRedirects(urlParam);
        } else
            switch (urlDecoder.uri()) {
                case "/hello": {
                    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer("Hello world!" +
                            " I am going to do my best to apply for the vacation", CharsetUtil.UTF_8));
                    response.headers().set(CONTENT_TYPE, "text/plain");
                    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

                    if (!keepAlive) {
                        ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                    } else {
                        response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                        ctx.executor().schedule(() -> ctx.writeAndFlush(response), 10, TimeUnit.SECONDS);
                    }
                    break;
                }

                case "/status": {
                    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK,
                            Unpooled.copiedBuffer(Data.getInstance().statusResponse(), CharsetUtil.UTF_8));
                    response.headers().set(CONTENT_TYPE, "html");
                    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
                    if (!keepAlive) {
                        ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                    } else {
                        response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                        ctx.write(response);
                    }
                    break;
                }
                default: {
                    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND, Unpooled.copiedBuffer("Page not found. However, Happy New Year!", CharsetUtil.UTF_8));
                    response.headers().set(CONTENT_TYPE, "text/plain");
                    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
                    if (!keepAlive) {
                        ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                    } else {
                        response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                        ctx.write(response);
                    }
                    break;
                }
            }
        return keepAlive;
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
        ctx.write(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        //TODO
        Data.getInstance().proccessStatusData(statusData, ctx.channel().remoteAddress().toString());
        Data.getInstance().removeActiveCounter();
        ctx.flush();
    }
}
