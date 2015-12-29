
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class ServerHandler extends SimpleChannelInboundHandler {

    private HttpRequest request;
    private StatusData statusData = new StatusData();


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String ipAddr = ctx.channel().remoteAddress().toString().replaceAll(":[0-9]+","");
        Data.getInstance().setBeginTimeAndIp(statusData, ipAddr);
        Data.getInstance().addQueryCounter();
        Data.getInstance().addActiveCounter();
        Data.getInstance().addIpCounter(ipAddr);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {

        if(msg instanceof HttpRequest) {
            request = (HttpRequest) msg;

            statusData.setReceivedBytes(statusData.getSentBytes() + msg.toString().length());

            QueryStringDecoder urlDecoder = new QueryStringDecoder(request.getUri());
            statusData.setUri(request.getUri());


            if (urlDecoder.path().equals("/redirect")) {
                String urlParam = urlDecoder.parameters().get("url").get(0);


                Pattern p = Pattern.compile("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
                Matcher m = p.matcher(urlParam);

                if (!m.matches())
                    throw new RuntimeException("Incorrect url as GET parameter " + urlParam);

                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, MOVED_PERMANENTLY);
                response.headers().set(LOCATION, urlParam);
                response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                Data.getInstance().addUrlNumberOfRedirects(urlParam);
                statusData.setSentBytes(response.toString().length());
            } else
                switch (urlDecoder.uri()) {
                    case "/hello": {
                        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer("Hello world!" +
                                " I am going to do my best to apply for the vacation", CharsetUtil.UTF_8));
                        response.headers().set(CONTENT_TYPE, "text/plain");
                        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

                        ctx.executor().schedule(() -> ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE), 10, TimeUnit.SECONDS);
                        statusData.setSentBytes(response.content().readableBytes());
                    }
                    break;

                    case "/status": {
                        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK,
                                Unpooled.copiedBuffer(Data.getInstance().statusResponse(), CharsetUtil.UTF_8));
                        response.headers().set(CONTENT_TYPE, "html");
                        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
                        ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                        statusData.setSentBytes(response.content().readableBytes());
                    }
                    break;
                    default: {
                        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND, Unpooled.copiedBuffer("Page not found. However, Happy New Year and Merry Christmas!", CharsetUtil.UTF_8));
                        response.headers().set(CONTENT_TYPE, "text/plain");
                        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
                        ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                        //TODO check
                        statusData.setSentBytes(response.content().readableBytes());
                    }
                    break;
                }
        }
}

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        Data.getInstance().processStatusData(statusData);
        Data.getInstance().removeActiveCounter();
        ctx.flush();
    }
}
