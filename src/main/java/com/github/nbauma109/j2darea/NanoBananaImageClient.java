package com.github.nbauma109.j2darea;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.imageio.ImageIO;

public class NanoBananaImageClient {

    static final String MODEL_ID = "gemini-3.1-flash-image-preview";
    private static final URI GENERATE_CONTENT_ENDPOINT =
        URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_ID + ":generateContent");

    private final HttpClient httpClient;

    public NanoBananaImageClient() {
        this(HttpClient.newHttpClient());
    }

    NanoBananaImageClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Result editImage(String apiKey, String prompt, BufferedImage... inputImages)
            throws IOException, InterruptedException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IOException("Google AI API key is missing.");
        }
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IOException("No Nano Banana 2 edit instruction was provided.");
        }
        if (inputImages == null || inputImages.length == 0) {
            throw new IOException("No source image was provided.");
        }

        HttpRequest request = HttpRequest.newBuilder(GENERATE_CONTENT_ENDPOINT)
            .header("x-goog-api-key", apiKey.trim())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(buildGenerateContentRequest(prompt, inputImages), StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(extractErrorMessage(response.body(), response.statusCode()));
        }
        return parseResponse(response.body());
    }

    static Result parseResponse(String responseBody) throws IOException {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            throw new IOException("Nano Banana 2 returned an empty response.");
        }

        int searchIndex = 0;
        while (true) {
            int inlineDataIndex = findNextKey(responseBody, searchIndex, "\"inline_data\"", "\"inlineData\"");
            if (inlineDataIndex < 0) {
                break;
            }
            int objectStart = responseBody.indexOf('{', inlineDataIndex);
            if (objectStart < 0) {
                break;
            }
            int objectEnd = findMatchingBrace(responseBody, objectStart);
            if (objectEnd < 0) {
                break;
            }

            String mimeType = findJsonStringValue(responseBody, "\"mime_type\"", objectStart, objectEnd);
            if (mimeType == null) {
                mimeType = findJsonStringValue(responseBody, "\"mimeType\"", objectStart, objectEnd);
            }
            String data = findJsonStringValue(responseBody, "\"data\"", objectStart, objectEnd);
            if (mimeType == null || data == null || !mimeType.toLowerCase().startsWith("image/")) {
                searchIndex = objectEnd + 1;
                continue;
            }

            byte[] imageBytes = Base64.getDecoder().decode(data);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image != null) {
                return new Result(image, null);
            }
            searchIndex = objectEnd + 1;
        }

        String text = findJsonStringValue(responseBody, "\"text\"");
        if (text != null && !text.trim().isEmpty()) {
            throw new IOException("Nano Banana 2 returned no image. Response text: " + firstLine(text));
        }
        throw new IOException("Nano Banana 2 returned no image data.");
    }

    static String extractErrorMessage(String responseBody, int statusCode) {
        String message = findJsonStringValue(responseBody, "\"message\"");
        if (message == null || message.isEmpty()) {
            message = "Nano Banana 2 request failed with HTTP " + statusCode + ".";
        }

        switch (statusCode) {
            case 400:
                return "Nano Banana 2 rejected the request (HTTP 400): " + firstLine(message);
            case 401:
                return "Nano Banana 2 request failed because the Google AI API key is missing (HTTP 401).";
            case 403:
                return "Nano Banana 2 rejected the Google AI API key (HTTP 403). Check that the key is valid and enabled for the Google AI image API.";
            case 404:
                return "Nano Banana 2 is not callable from this project or region right now (HTTP 404): " + firstLine(message);
            case 429:
                if (message.contains("limit: 0")) {
                    return "Nano Banana 2 rejected the request because this API key currently has no usable quota. "
                        + "That usually means billing or plan access is not enabled for this Google AI project.";
                }
                return "Nano Banana 2 rate limit exceeded (HTTP 429): " + firstLine(message);
            case 500:
            case 503:
                return "Nano Banana 2 failed with HTTP " + statusCode + ". Please retry later.";
            default:
                return "Nano Banana 2 request failed with HTTP " + statusCode + ": " + firstLine(message);
        }
    }

    static String buildGenerateContentRequest(String prompt, BufferedImage... inputImages) throws IOException {
        StringBuilder out = new StringBuilder(512);
        out.append("{\"contents\":[{\"parts\":[");
        out.append("{\"text\":\"").append(escapeJson(prompt)).append("\"}");
        for (BufferedImage inputImage : inputImages) {
            if (inputImage == null) {
                continue;
            }
            out.append(",{\"inline_data\":{\"mime_type\":\"image/png\",\"data\":\"")
                .append(encodePngBase64(inputImage))
                .append("\"}}");
        }
        out.append("]}],\"generationConfig\":{\"responseModalities\":[\"Image\"]}}");
        return out.toString();
    }

    private static String encodePngBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream imageOutput = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", imageOutput)) {
            throw new IOException("Unable to encode selection as PNG.");
        }
        return Base64.getEncoder().encodeToString(imageOutput.toByteArray());
    }

    private static int findNextKey(String json, int fromIndex, String... keys) {
        int nextIndex = -1;
        for (String key : keys) {
            int candidate = json.indexOf(key, fromIndex);
            if (candidate >= 0 && (nextIndex < 0 || candidate < nextIndex)) {
                nextIndex = candidate;
            }
        }
        return nextIndex;
    }

    private static int findMatchingBrace(String json, int objectStart) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = objectStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String findJsonStringValue(String json, String key) {
        return findJsonStringValue(json, key, 0, json != null ? json.length() : 0);
    }

    private static String findJsonStringValue(String json, String key, int startInclusive, int endInclusive) {
        if (json == null) {
            return null;
        }
        int keyIndex = json.indexOf(key, startInclusive);
        if (keyIndex < 0 || keyIndex >= endInclusive) {
            return null;
        }
        int colonIndex = json.indexOf(':', keyIndex + key.length());
        if (colonIndex < 0 || colonIndex >= endInclusive) {
            return null;
        }
        int valueStart = colonIndex + 1;
        while (valueStart < endInclusive && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= endInclusive || json.charAt(valueStart) != '"') {
            return null;
        }
        return readJsonString(json, valueStart, endInclusive);
    }

    private static String readJsonString(String json, int openingQuoteIndex, int endInclusive) {
        StringBuilder out = new StringBuilder();
        boolean escaped = false;
        for (int i = openingQuoteIndex + 1; i < endInclusive; i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case '"':
                        out.append('"');
                        break;
                    case '\\':
                        out.append('\\');
                        break;
                    case '/':
                        out.append('/');
                        break;
                    case 'b':
                        out.append('\b');
                        break;
                    case 'f':
                        out.append('\f');
                        break;
                    case 'n':
                        out.append('\n');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    case 'u':
                        if (i + 4 >= endInclusive) {
                            out.append("\\u");
                            break;
                        }
                        String hex = json.substring(i + 1, i + 5);
                        out.append((char) Integer.parseInt(hex, 16));
                        i += 4;
                        break;
                    default:
                        out.append(c);
                        break;
                }
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                return out.toString();
            }
            out.append(c);
        }
        return null;
    }

    private static String firstLine(String value) {
        int newlineIndex = value.indexOf('\n');
        return newlineIndex >= 0 ? value.substring(0, newlineIndex).trim() : value.trim();
    }

    static String escapeJson(String value) {
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", Integer.valueOf(c)));
                    } else {
                        out.append(c);
                    }
                    break;
            }
        }
        return out.toString();
    }

    public static final class Result {

        private final BufferedImage image;
        private final String note;

        Result(BufferedImage image, String note) {
            this.image = image;
            this.note = note;
        }

        public BufferedImage getImage() {
            return image;
        }

        public String getNote() {
            return note;
        }
    }
}
