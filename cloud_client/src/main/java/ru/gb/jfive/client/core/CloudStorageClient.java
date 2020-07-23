package ru.gb.jfive.client.core;

import ru.gb.jfive.msglib.Library;
import ru.gb.jfive.network.SocketThread;
import ru.gb.jfive.network.SocketThreadListener;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

enum ConnectionState {DISCONNECTED, CONNECTED, AUTH_PASSED}

public class CloudStorageClient implements SocketThreadListener {

    private static ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private static Scanner in = new Scanner(System.in);

    private SocketThread socketThread;
    private String username;
    private String password;

    public static void main(String[] args) {
        String commandLine;

        try {
            CloudStorageClient cloudStorageClient = new CloudStorageClient();

            begin:
            while (true) {
                System.out.print("\nEnter the command: > ");
                commandLine = in.nextLine();
                String[] arr = commandLine.split(" +");
                String command = arr[0];
                switch (command) {
                    case "quit":
                        cloudStorageClient.disconnect();
                        break begin;
                    case "connect":
                        if (arr.length > 2) {
                            cloudStorageClient.connect(arr[1], Integer.parseInt(arr[2]));
                            for (int i = 0; i < 10; i++) {
                                Thread.sleep(1000);
                                if (connectionState == ConnectionState.CONNECTED) break;
                            }
                            if (connectionState != ConnectionState.CONNECTED) {
                                System.out.println("Failed to connect to server");
                            }
                        } else {
                            System.out.println("Please, provide server and port to connect.");
                        }
                        break;
                    case "disconnect":
                        cloudStorageClient.disconnect();
                        break;
                    case "login":
                        if (arr.length > 2) {
                            cloudStorageClient.sendAuth(arr[1], arr[2]);
                        } else {
                            System.out.println("Please, provide username and password to log in.");
                        }
                        break;
                    case "upload":
                        if (arr.length > 1) {
                            cloudStorageClient.uploadFile(arr[1]);
                        }
                        break;
                    case "download":
                        if (arr.length > 1) {
                            cloudStorageClient.downloadFile(arr[1]);
                            System.out.println("File \"" + arr[1] + "\" downloaded from server");
                        }
                        break;
                    case "list":
                        System.out.println("Get the server directory list");
                        break;
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void connect(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        socketThread = new SocketThread(this, "Client", socket);
    }

    public void disconnect() {
        connectionState = ConnectionState.DISCONNECTED;
        socketThread.close();
    }

    public void sendAuth(String username, String password) {
        this.username = username;
        this.password = password;
        if (connectionState == ConnectionState.CONNECTED) {
            socketThread.sendMessage(Library.getAuthRequest(username, password));
        }
    }

    public void sendCommand(String command)  {
        if (connectionState == ConnectionState.AUTH_PASSED) {
            socketThread.sendMessage(Library.getCommand(command));
        }
    }

    public void uploadFile(String filename) throws IOException {
        if (connectionState == ConnectionState.AUTH_PASSED) {
            File inputFile = new File(filename);
            if (!inputFile.exists()) {
                System.out.println("File not found");
                return;
            }

            InputStream is = new FileInputStream(inputFile);
            long size = inputFile.length();
            int count = (int) (size / 8192) / 10 , readBuckets = 0;
            if(count == 0) count = 1;
            // /==========/
            DataOutputStream os = socketThread.getOutputStream();
            byte[] buffer = new byte[8192];
            socketThread.sendMessage(Library.getCommand("upload " + filename));
            System.out.print("/");
            while (is.available() > 0) {
                int readBytes = is.read(buffer);
                readBuckets++;
                if (readBuckets % count == 0) {
                    System.out.print("=");
                }
                os.write(buffer, 0, readBytes);
            }
            System.out.println("/");
            System.out.println("File \"" + filename + "\" uploaded to server");
        } else {
            System.out.println("Authentication needed!");
        }
    }

    public void downloadFile(String filename) {
        if (connectionState == ConnectionState.AUTH_PASSED) {
            socketThread.sendMessage(Library.getCommand("download " + filename));
        } else {
            System.out.println("Authentication needed!");
        }
    }


    @Override
    public void onSocketStart(SocketThread thread, Socket socket) {
        System.out.println("SocketThread started");
    }

    @Override
    public void onSocketStop(SocketThread thread) {
        connectionState = ConnectionState.DISCONNECTED;
        System.out.println("SocketThread stopped");
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        System.out.println("Socket is ready to interact.");
        connectionState = ConnectionState.CONNECTED;
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        String[] arr = msg.split(Library.DELIMITER);
        String msgType = arr[0];
        switch (msgType) {
            case Library.AUTH_ACCEPT:
                System.out.println("User successfully logged in");
                connectionState = ConnectionState.AUTH_PASSED;
                break;
            case Library.AUTH_DENIED:
                System.out.println("Log in failed");
                break;
            case Library.MSG_FORMAT_ERROR:
                System.out.println(msg);
                //socketThread.close();
                break;
            default:
                throw new RuntimeException("Unknown message type: " + msg);
        }
        System.out.println("String \"" + msg + "\" received");
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        exception.printStackTrace();
    }
}
