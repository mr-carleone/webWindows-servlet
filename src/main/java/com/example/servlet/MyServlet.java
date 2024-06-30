package com.example.servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
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

public class MyServlet extends HttpServlet {
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final Gson gson = new Gson();
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
            String requestBody = reader.lines().collect(Collectors.joining());
            JSObject requestJson = gson.fromJson(requestBody, JSObject.class);
            // Извлекаем данные из JSON
            String name = requestJson.getMember("name").toString();
            String extension = requestJson.getMember("extension").toString();
            String path = requestJson.getMember("path").toString();
            String fileData = requestJson.getMember("fileData").toString();
            // Декодирование Base64
            byte[] fileBytes = Base64.getDecoder().decode(fileData);
            // Создание файла
            String filePath = String.format("%s/%s.%s", path, name, extension);
            File file = new File(filePath);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(fileBytes);
            fos.close();
            // Ассинхронное выполнение скрипта VBS
            executor.execute(() -> {
                try {
                    // Вызов Script
                    Process process = new ProcessBuilder("your script", "path").start();
                    process.waitFor();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            // Отправка ответа
            response.setContentType("text/plain");
            PrintWriter out = response.getWriter();
            out.println("File received and processing started");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
