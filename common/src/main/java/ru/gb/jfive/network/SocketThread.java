package ru.gb.jfive.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketThread extends Thread {

    private final Socket socket;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private SocketThreadListener listener;

    public SocketThread(SocketThreadListener listener, String name, Socket socket) {
        super(name);
        this.socket = socket;
        this.listener = listener;
        start();
    }

    @Override
    public void run() {
        // Здесь описывается процесс типового взаимодействия сокета с внешним миром.
        // Обработчики событий у сервера и у клиента разные
        try {
            listener.onSocketStart(this, socket);
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
            listener.onSocketReady(this, socket);
            // после получения очередного сообщения сокет
            // вызывает обработчик события onReceiveString в объекте листенера
            while (!isInterrupted()) {
                String msg = inputStream.readUTF();
                listener.onReceiveString(this, socket, msg);
            }
        } catch (IOException e) {
            listener.onSocketException(this, e);
        } finally {
            close();
            listener.onSocketStop(this);
        }
    }

    public synchronized boolean sendMessage(String msg) {
        try {
            outputStream.writeUTF(msg);
            outputStream.flush();
            return true;
        } catch (IOException e) {
            listener.onSocketException(this, e);
            close();
            return false;
        }
    }

    public synchronized void close() {
        interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            listener.onSocketException(this, e);
        }
    }

    public DataOutputStream getOutputStream() {
        return outputStream;
    }

}
