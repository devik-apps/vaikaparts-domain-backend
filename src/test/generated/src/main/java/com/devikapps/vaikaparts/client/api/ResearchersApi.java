package com.devikapps.vaikaparts.client.api;

import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.model.Researcher;
import com.devikapps.vaikaparts.client.model.ResearcherPageResponse;
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
public class ResearchersApi {
  private ApiClient apiClient;

  public ResearchersApi() {
    this(new ApiClient());
  }

  public ResearchersApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Retrieve the currently authenticated Researcher profile Returns the complete profile of the
   * currently authenticated Researcher.
   *
   * <p><b>200</b> - Researcher profile retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Authenticated user is not a Researcher.
   *
   * @return Researcher
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec getCurrentResearcherRequestCreation() throws RestClientResponseException {
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

    ParameterizedTypeReference<Researcher> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/researchers/me",
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
   * Retrieve the currently authenticated Researcher profile Returns the complete profile of the
   * currently authenticated Researcher.
   *
   * <p><b>200</b> - Researcher profile retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Authenticated user is not a Researcher.
   *
   * @return Researcher
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public Researcher getCurrentResearcher() throws RestClientResponseException {
    ParameterizedTypeReference<Researcher> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getCurrentResearcherRequestCreation().body(localVarReturnType);
  }

  /**
   * Retrieve the currently authenticated Researcher profile Returns the complete profile of the
   * currently authenticated Researcher.
   *
   * <p><b>200</b> - Researcher profile retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Authenticated user is not a Researcher.
   *
   * @return ResponseEntity&lt;Researcher&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<Researcher> getCurrentResearcherWithHttpInfo()
      throws RestClientResponseException {
    ParameterizedTypeReference<Researcher> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getCurrentResearcherRequestCreation().toEntity(localVarReturnType);
  }

  /**
   * Retrieve the currently authenticated Researcher profile Returns the complete profile of the
   * currently authenticated Researcher.
   *
   * <p><b>200</b> - Researcher profile retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Authenticated user is not a Researcher.
   *
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec getCurrentResearcherWithResponseSpec() throws RestClientResponseException {
    return getCurrentResearcherRequestCreation();
  }

  /**
   * Retrieve Researcher profile by ID Returns the complete profile of a specific Researcher
   * identified by UUID. **Authorization**: Manager role required
   *
   * <p><b>200</b> - Researcher profile retrieved successfully
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param researcherId Unique identifier of the Researcher
   * @return Researcher
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec getResearcherByIdRequestCreation(
      @jakarta.annotation.Nonnull UUID researcherId) throws RestClientResponseException {
    Object postBody = null;
    // verify the required parameter 'researcherId' is set
    if (researcherId == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'researcherId' when calling getResearcherById",
          HttpStatus.BAD_REQUEST.value(),
          HttpStatus.BAD_REQUEST.getReasonPhrase(),
          null,
          null,
          null);
    }
    // create path and map variables
    final Map<String, Object> pathParams = new HashMap<>();

    pathParams.put("researcherId", researcherId);

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    final HttpHeaders headerParams = new HttpHeaders();
    final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
    final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();

    final String[] localVarAccepts = {"application/json"};
    final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    final String[] localVarContentTypes = {};
    final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {"supabaseAuth"};

    ParameterizedTypeReference<Researcher> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/researchers/{researcherId}",
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
   * Retrieve Researcher profile by ID Returns the complete profile of a specific Researcher
   * identified by UUID. **Authorization**: Manager role required
   *
   * <p><b>200</b> - Researcher profile retrieved successfully
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param researcherId Unique identifier of the Researcher
   * @return Researcher
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public Researcher getResearcherById(@jakarta.annotation.Nonnull UUID researcherId)
      throws RestClientResponseException {
    ParameterizedTypeReference<Researcher> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getResearcherByIdRequestCreation(researcherId).body(localVarReturnType);
  }

  /**
   * Retrieve Researcher profile by ID Returns the complete profile of a specific Researcher
   * identified by UUID. **Authorization**: Manager role required
   *
   * <p><b>200</b> - Researcher profile retrieved successfully
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param researcherId Unique identifier of the Researcher
   * @return ResponseEntity&lt;Researcher&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<Researcher> getResearcherByIdWithHttpInfo(
      @jakarta.annotation.Nonnull UUID researcherId) throws RestClientResponseException {
    ParameterizedTypeReference<Researcher> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getResearcherByIdRequestCreation(researcherId).toEntity(localVarReturnType);
  }

  /**
   * Retrieve Researcher profile by ID Returns the complete profile of a specific Researcher
   * identified by UUID. **Authorization**: Manager role required
   *
   * <p><b>200</b> - Researcher profile retrieved successfully
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param researcherId Unique identifier of the Researcher
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec getResearcherByIdWithResponseSpec(
      @jakarta.annotation.Nonnull UUID researcherId) throws RestClientResponseException {
    return getResearcherByIdRequestCreation(researcherId);
  }

  /**
   * List all Researcher profiles Returns a paginated list of all Researcher profiles in the system.
   * **Authorization**: Manager role required.
   *
   * <p><b>200</b> - Researcher list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return ResearcherPageResponse
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec getResearchersRequestCreation(
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

    ParameterizedTypeReference<ResearcherPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/researchers",
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
   * List all Researcher profiles Returns a paginated list of all Researcher profiles in the system.
   * **Authorization**: Manager role required.
   *
   * <p><b>200</b> - Researcher list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return ResearcherPageResponse
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResearcherPageResponse getResearchers(
      @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    ParameterizedTypeReference<ResearcherPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getResearchersRequestCreation(page, size).body(localVarReturnType);
  }

  /**
   * List all Researcher profiles Returns a paginated list of all Researcher profiles in the system.
   * **Authorization**: Manager role required.
   *
   * <p><b>200</b> - Researcher list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return ResponseEntity&lt;ResearcherPageResponse&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<ResearcherPageResponse> getResearchersWithHttpInfo(
      @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    ParameterizedTypeReference<ResearcherPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getResearchersRequestCreation(page, size).toEntity(localVarReturnType);
  }

  /**
   * List all Researcher profiles Returns a paginated list of all Researcher profiles in the system.
   * **Authorization**: Manager role required.
   *
   * <p><b>200</b> - Researcher list retrieved successfully.
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
  public ResponseSpec getResearchersWithResponseSpec(
      @jakarta.annotation.Nullable Integer page, @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    return getResearchersRequestCreation(page, size);
  }
}
