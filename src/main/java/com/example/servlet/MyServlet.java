package com.example.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import netscape.javascript.JSObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@WebServlet(name = "MyServlet", urlPatterns = "/convert")
public class MyServlet extends HttpServlet {
    private final ExecutorService executor = Executors.newFixedThreadPool(20);
    private final Gson gson = new Gson();
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
            String requestBody = reader.lines().collect(Collectors.joining());
            JSObject requestJson = gson.fromJson(requestBody, JSObject.class);
            // Извлекаем данные из JSON
            String name = requestJson.getMember("name").toString();
            String extension = requestJson.getMember("extension").toString();
            String path = requestJson.getMember("path").toString();
            String fileData = requestJson.getMember("fileData").toString();
            // Декодирование Base64
            byte[] fileBytes;
            try {
                fileBytes = Base64.getDecoder().decode(fileData);
            } catch (IllegalArgumentException e) {
                sendError(response, "Invalid Base64 encoded data.");
                return;
            }
            // Создание файла
            String filePath = String.format("%s/%s.%s", path, name, extension);
            File file = new File(filePath);
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(file);
                fos.write(fileBytes);
                fos.close();
            } catch (IOException e) {
                sendError(response, "Error saving file.");
                return;
            }
            // Ассинхронное выполнение скрипта VBS
            executor.execute(() -> {
                try {
                    // Вызов Script
                    Process process = new ProcessBuilder("your script", "path").start();
                    process.waitFor();
                } catch (Exception e) {
                    try {
                        sendError(response, "Error executing VBS script.");
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
            // Отправка ответа
            sendSuccess(response);
        } catch (Exception e) {
            sendError(response, "Internal server error.");
        }
    }

    private void sendSuccess(HttpServletResponse response) throws IOException {
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("status", "success");
        jsonResponse.addProperty("message", "File received and processing started...");
        response.getWriter().write(gson.toJson(jsonResponse));
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("status", "error");
        jsonResponse.addProperty("message", message);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write(gson.toJson(jsonResponse));
    }
}
