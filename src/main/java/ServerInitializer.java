import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;

/**
 * package: net.didorenko.netty
 * project: netty-http-server
 * class:
 *
 * @author: Grigoriy Didorenko
 * @date: 27.12.2015
 */
public class ServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {

        ChannelPipeline channelPipeline = socketChannel.pipeline();

        channelPipeline.addLast(new HttpRequestDecoder());
        channelPipeline.addLast(new HttpRequestEncoder());
    }
}
