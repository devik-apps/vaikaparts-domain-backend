package com.devikapps.vaikaparts.client.api;

import com.devikapps.vaikaparts.client.invoker.ApiClient;
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
public class DocsApi {
  private ApiClient apiClient;

  public DocsApi() {
    this(new ApiClient());
  }

  public DocsApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Redirect root path to Swagger UI
   *
   * <p><b>302</b> - Redirects to /doc
   *
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec rootRedirectRequestCreation() throws RestClientResponseException {
    Object postBody = null;
    // create path and map variables
    final Map<String, Object> pathParams = new HashMap<>();

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    final HttpHeaders headerParams = new HttpHeaders();
    final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
    final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();

    final String[] localVarAccepts = {};
    final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    final String[] localVarContentTypes = {};
    final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {"supabaseAuth"};

    ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/",
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
   * Redirect root path to Swagger UI
   *
   * <p><b>302</b> - Redirects to /doc
   *
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public void rootRedirect() throws RestClientResponseException {
    ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<>() {};
    rootRedirectRequestCreation().body(localVarReturnType);
  }

  /**
   * Redirect root path to Swagger UI
   *
   * <p><b>302</b> - Redirects to /doc
   *
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<Void> rootRedirectWithHttpInfo() throws RestClientResponseException {
    ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<>() {};
    return rootRedirectRequestCreation().toEntity(localVarReturnType);
  }

  /**
   * Redirect root path to Swagger UI
   *
   * <p><b>302</b> - Redirects to /doc
   *
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec rootRedirectWithResponseSpec() throws RestClientResponseException {
    return rootRedirectRequestCreation();
  }
}
