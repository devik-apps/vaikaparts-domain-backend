package com.devikapps.vaikaparts.client.api;

import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.model.HealthDbGet200Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient.ResponseSpec;
import org.springframework.web.client.RestClientResponseException;

@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    comments = "Generator version: 7.14.0")
public class HealthApi {
  private ApiClient apiClient;

  public HealthApi() {
    this(new ApiClient());
  }

  public HealthApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Check bucket health by uploading, downloading, and presigning a file This endpoint uploads a
   * randomly generated text file to the storage bucket, verifies its content by downloading it, and
   * returns a presigned URL for the file.
   *
   * <p><b>200</b> - Successfully uploaded, verified, and presigned file.
   *
   * <p><b>500</b> - Uploaded and downloaded content mismatch or other error occurred.
   *
   * @return String
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec healthBucketCheckRequestCreation() throws RestClientResponseException {
    Object postBody = null;
    // create path and map variables
    final Map<String, Object> pathParams = new HashMap<>();

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    final HttpHeaders headerParams = new HttpHeaders();
    final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
    final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();

    final String[] localVarAccepts = {"application/json"};
    final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    final String[] localVarContentTypes = {};
    final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {"supabaseAuth"};

    ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/health/bucket",
        HttpMethod.GET,
        pathParams,
        queryParams,
        postBody,
        headerParams,
        cookieParams,
        formParams,
        localVarAccept,
        localVarContentType,
        localVarAuthNames,
        localVarReturnType);
  }

  /**
   * Check bucket health by uploading, downloading, and presigning a file This endpoint uploads a
   * randomly generated text file to the storage bucket, verifies its content by downloading it, and
   * returns a presigned URL for the file.
   *
   * <p><b>200</b> - Successfully uploaded, verified, and presigned file.
   *
   * <p><b>500</b> - Uploaded and downloaded content mismatch or other error occurred.
   *
   * @return String
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public String healthBucketCheck() throws RestClientResponseException {
    ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<>() {};
    return healthBucketCheckRequestCreation().body(localVarReturnType);
  }

  /**
   * Check bucket health by uploading, downloading, and presigning a file This endpoint uploads a
   * randomly generated text file to the storage bucket, verifies its content by downloading it, and
   * returns a presigned URL for the file.
   *
   * <p><b>200</b> - Successfully uploaded, verified, and presigned file.
   *
   * <p><b>500</b> - Uploaded and downloaded content mismatch or other error occurred.
   *
   * @return ResponseEntity&lt;String&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<String> healthBucketCheckWithHttpInfo() throws RestClientResponseException {
    ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<>() {};
    return healthBucketCheckRequestCreation().toEntity(localVarReturnType);
  }

  /**
   * Check bucket health by uploading, downloading, and presigning a file This endpoint uploads a
   * randomly generated text file to the storage bucket, verifies its content by downloading it, and
   * returns a presigned URL for the file.
   *
   * <p><b>200</b> - Successfully uploaded, verified, and presigned file.
   *
   * <p><b>500</b> - Uploaded and downloaded content mismatch or other error occurred.
   *
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec healthBucketCheckWithResponseSpec() throws RestClientResponseException {
    return healthBucketCheckRequestCreation();
  }

  /**
   * Health check for the dummy database Returns a paginated list of Dummy entities.
   *
   * <p><b>200</b> - Successful database health check
   *
   * @param page Page number (0-based)
   * @param size Page size
   * @return HealthDbGet200Response
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec healthDbGetRequestCreation(
      @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    Object postBody = null;
    // create path and map variables
    final Map<String, Object> pathParams = new HashMap<>();

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    final HttpHeaders headerParams = new HttpHeaders();
    final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
    final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();

    queryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
    queryParams.putAll(apiClient.parameterToMultiValueMap(null, "size", size));

    final String[] localVarAccepts = {"application/json"};
    final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    final String[] localVarContentTypes = {};
    final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {"supabaseAuth"};

    ParameterizedTypeReference<HealthDbGet200Response> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/health/db",
        HttpMethod.GET,
        pathParams,
        queryParams,
        postBody,
        headerParams,
        cookieParams,
        formParams,
        localVarAccept,
        localVarContentType,
        localVarAuthNames,
        localVarReturnType);
  }

  /**
   * Health check for the dummy database Returns a paginated list of Dummy entities.
   *
   * <p><b>200</b> - Successful database health check
   *
   * @param page Page number (0-based)
   * @param size Page size
   * @return HealthDbGet200Response
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public HealthDbGet200Response healthDbGet(
      @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    ParameterizedTypeReference<HealthDbGet200Response> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return healthDbGetRequestCreation(page, size).body(localVarReturnType);
  }

  /**
   * Health check for the dummy database Returns a paginated list of Dummy entities.
   *
   * <p><b>200</b> - Successful database health check
   *
   * @param page Page number (0-based)
   * @param size Page size
   * @return ResponseEntity&lt;HealthDbGet200Response&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<HealthDbGet200Response> healthDbGetWithHttpInfo(
      @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    ParameterizedTypeReference<HealthDbGet200Response> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return healthDbGetRequestCreation(page, size).toEntity(localVarReturnType);
  }

  /**
   * Health check for the dummy database Returns a paginated list of Dummy entities.
   *
   * <p><b>200</b> - Successful database health check
   *
   * @param page Page number (0-based)
   * @param size Page size
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec healthDbGetWithResponseSpec(
      @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    return healthDbGetRequestCreation(page, size);
  }

  /**
   * Check if the server is alive
   *
   * <p><b>200</b> - A message showing that the server is alive
   *
   * @return String
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec pongRequestCreation() throws RestClientResponseException {
    Object postBody = null;
    // create path and map variables
    final Map<String, Object> pathParams = new HashMap<>();

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    final HttpHeaders headerParams = new HttpHeaders();
    final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
    final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();

    final String[] localVarAccepts = {"application/json"};
    final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    final String[] localVarContentTypes = {};
    final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {"supabaseAuth"};

    ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/ping",
        HttpMethod.GET,
        pathParams,
        queryParams,
        postBody,
        headerParams,
        cookieParams,
        formParams,
        localVarAccept,
        localVarContentType,
        localVarAuthNames,
        localVarReturnType);
  }

  /**
   * Check if the server is alive
   *
   * <p><b>200</b> - A message showing that the server is alive
   *
   * @return String
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public String pong() throws RestClientResponseException {
    ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<>() {};
    return pongRequestCreation().body(localVarReturnType);
  }

  /**
   * Check if the server is alive
   *
   * <p><b>200</b> - A message showing that the server is alive
   *
   * @return ResponseEntity&lt;String&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<String> pongWithHttpInfo() throws RestClientResponseException {
    ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<>() {};
    return pongRequestCreation().toEntity(localVarReturnType);
  }

  /**
   * Check if the server is alive
   *
   * <p><b>200</b> - A message showing that the server is alive
   *
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec pongWithResponseSpec() throws RestClientResponseException {
    return pongRequestCreation();
  }

  /**
   * Send health check email Sends a test email to verify email service functionality
   *
   * <p><b>200</b> - Email sent successfully
   *
   * <p><b>400</b> - Invalid email address format
   *
   * @param to Email address to send the health check email to
   * @return String
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec sendHealthEmailRequestCreation(@jakarta.annotation.Nonnull String to)
      throws RestClientResponseException {
    Object postBody = null;
    // verify the required parameter 'to' is set
    if (to == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'to' when calling sendHealthEmail",
          HttpStatus.BAD_REQUEST.value(),
          HttpStatus.BAD_REQUEST.getReasonPhrase(),
          null,
          null,
          null);
    }
    // create path and map variables
    final Map<String, Object> pathParams = new HashMap<>();

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    final HttpHeaders headerParams = new HttpHeaders();
    final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
    final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();

    queryParams.putAll(apiClient.parameterToMultiValueMap(null, "to", to));

    final String[] localVarAccepts = {"application/json"};
    final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    final String[] localVarContentTypes = {};
    final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {};

    ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/health/email",
        HttpMethod.GET,
        pathParams,
        queryParams,
        postBody,
        headerParams,
        cookieParams,
        formParams,
        localVarAccept,
        localVarContentType,
        localVarAuthNames,
        localVarReturnType);
  }

  /**
   * Send health check email Sends a test email to verify email service functionality
   *
   * <p><b>200</b> - Email sent successfully
   *
   * <p><b>400</b> - Invalid email address format
   *
   * @param to Email address to send the health check email to
   * @return String
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public String sendHealthEmail(@jakarta.annotation.Nonnull String to)
      throws RestClientResponseException {
    ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<>() {};
    return sendHealthEmailRequestCreation(to).body(localVarReturnType);
  }

  /**
   * Send health check email Sends a test email to verify email service functionality
   *
   * <p><b>200</b> - Email sent successfully
   *
   * <p><b>400</b> - Invalid email address format
   *
   * @param to Email address to send the health check email to
   * @return ResponseEntity&lt;String&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<String> sendHealthEmailWithHttpInfo(@jakarta.annotation.Nonnull String to)
      throws RestClientResponseException {
    ParameterizedTypeReference<String> localVarReturnType = new ParameterizedTypeReference<>() {};
    return sendHealthEmailRequestCreation(to).toEntity(localVarReturnType);
  }

  /**
   * Send health check email Sends a test email to verify email service functionality
   *
   * <p><b>200</b> - Email sent successfully
   *
   * <p><b>400</b> - Invalid email address format
   *
   * @param to Email address to send the health check email to
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec sendHealthEmailWithResponseSpec(@jakarta.annotation.Nonnull String to)
      throws RestClientResponseException {
    return sendHealthEmailRequestCreation(to);
  }

  /**
   * Trigger dummy health check events This endpoint triggers one or more dummy events through the
   * event producer for testing the messaging system (e.g., RabbitMQ or Kafka).
   *
   * <p><b>200</b> - List of UUIDs corresponding to triggered dummy events
   *
   * <p><b>400</b> - Invalid parameter values (e.g., nbEvent not in 1–500)
   *
   * @param nbEvent Number of events to trigger (1–500)
   * @param waitInSeconds Duration (in seconds) to simulate event processing
   * @return List&lt;String&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec triggerDummyEventsRequestCreation(
      @jakarta.annotation.Nullable Integer nbEvent,
      @jakarta.annotation.Nullable Integer waitInSeconds)
      throws RestClientResponseException {
    Object postBody = null;
    // create path and map variables
    final Map<String, Object> pathParams = new HashMap<>();

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    final HttpHeaders headerParams = new HttpHeaders();
    final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
    final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();

    queryParams.putAll(apiClient.parameterToMultiValueMap(null, "nbEvent", nbEvent));
    queryParams.putAll(apiClient.parameterToMultiValueMap(null, "waitInSeconds", waitInSeconds));

    final String[] localVarAccepts = {"application/json"};
    final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    final String[] localVarContentTypes = {};
    final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {"supabaseAuth"};

    ParameterizedTypeReference<List<String>> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/health/message",
        HttpMethod.GET,
        pathParams,
        queryParams,
        postBody,
        headerParams,
        cookieParams,
        formParams,
        localVarAccept,
        localVarContentType,
        localVarAuthNames,
        localVarReturnType);
  }

  /**
   * Trigger dummy health check events This endpoint triggers one or more dummy events through the
   * event producer for testing the messaging system (e.g., RabbitMQ or Kafka).
   *
   * <p><b>200</b> - List of UUIDs corresponding to triggered dummy events
   *
   * <p><b>400</b> - Invalid parameter values (e.g., nbEvent not in 1–500)
   *
   * @param nbEvent Number of events to trigger (1–500)
   * @param waitInSeconds Duration (in seconds) to simulate event processing
   * @return List&lt;String&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public List<String> triggerDummyEvents(
      @jakarta.annotation.Nullable Integer nbEvent,
      @jakarta.annotation.Nullable Integer waitInSeconds)
      throws RestClientResponseException {
    ParameterizedTypeReference<List<String>> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return triggerDummyEventsRequestCreation(nbEvent, waitInSeconds).body(localVarReturnType);
  }

  /**
   * Trigger dummy health check events This endpoint triggers one or more dummy events through the
   * event producer for testing the messaging system (e.g., RabbitMQ or Kafka).
   *
   * <p><b>200</b> - List of UUIDs corresponding to triggered dummy events
   *
   * <p><b>400</b> - Invalid parameter values (e.g., nbEvent not in 1–500)
   *
   * @param nbEvent Number of events to trigger (1–500)
   * @param waitInSeconds Duration (in seconds) to simulate event processing
   * @return ResponseEntity&lt;List&lt;String&gt;&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<List<String>> triggerDummyEventsWithHttpInfo(
      @jakarta.annotation.Nullable Integer nbEvent,
      @jakarta.annotation.Nullable Integer waitInSeconds)
      throws RestClientResponseException {
    ParameterizedTypeReference<List<String>> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return triggerDummyEventsRequestCreation(nbEvent, waitInSeconds).toEntity(localVarReturnType);
  }

  /**
   * Trigger dummy health check events This endpoint triggers one or more dummy events through the
   * event producer for testing the messaging system (e.g., RabbitMQ or Kafka).
   *
   * <p><b>200</b> - List of UUIDs corresponding to triggered dummy events
   *
   * <p><b>400</b> - Invalid parameter values (e.g., nbEvent not in 1–500)
   *
   * @param nbEvent Number of events to trigger (1–500)
   * @param waitInSeconds Duration (in seconds) to simulate event processing
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec triggerDummyEventsWithResponseSpec(
      @jakarta.annotation.Nullable Integer nbEvent,
      @jakarta.annotation.Nullable Integer waitInSeconds)
      throws RestClientResponseException {
    return triggerDummyEventsRequestCreation(nbEvent, waitInSeconds);
  }
}
