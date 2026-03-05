package com.devikapps.vaikaparts.client.api;

import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.model.Manager;
import com.devikapps.vaikaparts.client.model.ManagerPageResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient.ResponseSpec;
import org.springframework.web.client.RestClientResponseException;

@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    comments = "Generator version: 7.14.0")
public class ManagersApi {
  private ApiClient apiClient;

  public ManagersApi() {
    this(new ApiClient());
  }

  public ManagersApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Retrieve the currently authenticated Manager profile Returns the complete profile of the
   * currently authenticated Manager. **Authorization**: Manager role required.
   *
   * <p><b>200</b> - Manager profile retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Authenticated user is not a Manager.
   *
   * @return Manager
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec getCurrentManagerRequestCreation() throws RestClientResponseException {
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

    ParameterizedTypeReference<Manager> localVarReturnType = new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/managers/me",
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
   * Retrieve the currently authenticated Manager profile Returns the complete profile of the
   * currently authenticated Manager. **Authorization**: Manager role required.
   *
   * <p><b>200</b> - Manager profile retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Authenticated user is not a Manager.
   *
   * @return Manager
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public Manager getCurrentManager() throws RestClientResponseException {
    ParameterizedTypeReference<Manager> localVarReturnType = new ParameterizedTypeReference<>() {};
    return getCurrentManagerRequestCreation().body(localVarReturnType);
  }

  /**
   * Retrieve the currently authenticated Manager profile Returns the complete profile of the
   * currently authenticated Manager. **Authorization**: Manager role required.
   *
   * <p><b>200</b> - Manager profile retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Authenticated user is not a Manager.
   *
   * @return ResponseEntity&lt;Manager&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<Manager> getCurrentManagerWithHttpInfo()
      throws RestClientResponseException {
    ParameterizedTypeReference<Manager> localVarReturnType = new ParameterizedTypeReference<>() {};
    return getCurrentManagerRequestCreation().toEntity(localVarReturnType);
  }

  /**
   * Retrieve the currently authenticated Manager profile Returns the complete profile of the
   * currently authenticated Manager. **Authorization**: Manager role required.
   *
   * <p><b>200</b> - Manager profile retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Authenticated user is not a Manager.
   *
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec getCurrentManagerWithResponseSpec() throws RestClientResponseException {
    return getCurrentManagerRequestCreation();
  }

  /**
   * List all Manager profiles Returns a paginated list of all Manager profiles in the system.
   * **Authorization**: Manager role required.
   *
   * <p><b>200</b> - Manager list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return ManagerPageResponse
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec getManagersRequestCreation(
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

    ParameterizedTypeReference<ManagerPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/managers",
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
   * List all Manager profiles Returns a paginated list of all Manager profiles in the system.
   * **Authorization**: Manager role required.
   *
   * <p><b>200</b> - Manager list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return ManagerPageResponse
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ManagerPageResponse getManagers(
      @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    ParameterizedTypeReference<ManagerPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getManagersRequestCreation(page, size).body(localVarReturnType);
  }

  /**
   * List all Manager profiles Returns a paginated list of all Manager profiles in the system.
   * **Authorization**: Manager role required.
   *
   * <p><b>200</b> - Manager list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return ResponseEntity&lt;ManagerPageResponse&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<ManagerPageResponse> getManagersWithHttpInfo(
      @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    ParameterizedTypeReference<ManagerPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getManagersRequestCreation(page, size).toEntity(localVarReturnType);
  }

  /**
   * List all Manager profiles Returns a paginated list of all Manager profiles in the system.
   * **Authorization**: Manager role required.
   *
   * <p><b>200</b> - Manager list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec getManagersWithResponseSpec(
      @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    return getManagersRequestCreation(page, size);
  }
}
