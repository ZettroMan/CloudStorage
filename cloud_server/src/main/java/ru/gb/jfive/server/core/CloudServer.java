package ru.gb.jfive.server.core;

import ru.gb.jfive.msglib.Library;
import ru.gb.jfive.network.ServerSocketThread;
import ru.gb.jfive.network.ServerSocketThreadListener;
import ru.gb.jfive.network.SocketThread;
import ru.gb.jfive.network.SocketThreadListener;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class CloudServer implements  ServerSocketThreadListener, SocketThreadListener {

    ServerSocketThread server;
    CloudServerListener listener;
    private Vector<SocketThread> clients = new Vector<>();

    public CloudServer(CloudServerListener listener) {
        this.listener = listener;
    }

    public void start(int port) {
        if (server == null || !server.isAlive()) {
            server = new ServerSocketThread(this, "Server", port, 2000);
        } else {
            putLog("Server already started!");
        }
    }

    public void stop() {
        if (server != null && server.isAlive()) {
            server.interrupt();
        } else {
            putLog("Server is not running");
        }
    }

    private void putLog(String msg) {
        listener.onCloudServerCommand(msg);
    }

    /**
     * Server Socket Thread methods
     * */

    @Override
    public void onServerStart(ServerSocketThread thread) {
        putLog("Server started");
        SqlClient.connect();
    }

    @Override
    public void onServerStop(ServerSocketThread thread) {
        putLog("Server stopped");
        for (SocketThread client : clients) {
            client.close();
        }
        SqlClient.disconnect();
    }

    @Override
    public void onServerSocketCreated(ServerSocketThread thread, ServerSocket server) {
        putLog("Server socket created");
    }

    @Override
    public void onServerTimeout(ServerSocketThread thread, ServerSocket server) { }

    @Override
    public void onSocketAccepted(ServerSocketThread thread, ServerSocket server, Socket socket) {
        putLog("Client connected");
        String name = "Socket Thread " + socket.getInetAddress() + ":" + socket.getPort();
        new ClientThread(this, name, socket);
    }

    @Override
    public void onServerException(ServerSocketThread thread, Throwable exception) {
        exception.printStackTrace();
    }

    /**
     * Socket Thread methods
     * */

    @Override
    public synchronized void onSocketStart(SocketThread thread, Socket socket) {

    }

    @Override
    public synchronized void onSocketStop(SocketThread thread) {
        ClientThread client = (ClientThread) thread;
        clients.remove(thread);
    }

    @Override
    public synchronized void onSocketReady(SocketThread thread, Socket socket) {
        putLog("Client is ready to work with me");
        clients.add(thread);
    }

    @Override
    public synchronized void onReceiveString(SocketThread thread, Socket socket, String msg) {
        ClientThread client = (ClientThread) thread;
        putLog("Received message: " + msg);
        if (client.isAuthorized()) {
            try {
                handleAutorizedMessage(client, socket, msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            handleNonAuthorizedMessage(client, msg);
        }
    }

    private void handleNonAuthorizedMessage(ClientThread client, String msg) {
//        /auth_request±login±password
        // принимаем только запросы на авторизацию
        String[] arr = msg.split(Library.DELIMITER);
        if (arr.length != 3 || !arr[0].equals(Library.AUTH_REQUEST)) {
            client.msgFormatError(msg);
            return;
        }
        String login = arr[1];
        String password = arr[2];

        if (!SqlClient.isRegisteredUser(login, password)) {
            putLog("Invalid credentials for user" + login);
            client.authFail();
        } else {
            client.authAccept(login);
        }
    }

    private void handleAutorizedMessage(ClientThread client, Socket socket,  String msg) throws IOException {
        String[] arr = msg.split(Library.DELIMITER);
        String msgType = arr[0];
        switch (msgType) {
            case Library.AUTH_REQUEST:
                break;
            case Library.COMMAND:
                if(arr.length > 1) executeCommand(client, arr[1], socket);
                break;
            default:
                client.sendMessage(Library.getMsgFormatError(msg));
        }
    }

    private void executeCommand(ClientThread client, String commandString, Socket socket) throws IOException {
        String[] arr = commandString.split(" +");
        String command = arr[0];
        switch (command) {
            case "upload":
                if(arr.length < 2) {
                    System.out.println("File name is not provided.");
                    return;
                }
                // recieve the file from client
                DataInputStream is = new DataInputStream(socket.getInputStream());
                String fileName = arr[1];
                System.out.println("fileName: " + fileName);
                File file = new File("./cloud_server/" + fileName);
                file.createNewFile();
                try (FileOutputStream os = new FileOutputStream(file)) {
                    byte[] buffer = new byte[8192];
                    while (true) {
                        int r = is.read(buffer);
                        if (r == -1) break;
                        os.write(buffer, 0, r);
                    }
                }

                System.out.println("File uploaded!");
                break;
            case "download":
                //send the file to client;
                break;
            default:
                client.sendMessage(Library.getMsgFormatError(commandString));
        }
    }

    @Override
    public synchronized void onSocketException(SocketThread thread, Exception exception) {
        exception.printStackTrace();
    }

}
