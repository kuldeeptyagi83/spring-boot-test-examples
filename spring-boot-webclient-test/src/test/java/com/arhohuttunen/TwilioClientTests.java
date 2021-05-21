package com.arhohuttunen;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class TwilioClientTests {
    private final BasicJsonTester json = new BasicJsonTester(this.getClass());
    private MockWebServer mockWebServer;
    private TwilioClient twilioClient;

    @BeforeEach
    void setupMockWebServer() {
        mockWebServer = new MockWebServer();

        TwilioClientProperties properties = new TwilioClientProperties();
        properties.setBaseUrl(mockWebServer.url("/").url().toString());
        properties.setAccountSid("ACd936ed6dc1504dd79530f19f57b9c008");

        twilioClient = new TwilioClient(WebClient.create(), properties);
    }

    @Test
    void makesTheCorrectRequest() {
        prepareResponse(response ->
                response.setResponseCode(200)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(getJson("message-response.json"))
        );

        twilioClient.sendSms("+123456", "+234567", "test message");

        expectRequest(request -> {
            assertThat(request.getMethod()).isEqualTo("POST");
            assertThat(request.getPath()).isEqualTo("/Accounts/ACd936ed6dc1504dd79530f19f57b9c008/Messages.json");
        });
    }

    @Test
    void serializesRequest() {
        prepareResponse(response ->
                response.setResponseCode(200)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(getJson("message-response.json"))
        );

        twilioClient.sendSms("+123456", "+234567", "test message");

        expectRequestBody(body -> {
            assertThat(body).extractingJsonPathStringValue("$.from").isEqualTo("+123456");
            assertThat(body).extractingJsonPathStringValue("$.to").isEqualTo("+234567");
            assertThat(body).extractingJsonPathStringValue("$.body").isEqualTo("test message");

        });
    }

    private void prepareResponse(Consumer<MockResponse> consumer) {
        MockResponse response = new MockResponse();
        consumer.accept(response);
        this.mockWebServer.enqueue(response);
    }

    private void expectRequest(Consumer<RecordedRequest> consumer) {
        try {
            consumer.accept(this.mockWebServer.takeRequest());
        }
        catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void expectRequestBody(Consumer<JsonContent<Object>> consumer) {
        try {
            RecordedRequest request = mockWebServer.takeRequest();
            JsonContent<Object> requestBody = json.from(request.getBody().readUtf8());
            consumer.accept(requestBody);
        }
        catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String getJson(String path) {
        try {
            InputStream jsonStream = this.getClass().getClassLoader().getResourceAsStream(path);
            assert jsonStream != null;
            return new String(jsonStream.readAllBytes());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
