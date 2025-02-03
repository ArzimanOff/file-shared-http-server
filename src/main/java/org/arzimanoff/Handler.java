package org.arzimanoff;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Handler extends Thread {
    private static final Map<String, String> CONTENT_TYPES = new HashMap<>(){
        {
            put("html", "text/html");
            put("json", "application/json");
            put("txt", "text/plain");
            put("jpg", "image/jpeg");
            put("", "text/plain");
        };
    };

    public static final String NOT_FOUND_MSG_TEXT = "NOT FOUND";

    private Socket socket;
    private String directory;

    Handler(Socket socket, String directory) {
        this.socket = socket;
        this.directory = directory;
    }

    @Override
    public void run() {
        try(var inputStream = this.socket.getInputStream();
            var outputStream = this.socket.getOutputStream();)
        {
            var url = getRequest(inputStream);
            var filePath = Path.of(this.directory + url);

            // если файл существует и не является директорией
            if (Files.exists(filePath) && !Files.isDirectory(filePath)){
                // Файл найден
                var fileExtension = getFileExtension(filePath);
                var type = CONTENT_TYPES.get(fileExtension);
                var fileBytes = Files.readAllBytes(filePath);
                this.sendHeader(
                        outputStream,
                        200,
                        "OK",
                        type,
                        fileBytes.length
                );
                outputStream.write(fileBytes);
            } else {
                // FILE NOT FOUND
                var type = CONTENT_TYPES.get("text");
                this.sendHeader(
                        outputStream,
                        404,
                        NOT_FOUND_MSG_TEXT,
                        type,
                        NOT_FOUND_MSG_TEXT.length()
                );
                outputStream.write(NOT_FOUND_MSG_TEXT.getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getRequest(InputStream inputStream){
        var reader = new Scanner(inputStream).useDelimiter("\r\n");
        var line = reader.next();
        return line.split(" ")[1];
    }

    private String getFileExtension(Path path){
        var name = path.getFileName().toString();
        var extensionStart = name.lastIndexOf('.');
        return (extensionStart == -1) ? "" : name.substring(extensionStart + 1);
    }

    private void sendHeader(OutputStream output, int statusCode, String statusText, String type, long length) {
        var ps = new PrintStream(output);
        ps.printf("HTTP/1.1 %s %s%n", statusCode, statusText);
        ps.printf("Content-Type: %s%n", type);
        ps.printf("Content-Length: %s%n%n", length);
    }
}
