package ru.sms.cloud.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import ru.sms.cloud.common.serverin.AuthRequest;
import ru.sms.cloud.common.serverin.FileDeleteRequest;
import ru.sms.cloud.common.serverin.FileListRequest;
import ru.sms.cloud.common.serverin.FileRequest;
import ru.sms.cloud.common.serverout.AuthAnswer;
import ru.sms.cloud.common.serverout.FileListAnswer;
import ru.sms.cloud.common.serverout.FileMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private static final String ROOT_SERVER_STORAGE = "server_storage/";

    private static final String AUTH_SUCCEEDED = "Auth Succeeded";
    private static final String WRONG_CREDENTIALS = "Wrong user credentials";
    private static final String BAD_AUTH_REQUEST = "Bad auth request";
    private static final String AUTH_NEEDED = "Need to authenticate";
    private Path userPath;
    private boolean isAuth = false;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null) return;

            else if (msg instanceof AuthRequest){
                AuthRequest authRequest = (AuthRequest) msg;
                String userName = authRequest.getUserName();
                String pass = authRequest.getPass();
                if (userName!=null && pass!=null) {
                    Database database = Database.INSTANCE;

                    boolean tryAuth = database.tryAuth(userName, pass);
                    if (tryAuth) {
                        this.isAuth = true;
                        userPath = Paths.get(ROOT_SERVER_STORAGE + userName);
                        if (!Files.exists(userPath)) Files.createDirectories(userPath);
                        ctx.writeAndFlush(new AuthAnswer(true, AUTH_SUCCEEDED));
                        sendFileList(ctx);
                    } else {
                        ctx.writeAndFlush(new AuthAnswer(false, WRONG_CREDENTIALS));
                    }
                } else ctx.writeAndFlush(new AuthAnswer(false, BAD_AUTH_REQUEST));
            }
            else if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                Path path = this.userPath.resolve(fr.getFilename());
                if (Files.exists(path)) {
                    FileMessage fm = new FileMessage(path);
                    ctx.writeAndFlush(fm);
                }
            }
            else if (msg instanceof FileListRequest && isAuth)
                sendFileList(ctx);

            else if (msg instanceof FileMessage && isAuth){
                FileMessage fm = (FileMessage) msg;
//                Files.write(Paths.get(ROOT_SERVER_STORAGE + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                Files.write(this.userPath.resolve(fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                sendFileList(ctx);
            }
            else if (msg instanceof FileDeleteRequest){
                String filename = ((FileDeleteRequest) msg).getFilename();
                Path path = this.userPath.resolve(filename);
                if (Files.exists(path)) {
                    Files.delete(path);
                    sendFileList(ctx);
                }

            }
            else ctx.writeAndFlush(new AuthAnswer(false, AUTH_NEEDED));
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private void sendFileList(ChannelHandlerContext ctx) throws IOException {
        List<String> fileNamesList = Files.list(userPath).map(path ->
                path.getFileName().toString()).collect(Collectors.toList());
        ctx.writeAndFlush(new FileListAnswer(fileNamesList));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
