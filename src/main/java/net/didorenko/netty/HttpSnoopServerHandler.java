/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package net.didorenko.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpSnoopServerHandler extends SimpleChannelInboundHandler<Object> {

    private static final byte[] CONTENT = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};
    private HttpRequest request;
    /**
     * Buffer that stores the response content
     */
    private final StringBuilder buf = new StringBuilder();

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest request = this.request = (HttpRequest) msg;

            if (HttpHeaders.is100ContinueExpected(request)) {
                send100Continue(ctx);
            }

            buf.setLength(0);
            buf.append("WELCOME TO THE WILD WILD WEB SERVER\r\n");
            buf.append("===================================\r\n");

            buf.append("VERSION: ").append(request.getProtocolVersion()).append("\r\n");
            buf.append("HOSTNAME: ").append(HttpHeaders.getHost(request, "unknown")).append("\r\n");
            buf.append("REQUEST_URI: ").append(request.getUri()).append("\r\n\r\n");

            HttpHeaders headers = request.headers();
            if (!headers.isEmpty()) {
                for (Entry<String, String> h : headers) {
                    String key = h.getKey();
                    String value = h.getValue();
                    buf.append("HEADER: ").append(key).append(" = ").append(value).append("\r\n");
                }
                buf.append("\r\n");
            }

            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
            Map<String, List<String>> params = queryStringDecoder.parameters();
            if (!params.isEmpty()) {
                for (Entry<String, List<String>> p : params.entrySet()) {
                    String key = p.getKey();
                    List<String> vals = p.getValue();
                    for (String val : vals) {
                        buf.append("PARAM: ").append(key).append(" = ").append(val).append("\r\n");
                    }
                }
                buf.append("\r\n");
            }

            appendDecoderResult(buf, request);
        }

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;

            ByteBuf content = httpContent.content();
            if (content.isReadable()) {
                buf.append("CONTENT: ");
                buf.append(content.toString(CharsetUtil.UTF_8));
                buf.append("\r\n");
                appendDecoderResult(buf, request);
            }

            if (msg instanceof LastHttpContent) {
                buf.append("END OF CONTENT\r\n");

                LastHttpContent trailer = (LastHttpContent) msg;
                if (!trailer.trailingHeaders().isEmpty()) {
                    buf.append("\r\n");
                    for (String name : trailer.trailingHeaders().names()) {
                        for (String value : trailer.trailingHeaders().getAll(name)) {
                            buf.append("TRAILING HEADER: ");
                            buf.append(name).append(" = ").append(value).append("\r\n");
                        }
                    }
                    buf.append("\r\n");
                }

                if (!writeResponse(trailer, ctx)) {
                    // If keep-alive is off, close the connection once the content is fully written.
                    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }
            }
        }
    }

    private static void appendDecoderResult(StringBuilder buf, HttpObject o) {
        DecoderResult result = o.getDecoderResult();
        if (result.isSuccess()) {
            return;
        }

        buf.append(".. WITH DECODER FAILURE: ");
        buf.append(result.cause());
        buf.append("\r\n");
    }

    private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
        boolean keepAlive = HttpHeaders.isKeepAlive(request);


        QueryStringDecoder urlDecoder = new QueryStringDecoder(request.getUri());


        switch (urlDecoder.uri()) {
            case "/redirect": {
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, MOVED_PERMANENTLY);
                response.headers().set(LOCATION, urlDecoder.parameters().get("url").get(0));
                response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

                if (!keepAlive) {
                    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                } else {
                    response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                    ctx.write(response);
                }
                break;
            }

            case "/hello": {
                System.out.println("i am in hello");
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(CONTENT));
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
                break;
            }
        }

//        FullHttpResponse response = new DefaultFullHttpResponse(
//                HTTP_1_1, currentObj.getDecoderResult().isSuccess() ? OK : BAD_REQUEST,
//                Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));
//
//        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
//
//        if (keepAlive) {
//            // Add 'Content-Length' header only for a keep-alive connection.
//            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
//            // Add keep alive header as per:
//            // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
//            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
//        }
//        // Write the response.

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
}