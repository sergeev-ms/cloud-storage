package ru.sms.cloud.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import ru.sms.cloud.common.FileListAnswer;
import ru.sms.cloud.common.FileListRequest;
import ru.sms.cloud.common.FileMessage;
import ru.sms.cloud.common.FileRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

public class MainHandler extends ChannelInboundHandlerAdapter {

    public static final String SERVER_STORAGE = "server_storage/";
    private Path rootPath;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        rootPath = Paths.get(SERVER_STORAGE);
        try {
            if (msg == null) {
                return;
            }
            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                Path path = Paths.get(SERVER_STORAGE + fr.getFilename());
                if (Files.exists(path)) {
                    FileMessage fm = new FileMessage(path);
                    ctx.writeAndFlush(fm);
                }
            }
            if (msg instanceof FileListRequest) {
                List<String> fileNamesList = Files.list(rootPath).map(path -> path.getFileName().toString()).collect(Collectors.toList());
                ctx.writeAndFlush(new FileListAnswer(fileNamesList));
            }
            if (msg instanceof FileMessage){
                FileMessage fm = (FileMessage) msg;
                Files.write(Paths.get(SERVER_STORAGE + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
