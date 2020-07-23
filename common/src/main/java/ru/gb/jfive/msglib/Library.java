package ru.gb.jfive.msglib;

// класс с описанием протокола взаимодействия между клиентом и сервером

public class Library {
    /*
/auth_request±login±password - запрос аутентификации
/auth_accept                 - аутентификация принята
/auth_error                  - неудачная аутентификация
/command                     - послать команду на сервер
/msg_format_error±msg        - неверный формат команды
* */
    public static final String DELIMITER = "±";
    public static final String AUTH_REQUEST = "/auth_request";
    public static final String AUTH_ACCEPT = "/auth_accept";
    public static final String AUTH_DENIED = "/auth_denied";
    public static final String COMMAND = "/command";
    public static final String MSG_FORMAT_ERROR = "/msg_format_error";


    public static String getAuthRequest(String login, String password) {
        return AUTH_REQUEST + DELIMITER + login + DELIMITER + password;
    }

    public static String getAuthAccept() {
        return AUTH_ACCEPT;
    }

    public static String getAuthDenied() {
        return AUTH_DENIED;
    }

    public static String getCommand(String command) {
        return COMMAND + DELIMITER + command;
    }

    public static String getMsgFormatError(String message) {
        return MSG_FORMAT_ERROR + DELIMITER + message;
    }

}
