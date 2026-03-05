package com.devikapps.vaikaparts.client.api;

import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.model.Notification;
import com.devikapps.vaikaparts.client.model.NotificationPageResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
public class NotificationsApi {
  private ApiClient apiClient;

  public NotificationsApi() {
    this(new ApiClient());
  }

  public NotificationsApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Retrieve seller notifications Returns a paginated list of notifications for the current active
   * seller.
   *
   * <p><b>200</b> - Notifications retrieved successfully
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return NotificationPageResponse
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec fetchNotificationsRequestCreation(
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

    ParameterizedTypeReference<NotificationPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/v1/notifications",
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
   * Retrieve seller notifications Returns a paginated list of notifications for the current active
   * seller.
   *
   * <p><b>200</b> - Notifications retrieved successfully
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return NotificationPageResponse
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public NotificationPageResponse fetchNotifications(
      @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    ParameterizedTypeReference<NotificationPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return fetchNotificationsRequestCreation(page, size).body(localVarReturnType);
  }

  /**
   * Retrieve seller notifications Returns a paginated list of notifications for the current active
   * seller.
   *
   * <p><b>200</b> - Notifications retrieved successfully
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return ResponseEntity&lt;NotificationPageResponse&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<NotificationPageResponse> fetchNotificationsWithHttpInfo(
      @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    ParameterizedTypeReference<NotificationPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return fetchNotificationsRequestCreation(page, size).toEntity(localVarReturnType);
  }

  /**
   * Retrieve seller notifications Returns a paginated list of notifications for the current active
   * seller.
   *
   * <p><b>200</b> - Notifications retrieved successfully
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec fetchNotificationsWithResponseSpec(
      @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    return fetchNotificationsRequestCreation(page, size);
  }

  /**
   * Get notification with given id This endpoints allows sellers to fetch the notifications object
   * with the given id.
   *
   * <p><b>202</b> - Fetched notification with the corresponding given id
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param notificationId Unique identifier of the notification
   * @return Notification
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec getNotificationRequestCreation(
      @jakarta.annotation.Nonnull UUID notificationId) throws RestClientResponseException {
    Object postBody = null;
    // verify the required parameter 'notificationId' is set
    if (notificationId == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'notificationId' when calling getNotification",
          HttpStatus.BAD_REQUEST.value(),
          HttpStatus.BAD_REQUEST.getReasonPhrase(),
          null,
          null,
          null);
    }
    // create path and map variables
    final Map<String, Object> pathParams = new HashMap<>();

    pathParams.put("notificationId", notificationId);

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    final HttpHeaders headerParams = new HttpHeaders();
    final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
    final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();

    final String[] localVarAccepts = {"appliction/json", "application/json"};
    final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    final String[] localVarContentTypes = {};
    final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {"supabaseAuth"};

    ParameterizedTypeReference<Notification> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/v1/notifications/{notificationId}",
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
   * Get notification with given id This endpoints allows sellers to fetch the notifications object
   * with the given id.
   *
   * <p><b>202</b> - Fetched notification with the corresponding given id
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param notificationId Unique identifier of the notification
   * @return Notification
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public Notification getNotification(@jakarta.annotation.Nonnull UUID notificationId)
      throws RestClientResponseException {
    ParameterizedTypeReference<Notification> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getNotificationRequestCreation(notificationId).body(localVarReturnType);
  }

  /**
   * Get notification with given id This endpoints allows sellers to fetch the notifications object
   * with the given id.
   *
   * <p><b>202</b> - Fetched notification with the corresponding given id
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param notificationId Unique identifier of the notification
   * @return ResponseEntity&lt;Notification&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<Notification> getNotificationWithHttpInfo(
      @jakarta.annotation.Nonnull UUID notificationId) throws RestClientResponseException {
    ParameterizedTypeReference<Notification> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getNotificationRequestCreation(notificationId).toEntity(localVarReturnType);
  }

  /**
   * Get notification with given id This endpoints allows sellers to fetch the notifications object
   * with the given id.
   *
   * <p><b>202</b> - Fetched notification with the corresponding given id
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param notificationId Unique identifier of the notification
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec getNotificationWithResponseSpec(
      @jakarta.annotation.Nonnull UUID notificationId) throws RestClientResponseException {
    return getNotificationRequestCreation(notificationId);
  }

  /**
   * Mark notification as read Updates the read status of a specific notification.
   *
   * <p><b>202</b> - Notification marked as read
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param notificationId Unique identifier of the notification
   * @return Notification
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec markAsReadRequestCreation(@jakarta.annotation.Nonnull UUID notificationId)
      throws RestClientResponseException {
    Object postBody = null;
    // verify the required parameter 'notificationId' is set
    if (notificationId == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'notificationId' when calling markAsRead",
          HttpStatus.BAD_REQUEST.value(),
          HttpStatus.BAD_REQUEST.getReasonPhrase(),
          null,
          null,
          null);
    }
    // create path and map variables
    final Map<String, Object> pathParams = new HashMap<>();

    pathParams.put("notificationId", notificationId);

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    final HttpHeaders headerParams = new HttpHeaders();
    final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
    final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();

    final String[] localVarAccepts = {"appliction/json", "application/json"};
    final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    final String[] localVarContentTypes = {};
    final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {"supabaseAuth"};

    ParameterizedTypeReference<Notification> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/v1/notifications/mark-as-read/{notificationId}",
        HttpMethod.PATCH,
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
   * Mark notification as read Updates the read status of a specific notification.
   *
   * <p><b>202</b> - Notification marked as read
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param notificationId Unique identifier of the notification
   * @return Notification
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public Notification markAsRead(@jakarta.annotation.Nonnull UUID notificationId)
      throws RestClientResponseException {
    ParameterizedTypeReference<Notification> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return markAsReadRequestCreation(notificationId).body(localVarReturnType);
  }

  /**
   * Mark notification as read Updates the read status of a specific notification.
   *
   * <p><b>202</b> - Notification marked as read
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param notificationId Unique identifier of the notification
   * @return ResponseEntity&lt;Notification&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<Notification> markAsReadWithHttpInfo(
      @jakarta.annotation.Nonnull UUID notificationId) throws RestClientResponseException {
    ParameterizedTypeReference<Notification> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return markAsReadRequestCreation(notificationId).toEntity(localVarReturnType);
  }

  /**
   * Mark notification as read Updates the read status of a specific notification.
   *
   * <p><b>202</b> - Notification marked as read
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param notificationId Unique identifier of the notification
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec markAsReadWithResponseSpec(@jakarta.annotation.Nonnull UUID notificationId)
      throws RestClientResponseException {
    return markAsReadRequestCreation(notificationId);
  }
}
