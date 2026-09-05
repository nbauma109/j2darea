package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.Test;

public class NanoBananaImageClientTest {

    @Test
    public void parseResponseExtractsImageFromInlineData() throws Exception {
        BufferedImage image = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, new Color(255, 0, 0, 255).getRGB());
        image.setRGB(1, 0, new Color(0, 0, 255, 255).getRGB());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        String responseBody =
            "{\"candidates\":[{\"content\":{\"parts\":[{\"inline_data\":{\"mime_type\":\"image/png\",\"data\":\""
                + java.util.Base64.getEncoder().encodeToString(out.toByteArray())
                + "\"}}]}}]}";

        NanoBananaImageClient.Result result = NanoBananaImageClient.parseResponse(responseBody);

        assertNotNull(result.getImage());
        assertEquals(2, result.getImage().getWidth());
        assertEquals(1, result.getImage().getHeight());
        assertEquals(image.getRGB(0, 0), result.getImage().getRGB(0, 0));
        assertEquals(image.getRGB(1, 0), result.getImage().getRGB(1, 0));
        assertEquals(null, result.getNote());
    }

    @Test
    public void parseResponseHandlesLargePayloadWithoutRegexBacktracking() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, new Color(12, 34, 56, 255).getRGB());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        StringBuilder responseBody = new StringBuilder(300000);
        responseBody.append("{\"candidates\":[{\"content\":{\"parts\":[");
        for (int i = 0; i < 2000; i++) {
            responseBody.append("{\"text\":\"filler-").append(i).append("\"},");
        }
        responseBody.append("{\"inline_data\":{\"mime_type\":\"image/png\",\"data\":\"")
            .append(java.util.Base64.getEncoder().encodeToString(out.toByteArray()))
            .append("\"}}]}}]}");

        NanoBananaImageClient.Result result = NanoBananaImageClient.parseResponse(responseBody.toString());

        assertNotNull(result.getImage());
        assertEquals(image.getRGB(0, 0), result.getImage().getRGB(0, 0));
    }

    @Test
    public void parseResponseFailsWhenNoImageIsReturned() {
        try {
            NanoBananaImageClient.parseResponse("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"no image\"}]}}]}");
        } catch (IOException ex) {
            assertTrue(ex.getMessage().contains("returned no image"));
            return;
        }
        throw new AssertionError("Expected parseResponse to fail on non-image bytes.");
    }

    @Test
    public void extractErrorMessageIncludes400Message() {
        String errorBody = "{\"error\":{\"message\":\"Bad input image\"}}";
        assertEquals(
            "Nano Banana 2 rejected the request (HTTP 400): Bad input image",
            NanoBananaImageClient.extractErrorMessage(errorBody, 400)
        );
    }

    @Test
    public void extractErrorMessageExplainsZeroQuota() {
        String errorBody = "{\"error\":{\"message\":\"Quota exceeded for metric: generate_content_free_tier_requests, limit: 0, model: gemini-3.1-flash-image\"}}";
        assertEquals(
            "Nano Banana 2 rejected the request because this API key currently has no usable quota. That usually means billing or plan access is not enabled for this Google AI project.",
            NanoBananaImageClient.extractErrorMessage(errorBody, 429)
        );
    }

    @Test
    public void extractErrorMessageExplainsMissingModelAccess() {
        String errorBody = "{\"error\":{\"message\":\"models/gemini-3.1-flash-image-preview is not found for API version v1beta, or is not supported for generateContent\"}}";
        assertEquals(
            "Nano Banana 2 is not callable from this project or region right now (HTTP 404): models/gemini-3.1-flash-image-preview is not found for API version v1beta, or is not supported for generateContent",
            NanoBananaImageClient.extractErrorMessage(errorBody, 404)
        );
    }

    @Test
    public void buildGenerateContentRequestIncludesPromptAndImages() throws Exception {
        BufferedImage firstImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        BufferedImage secondImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        String request = NanoBananaImageClient.buildGenerateContentRequest("remove background", firstImage, secondImage);

        assertTrue(request.contains("\"text\":\"remove background\""));
        assertTrue(request.contains("\"inline_data\":{\"mime_type\":\"image/png\",\"data\":\""));
        assertTrue(request.contains("\"responseModalities\":[\"Image\"]"));
    }

    @Test
    public void escapeJsonEscapesControlCharacters() {
        assertEquals(
            "line1\\nline2\\t\\\"quoted\\\"",
            NanoBananaImageClient.escapeJson("line1\nline2\t\"quoted\"")
        );
    }
}
