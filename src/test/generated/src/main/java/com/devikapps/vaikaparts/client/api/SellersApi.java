package com.devikapps.vaikaparts.client.api;

import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.model.Seller;
import com.devikapps.vaikaparts.client.model.SellerPageResponse;
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
public class SellersApi {
  private ApiClient apiClient;

  public SellersApi() {
    this(new ApiClient());
  }

  public SellersApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Retrieve the currently authenticated Seller profile Returns the complete profile of the
   * currently authenticated Seller including all contact details. **Authorization**: Seller role
   * required.
   *
   * <p><b>200</b> - Seller profile retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Authenticated user is not a Seller.
   *
   * @return Seller
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec getCurrentSellerRequestCreation() throws RestClientResponseException {
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

    ParameterizedTypeReference<Seller> localVarReturnType = new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/sellers/me",
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
   * Retrieve the currently authenticated Seller profile Returns the complete profile of the
   * currently authenticated Seller including all contact details. **Authorization**: Seller role
   * required.
   *
   * <p><b>200</b> - Seller profile retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Authenticated user is not a Seller.
   *
   * @return Seller
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public Seller getCurrentSeller() throws RestClientResponseException {
    ParameterizedTypeReference<Seller> localVarReturnType = new ParameterizedTypeReference<>() {};
    return getCurrentSellerRequestCreation().body(localVarReturnType);
  }

  /**
   * Retrieve the currently authenticated Seller profile Returns the complete profile of the
   * currently authenticated Seller including all contact details. **Authorization**: Seller role
   * required.
   *
   * <p><b>200</b> - Seller profile retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Authenticated user is not a Seller.
   *
   * @return ResponseEntity&lt;Seller&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<Seller> getCurrentSellerWithHttpInfo() throws RestClientResponseException {
    ParameterizedTypeReference<Seller> localVarReturnType = new ParameterizedTypeReference<>() {};
    return getCurrentSellerRequestCreation().toEntity(localVarReturnType);
  }

  /**
   * Retrieve the currently authenticated Seller profile Returns the complete profile of the
   * currently authenticated Seller including all contact details. **Authorization**: Seller role
   * required.
   *
   * <p><b>200</b> - Seller profile retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Authenticated user is not a Seller.
   *
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec getCurrentSellerWithResponseSpec() throws RestClientResponseException {
    return getCurrentSellerRequestCreation();
  }

  /**
   * List all Seller profiles Returns a paginated list of all Seller profiles in the system.
   * **Authorization**: Manager role required.
   *
   * <p><b>200</b> - Seller list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return SellerPageResponse
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec getSellersRequestCreation(
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

    ParameterizedTypeReference<SellerPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/sellers",
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
   * List all Seller profiles Returns a paginated list of all Seller profiles in the system.
   * **Authorization**: Manager role required.
   *
   * <p><b>200</b> - Seller list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return SellerPageResponse
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public SellerPageResponse getSellers(
      @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    ParameterizedTypeReference<SellerPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getSellersRequestCreation(page, size).body(localVarReturnType);
  }

  /**
   * List all Seller profiles Returns a paginated list of all Seller profiles in the system.
   * **Authorization**: Manager role required.
   *
   * <p><b>200</b> - Seller list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return ResponseEntity&lt;SellerPageResponse&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<SellerPageResponse> getSellersWithHttpInfo(
      @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    ParameterizedTypeReference<SellerPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getSellersRequestCreation(page, size).toEntity(localVarReturnType);
  }

  /**
   * List all Seller profiles Returns a paginated list of all Seller profiles in the system.
   * **Authorization**: Manager role required.
   *
   * <p><b>200</b> - Seller list retrieved successfully.
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
  public ResponseSpec getSellersWithResponseSpec(
      @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    return getSellersRequestCreation(page, size);
  }
}
