package com.igot.cb.pores.util;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

class OutboundRequestHandlerServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OutboundRequestHandlerServiceImpl service;
    @Mock
    private Logger mockLogger;

    private final String uri = "http://localhost:8080/test";


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(service, "log", mockLogger);
        when(mockLogger.isDebugEnabled()).thenReturn(true);
    }

    @Test
    void testFetchResultUsingPost_success() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("key", "value");

        when(restTemplate.postForObject(eq(uri), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponse);

        Object result = service.fetchResultUsingPost(uri, Map.of("id", 1));
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals("value", ((Map<?, ?>) result).get("key"));

        verify(restTemplate, times(1)).postForObject(eq(uri), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testFetchResultUsingPost_httpClientErrorException_withJsonBody() throws Exception {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "BadRequest");

        String jsonError = new ObjectMapper().writeValueAsString(errorResponse);
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                jsonError.getBytes(), null);

        when(restTemplate.postForObject(eq(uri), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(exception);

        Object result = service.fetchResultUsingPost(uri, Map.of("id", 2));
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals("BadRequest", ((Map<?, ?>) result).get("error"));
    }

    @Test
    void testFetchResultUsingPost_httpClientErrorException_invalidJson() {
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                "invalid-json".getBytes(), null);

        when(restTemplate.postForObject(eq(uri), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(exception);

        Object result = service.fetchResultUsingPost(uri, Map.of("id", 3));
        assertNull(result);
    }

    @Test
    void testFetchResultUsingPost_genericException() {
        when(restTemplate.postForObject(eq(uri), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        Object result = service.fetchResultUsingPost(uri, Map.of("id", 4));
        assertNull(result);
    }

    @Test
    void testFetchResultUsingPost_debugLoggingEnabled() throws Exception {
        // Arrange
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("status", "ok");

        when(restTemplate.postForObject(eq(uri), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponse);

        // Create a mock logger with isDebugEnabled returning true
        Logger mockLogger = mock(Logger.class);
        when(mockLogger.isDebugEnabled()).thenReturn(true);

        // Inject mock logger via reflection
        Field logField = OutboundRequestHandlerServiceImpl.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(service, mockLogger);

        // Act
        Object result = service.fetchResultUsingPost(uri, Map.of("x", "y"));

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals("ok", ((Map<?, ?>) result).get("status"));

        // Verify debug log was triggered
        verify(mockLogger, atLeastOnce()).debug(anyString());
    }

    @Test
    void testFetchResult_whenDebugEnabled_shouldReturnResponse() {
        Logger mockLogger = mock(Logger.class);
        // Arrange
        Map<String, Object> mockResponse = Map.of("result", "success");
        when(mockLogger.isDebugEnabled()).thenReturn(true);
        when(restTemplate.getForObject(uri, Map.class)).thenReturn(mockResponse);

        // Act
        Object result = service.fetchResult(uri);

        // Assert
        assertEquals(mockResponse, result);
        //verify(mockLogger).debug(anyString());
    }

    @Test
    void testFetchResult_whenHttpClientError_shouldParseErrorResponse() throws Exception {
        Logger mockLogger = mock(Logger.class);
        // Arrange
        String errorJson = "{\"status\":\"error\"}";
        HttpClientErrorException exception = mock(HttpClientErrorException.class);
        when(exception.getResponseBodyAsString()).thenReturn(errorJson);
        when(restTemplate.getForObject(uri, Map.class)).thenThrow(exception);

        when(mockLogger.isDebugEnabled()).thenReturn(false);

        // Act
        Object result = service.fetchResult(uri);

        // Assert
        assertTrue(result instanceof Map);
        assertEquals("error", ((Map<?, ?>) result).get("status"));
        //verify(mockLogger).error(contains("Error received"), eq(exception));
    }

    @Test
    void testFetchResult_whenGeneralException_shouldLogAndReturnNull() {
        Logger mockLogger = mock(Logger.class);
        // Arrange
        when(restTemplate.getForObject(uri, Map.class)).thenThrow(new RuntimeException("Some error"));
        when(mockLogger.isDebugEnabled()).thenReturn(false);

        // Act
        Object result = service.fetchResult(uri);

        // Assert
        assertNull(result);
//        verify(mockLogger).error("Some error");
//        verify(mockLogger).warn(startsWith("Error Response"));
    }

    @Test
    void testFetchResult_whenDebugEnabled_logsRequestUri() throws NoSuchFieldException, IllegalAccessException {
        Logger mockLogger = mock(Logger.class);
        Field logField = OutboundRequestHandlerServiceImpl.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(service, mockLogger);
        // Arrange
        Map<String, Object> mockResponse = Map.of("status", "ok");
        when(mockLogger.isDebugEnabled()).thenReturn(true);
        when(restTemplate.getForObject(uri, Map.class)).thenReturn(mockResponse);

        // Act
        Object result = service.fetchResult(uri);

        // Assert
        assertEquals(mockResponse, result);
        verify(mockLogger).debug(contains("OutboundRequestHandlerServiceImpl"));
        verify(mockLogger).debug(contains(uri));
    }

    @Test
    void testFetchUsingGetWithHeaders_success() {
        String uri = "http://test.com";
        Map<String, String> headers = Map.of("Authorization", "Bearer token");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(Map.of("key", "value"), HttpStatus.OK);

        when(restTemplate.exchange(eq(uri), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        Object result = service.fetchUsingGetWithHeaders(uri, headers);
        assertNotNull(result);
    }

    @Test
    void testFetchUsingGetWithHeadersProfile_HttpClientErrorException() throws JsonProcessingException {
        String uri = "http://bad.com";
        Map<String, String> headers = new HashMap<>();
        String errorJson = new ObjectMapper().writeValueAsString(Map.of("error", "Unauthorized"));

        when(restTemplate.exchange(eq(uri), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized", errorJson.getBytes(), StandardCharsets.UTF_8));

        Object result = service.fetchUsingGetWithHeadersProfile(uri, headers);
        assertNotNull(result);
    }

    @Test
    void testFetchResultUsingPost_error() throws JsonProcessingException {
        String uri = "http://fail.com";
        String errorJson = new ObjectMapper().writeValueAsString(Map.of("error", "bad request"));

        when(restTemplate.postForObject(eq(uri), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad", errorJson.getBytes(), StandardCharsets.UTF_8));

        Map<String, Object> result = service.fetchResultUsingPost(uri, Map.of("fail", "true"), Map.of());
        assertEquals("bad request", result.get("error"));
    }

    @Test
    void testFetchResultUsingPatch_success() {
        String uri = "http://test.com/patch";
        Map<String, String> headers = Map.of("token", "abc");
        Map<String, Object> response = Map.of("patched", true);

        when(restTemplate.patchForObject(eq(uri), any(HttpEntity.class), eq(Map.class))).thenReturn(response);

        Map<String, Object> result = service.fetchResultUsingPatch(uri, Map.of("id", 1), headers);
        assertEquals(true, result.get("patched"));
    }

    @Test
    void testFetchResultUsingPatch_HttpClientErrorException() throws JsonProcessingException {
        String uri = "http://patch-fail.com";
        String errorJson = new ObjectMapper().writeValueAsString(Map.of("error", "patch failed"));

        when(restTemplate.patchForObject(eq(uri), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad", errorJson.getBytes(), StandardCharsets.UTF_8));

        Map<String, Object> result = service.fetchResultUsingPatch(uri, Map.of(), Map.of());
        assertEquals("patch failed", result.get("error"));
    }

    @Test
    void testFetchResultUsingPostAsString_success() {
        String uri = "http://post-string.com";
        String response = "Success";

        when(restTemplate.postForObject(eq(uri), any(HttpEntity.class), eq(String.class))).thenReturn(response);

        Object result = service.fetchResultUsingPostAsString(uri, Map.of("test", "value"));
        assertEquals("Success", result);
    }

    @Test
    void testFetchResultUsingPostAsString_HttpClientErrorException() throws JsonProcessingException {
        String uri = "http://post-string-error.com";
        String errorJson = new ObjectMapper().writeValueAsString(Map.of("error", "fail"));

        when(restTemplate.postForObject(eq(uri), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad", errorJson.getBytes(), StandardCharsets.UTF_8));

        Object result = service.fetchResultUsingPostAsString(uri, Map.of());
        assertEquals("fail", ((Map<?, ?>) result).get("error"));
    }
}
