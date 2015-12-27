package net.didorenko.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

@ChannelHandler.Sharable
public class StatusHandler extends SimpleChannelInboundHandler<Object> {

    private static final StatusHandler STATUS_HANDLER = new StatusHandler();

    public static StatusHandler getInstance() {
        return STATUS_HANDLER;
    }

    final ChannelGroup channels =
            new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private int requestCounter;
    private int uniqueRequestCounter;
    private HashMap<String, Integer> urlNumberOfRedirections = new HashMap<>();
    private HashMap<String, IPStat> IPNumberOfRequests = new HashMap<>();
    private static final byte[] DEFAULT = {'H', 'a', 'p', 'p', 'y', ' ', 'n', 'e', 'w', ' ', 'y', 'e', 'a', 'r', '!'};
    private static final byte[] HELLO_WORLD = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};
    private HttpRequest request;

    private final StringBuilder buf = new StringBuilder();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        channels.add(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        channelActive(ctx);
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
                for (Map.Entry<String, String> h : headers) {
                    String key = h.getKey();
                    String value = h.getValue();
                    buf.append("HEADER: ").append(key).append(" = ").append(value).append("\r\n");
                }
                buf.append("\r\n");
            }

            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
            Map<String, List<String>> params = queryStringDecoder.parameters();
            if (!params.isEmpty()) {
                for (Map.Entry<String, List<String>> p : params.entrySet()) {
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
                buf.append("HELLO_WORLD: ");
                buf.append(content.toString(CharsetUtil.UTF_8));
                buf.append("\r\n");
                appendDecoderResult(buf, request);
            }

            if (msg instanceof LastHttpContent) {
                buf.append("END OF HELLO_WORLD\r\n");

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

    private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
        boolean keepAlive = HttpHeaders.isKeepAlive(request);

        QueryStringDecoder urlDecoder = new QueryStringDecoder(request.getUri());


        if (urlDecoder.path().equals("/redirect")) {
            String urlParam = urlDecoder.parameters().get("url").get(0);
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, MOVED_PERMANENTLY);
            response.headers().set(LOCATION, urlParam);
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

            if (urlNumberOfRedirections.containsKey(urlParam))
                urlNumberOfRedirections.put(urlParam, urlNumberOfRedirections.get(urlParam) + 1);
            else
                urlNumberOfRedirections.put(urlParam, 1);

            if (!keepAlive) {
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                ctx.write(response);
            }
        }
        switch (urlDecoder.uri()) {
            case "/hello": {
                String remoteAddr = ctx.channel().remoteAddress().toString();
                requestCounter++;
                if (IPNumberOfRequests.containsKey(remoteAddr)) {
                    IPStat tmp = IPNumberOfRequests.get(remoteAddr);
                    tmp.addQuery();
                    IPNumberOfRequests.put(remoteAddr, tmp);
                } else {
                    IPNumberOfRequests.put(remoteAddr, new IPStat(LocalDateTime.now(),1));
                    uniqueRequestCounter++;
                }
                break;
            }

            case "/status": {
                channelActive(ctx);
                String remoteAddr = ctx.channel().remoteAddress().toString();
                requestCounter++;
                if (IPNumberOfRequests.containsKey(remoteAddr)) {
                    IPStat tmp = IPNumberOfRequests.get(remoteAddr);
                    tmp.addQuery();
                    IPNumberOfRequests.put(remoteAddr, tmp);
                } else {
                    IPNumberOfRequests.put(remoteAddr, new IPStat(LocalDateTime.now(),1));
                    uniqueRequestCounter++;
                }

                    System.out.println(requestCounter);
                System.out.println(urlNumberOfRedirections);
                System.out.println("channels size" + channels.size());
                break;
            }
            default: {
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(DEFAULT));
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

    private static void appendDecoderResult(StringBuilder buf, HttpObject o) {
        DecoderResult result = o.getDecoderResult();
        if (result.isSuccess()) {
            return;
        }

        buf.append(".. WITH DECODER FAILURE: ");
        buf.append(result.cause());
        buf.append("\r\n");
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
