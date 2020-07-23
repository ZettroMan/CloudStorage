package ru.gb.jfive.server.core;

import ru.gb.jfive.msglib.Library;
import ru.gb.jfive.network.SocketThread;
import ru.gb.jfive.network.SocketThreadListener;

import java.net.Socket;

public class ClientThread extends SocketThread {
    private String username;
    private boolean isAuthorized;

    public ClientThread(SocketThreadListener listener, String name, Socket socket) {
        super(listener, name, socket);
    }

    public String getUserName() {
        return username;
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

    void authAccept(String username) {
        isAuthorized = true;
        this.username = username;
        sendMessage(Library.getAuthAccept());
    }

    void authFail() {
        sendMessage(Library.getAuthDenied());
        close();
    }

    void msgFormatError(String msg) {
        sendMessage(Library.getMsgFormatError(msg));
        close();
    }

}
