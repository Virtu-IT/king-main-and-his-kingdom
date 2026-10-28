package com.bnp.kingmainandhiskingdom.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds error responses that consist of the matching http.cat picture. The reason is also
 * sent in the X-Error-Message header so that a client (like our frontend) can show it as text.
 * Used by the exception handler (controller errors) and by the security handlers (401 / 403).
 */
@Component
public class HttpCatImages {

    public static final String ERROR_MESSAGE_HEADER = "X-Error-Message";

    private static final Map<Integer, byte[]> IMAGE_CACHE = new ConcurrentHashMap<>();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public ResponseEntity<byte[]> response(HttpStatusCode status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.IMAGE_JPEG)
                .header(ERROR_MESSAGE_HEADER, sanitize(message))
                .body(imageFor(status.value()));
    }

    public byte[] imageFor(int status) {
        return IMAGE_CACHE.computeIfAbsent(status, s -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://http.cat/" + s + ".jpg"))
                        .timeout(Duration.ofSeconds(3))
                        .GET()
                        .build();
                HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200) {
                    return response.body();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
                // no internet: we answer with an empty body, the status code still tells the story
            }
            return new byte[0];
        });
    }

    /** Header values may not contain line breaks or non-ASCII characters. */
    private static String sanitize(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("[^\\x20-\\x7E]", " ");
    }
}
