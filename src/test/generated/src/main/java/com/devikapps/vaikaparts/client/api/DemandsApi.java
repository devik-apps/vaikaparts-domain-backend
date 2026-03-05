package com.devikapps.vaikaparts.client.api;

import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.model.Demand;
import com.devikapps.vaikaparts.client.model.DemandPageResponse;
import com.devikapps.vaikaparts.client.model.DemandStatus;
import com.devikapps.vaikaparts.client.model.OfferPageResponse;
import com.devikapps.vaikaparts.client.model.PartCreateRequest;
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
public class DemandsApi {
  private ApiClient apiClient;

  public DemandsApi() {
    this(new ApiClient());
  }

  public DemandsApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Create a new automotive parts demand Creates a new demand on behalf of the authenticated
   * Researcher. The request is submitted as &#x60;multipart/form-data&#x60;. Part fields are bound
   * using Spring dot-notation (&#x60;part.name&#x60;, &#x60;part.carBrand&#x60;, etc.). Images are
   * optional and bound under &#x60;part.images&#x60;; maximum 5 files per demand.
   * **Authorization**: Researcher role required.
   *
   * <p><b>201</b> - Demand created successfully.
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param description Additional context or specifications for the sought part.
   * @param part The part parameter
   * @return Demand
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec createDemandRequestCreation(
      @jakarta.annotation.Nonnull String description,
      @jakarta.annotation.Nonnull PartCreateRequest part)
      throws RestClientResponseException {
    Object postBody = null;
    // verify the required parameter 'description' is set
    if (description == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'description' when calling createDemand",
          HttpStatus.BAD_REQUEST.value(),
          HttpStatus.BAD_REQUEST.getReasonPhrase(),
          null,
          null,
          null);
    }
    // verify the required parameter 'part' is set
    if (part == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'part' when calling createDemand",
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

    if (description != null) formParams.add("description", description);
    if (part != null) formParams.add("part", part);

    final String[] localVarAccepts = {"application/json"};
    final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    final String[] localVarContentTypes = {"multipart/form-data"};
    final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {"supabaseAuth"};

    ParameterizedTypeReference<Demand> localVarReturnType = new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/v1/demands",
        HttpMethod.POST,
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
   * Create a new automotive parts demand Creates a new demand on behalf of the authenticated
   * Researcher. The request is submitted as &#x60;multipart/form-data&#x60;. Part fields are bound
   * using Spring dot-notation (&#x60;part.name&#x60;, &#x60;part.carBrand&#x60;, etc.). Images are
   * optional and bound under &#x60;part.images&#x60;; maximum 5 files per demand.
   * **Authorization**: Researcher role required.
   *
   * <p><b>201</b> - Demand created successfully.
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param description Additional context or specifications for the sought part.
   * @param part The part parameter
   * @return Demand
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public Demand createDemand(
      @jakarta.annotation.Nonnull String description,
      @jakarta.annotation.Nonnull PartCreateRequest part)
      throws RestClientResponseException {
    ParameterizedTypeReference<Demand> localVarReturnType = new ParameterizedTypeReference<>() {};
    return createDemandRequestCreation(description, part).body(localVarReturnType);
  }

  /**
   * Create a new automotive parts demand Creates a new demand on behalf of the authenticated
   * Researcher. The request is submitted as &#x60;multipart/form-data&#x60;. Part fields are bound
   * using Spring dot-notation (&#x60;part.name&#x60;, &#x60;part.carBrand&#x60;, etc.). Images are
   * optional and bound under &#x60;part.images&#x60;; maximum 5 files per demand.
   * **Authorization**: Researcher role required.
   *
   * <p><b>201</b> - Demand created successfully.
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param description Additional context or specifications for the sought part.
   * @param part The part parameter
   * @return ResponseEntity&lt;Demand&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<Demand> createDemandWithHttpInfo(
      @jakarta.annotation.Nonnull String description,
      @jakarta.annotation.Nonnull PartCreateRequest part)
      throws RestClientResponseException {
    ParameterizedTypeReference<Demand> localVarReturnType = new ParameterizedTypeReference<>() {};
    return createDemandRequestCreation(description, part).toEntity(localVarReturnType);
  }

  /**
   * Create a new automotive parts demand Creates a new demand on behalf of the authenticated
   * Researcher. The request is submitted as &#x60;multipart/form-data&#x60;. Part fields are bound
   * using Spring dot-notation (&#x60;part.name&#x60;, &#x60;part.carBrand&#x60;, etc.). Images are
   * optional and bound under &#x60;part.images&#x60;; maximum 5 files per demand.
   * **Authorization**: Researcher role required.
   *
   * <p><b>201</b> - Demand created successfully.
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param description Additional context or specifications for the sought part.
   * @param part The part parameter
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec createDemandWithResponseSpec(
      @jakarta.annotation.Nonnull String description,
      @jakarta.annotation.Nonnull PartCreateRequest part)
      throws RestClientResponseException {
    return createDemandRequestCreation(description, part);
  }

  /**
   * Retrieve a demand by its identifier Returns the full detail view for a single demand. Access is
   * restricted to the owning Researcher — requests for demands belonging to another principal
   * return 404 to prevent information disclosure. **Authorization**: Researcher role required.
   *
   * <p><b>200</b> - Demand retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param demandId Unique identifier of the demand.
   * @return Demand
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec getDemandByIdRequestCreation(@jakarta.annotation.Nonnull String demandId)
      throws RestClientResponseException {
    Object postBody = null;
    // verify the required parameter 'demandId' is set
    if (demandId == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'demandId' when calling getDemandById",
          HttpStatus.BAD_REQUEST.value(),
          HttpStatus.BAD_REQUEST.getReasonPhrase(),
          null,
          null,
          null);
    }
    // create path and map variables
    final Map<String, Object> pathParams = new HashMap<>();

    pathParams.put("demandId", demandId);

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    final HttpHeaders headerParams = new HttpHeaders();
    final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
    final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();

    final String[] localVarAccepts = {"application/json"};
    final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    final String[] localVarContentTypes = {};
    final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {"supabaseAuth"};

    ParameterizedTypeReference<Demand> localVarReturnType = new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/v1/demands/{demandId}",
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
   * Retrieve a demand by its identifier Returns the full detail view for a single demand. Access is
   * restricted to the owning Researcher — requests for demands belonging to another principal
   * return 404 to prevent information disclosure. **Authorization**: Researcher role required.
   *
   * <p><b>200</b> - Demand retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param demandId Unique identifier of the demand.
   * @return Demand
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public Demand getDemandById(@jakarta.annotation.Nonnull String demandId)
      throws RestClientResponseException {
    ParameterizedTypeReference<Demand> localVarReturnType = new ParameterizedTypeReference<>() {};
    return getDemandByIdRequestCreation(demandId).body(localVarReturnType);
  }

  /**
   * Retrieve a demand by its identifier Returns the full detail view for a single demand. Access is
   * restricted to the owning Researcher — requests for demands belonging to another principal
   * return 404 to prevent information disclosure. **Authorization**: Researcher role required.
   *
   * <p><b>200</b> - Demand retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param demandId Unique identifier of the demand.
   * @return ResponseEntity&lt;Demand&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<Demand> getDemandByIdWithHttpInfo(
      @jakarta.annotation.Nonnull String demandId) throws RestClientResponseException {
    ParameterizedTypeReference<Demand> localVarReturnType = new ParameterizedTypeReference<>() {};
    return getDemandByIdRequestCreation(demandId).toEntity(localVarReturnType);
  }

  /**
   * Retrieve a demand by its identifier Returns the full detail view for a single demand. Access is
   * restricted to the owning Researcher — requests for demands belonging to another principal
   * return 404 to prevent information disclosure. **Authorization**: Researcher role required.
   *
   * <p><b>200</b> - Demand retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param demandId Unique identifier of the demand.
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec getDemandByIdWithResponseSpec(@jakarta.annotation.Nonnull String demandId)
      throws RestClientResponseException {
    return getDemandByIdRequestCreation(demandId);
  }

  /**
   * Retrieve paginated offers submitted for a specific demand Returns a paginated list of all
   * offers submitted in response to the specified demand. Access is restricted to the demand owner.
   * **Authorization**: Researcher role required.
   *
   * <p><b>200</b> - Offer list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param demandId Unique identifier of the demand.
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return OfferPageResponse
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec getOffersForDemandRequestCreation(
      @jakarta.annotation.Nonnull String demandId,
      @jakarta.annotation.Nullable Integer page,
      @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    Object postBody = null;
    // verify the required parameter 'demandId' is set
    if (demandId == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'demandId' when calling getOffersForDemand",
          HttpStatus.BAD_REQUEST.value(),
          HttpStatus.BAD_REQUEST.getReasonPhrase(),
          null,
          null,
          null);
    }
    // create path and map variables
    final Map<String, Object> pathParams = new HashMap<>();

    pathParams.put("demandId", demandId);

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

    ParameterizedTypeReference<OfferPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/v1/demands/{demandId}/offers",
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
   * Retrieve paginated offers submitted for a specific demand Returns a paginated list of all
   * offers submitted in response to the specified demand. Access is restricted to the demand owner.
   * **Authorization**: Researcher role required.
   *
   * <p><b>200</b> - Offer list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param demandId Unique identifier of the demand.
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return OfferPageResponse
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public OfferPageResponse getOffersForDemand(
      @jakarta.annotation.Nonnull String demandId,
      @jakarta.annotation.Nullable Integer page,
      @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    ParameterizedTypeReference<OfferPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getOffersForDemandRequestCreation(demandId, page, size).body(localVarReturnType);
  }

  /**
   * Retrieve paginated offers submitted for a specific demand Returns a paginated list of all
   * offers submitted in response to the specified demand. Access is restricted to the demand owner.
   * **Authorization**: Researcher role required.
   *
   * <p><b>200</b> - Offer list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param demandId Unique identifier of the demand.
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return ResponseEntity&lt;OfferPageResponse&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<OfferPageResponse> getOffersForDemandWithHttpInfo(
      @jakarta.annotation.Nonnull String demandId,
      @jakarta.annotation.Nullable Integer page,
      @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    ParameterizedTypeReference<OfferPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getOffersForDemandRequestCreation(demandId, page, size).toEntity(localVarReturnType);
  }

  /**
   * Retrieve paginated offers submitted for a specific demand Returns a paginated list of all
   * offers submitted in response to the specified demand. Access is restricted to the demand owner.
   * **Authorization**: Researcher role required.
   *
   * <p><b>200</b> - Offer list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param demandId Unique identifier of the demand.
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec getOffersForDemandWithResponseSpec(
      @jakarta.annotation.Nonnull String demandId,
      @jakarta.annotation.Nullable Integer page,
      @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    return getOffersForDemandRequestCreation(demandId, page, size);
  }

  /**
   * List researcher demands with optional status filtering Returns a paginated list of demands
   * belonging to the authenticated Researcher. Results may be narrowed by lifecycle status.
   * **Authorization**: Researcher role required.
   *
   * <p><b>200</b> - Demand list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @param status Filter by demand lifecycle status. Omit to return all statuses.
   * @return DemandPageResponse
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec getResearcherDemandsRequestCreation(
      @jakarta.annotation.Nullable Integer page,
      @jakarta.annotation.Nullable Integer size,
      @jakarta.annotation.Nullable DemandStatus status)
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
    queryParams.putAll(apiClient.parameterToMultiValueMap(null, "status", status));

    final String[] localVarAccepts = {"application/json"};
    final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    final String[] localVarContentTypes = {};
    final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {"supabaseAuth"};

    ParameterizedTypeReference<DemandPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/v1/demands",
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
   * List researcher demands with optional status filtering Returns a paginated list of demands
   * belonging to the authenticated Researcher. Results may be narrowed by lifecycle status.
   * **Authorization**: Researcher role required.
   *
   * <p><b>200</b> - Demand list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @param status Filter by demand lifecycle status. Omit to return all statuses.
   * @return DemandPageResponse
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public DemandPageResponse getResearcherDemands(
      @jakarta.annotation.Nullable Integer page,
      @jakarta.annotation.Nullable Integer size,
      @jakarta.annotation.Nullable DemandStatus status)
      throws RestClientResponseException {
    ParameterizedTypeReference<DemandPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getResearcherDemandsRequestCreation(page, size, status).body(localVarReturnType);
  }

  /**
   * List researcher demands with optional status filtering Returns a paginated list of demands
   * belonging to the authenticated Researcher. Results may be narrowed by lifecycle status.
   * **Authorization**: Researcher role required.
   *
   * <p><b>200</b> - Demand list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @param status Filter by demand lifecycle status. Omit to return all statuses.
   * @return ResponseEntity&lt;DemandPageResponse&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<DemandPageResponse> getResearcherDemandsWithHttpInfo(
      @jakarta.annotation.Nullable Integer page,
      @jakarta.annotation.Nullable Integer size,
      @jakarta.annotation.Nullable DemandStatus status)
      throws RestClientResponseException {
    ParameterizedTypeReference<DemandPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getResearcherDemandsRequestCreation(page, size, status).toEntity(localVarReturnType);
  }

  /**
   * List researcher demands with optional status filtering Returns a paginated list of demands
   * belonging to the authenticated Researcher. Results may be narrowed by lifecycle status.
   * **Authorization**: Researcher role required.
   *
   * <p><b>200</b> - Demand list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @param status Filter by demand lifecycle status. Omit to return all statuses.
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec getResearcherDemandsWithResponseSpec(
      @jakarta.annotation.Nullable Integer page,
      @jakarta.annotation.Nullable Integer size,
      @jakarta.annotation.Nullable DemandStatus status)
      throws RestClientResponseException {
    return getResearcherDemandsRequestCreation(page, size, status);
  }

  /**
   * Update the lifecycle status of a demand Transitions the demand to a new lifecycle status. Only
   * permitted status transitions are accepted; invalid transitions return 400. **Authorization**:
   * Researcher role required.
   *
   * <p><b>200</b> - Demand status updated successfully.
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param demandId Unique identifier of the demand.
   * @param status Target lifecycle status to apply to the demand.
   * @return Demand
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec updateDemandStatusRequestCreation(
      @jakarta.annotation.Nonnull String demandId, @jakarta.annotation.Nonnull DemandStatus status)
      throws RestClientResponseException {
    Object postBody = null;
    // verify the required parameter 'demandId' is set
    if (demandId == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'demandId' when calling updateDemandStatus",
          HttpStatus.BAD_REQUEST.value(),
          HttpStatus.BAD_REQUEST.getReasonPhrase(),
          null,
          null,
          null);
    }
    // verify the required parameter 'status' is set
    if (status == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'status' when calling updateDemandStatus",
          HttpStatus.BAD_REQUEST.value(),
          HttpStatus.BAD_REQUEST.getReasonPhrase(),
          null,
          null,
          null);
    }
    // create path and map variables
    final Map<String, Object> pathParams = new HashMap<>();

    pathParams.put("demandId", demandId);

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    final HttpHeaders headerParams = new HttpHeaders();
    final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
    final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();

    queryParams.putAll(apiClient.parameterToMultiValueMap(null, "status", status));

    final String[] localVarAccepts = {"application/json"};
    final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    final String[] localVarContentTypes = {};
    final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {"supabaseAuth"};

    ParameterizedTypeReference<Demand> localVarReturnType = new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/v1/demands/{demandId}/status",
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
   * Update the lifecycle status of a demand Transitions the demand to a new lifecycle status. Only
   * permitted status transitions are accepted; invalid transitions return 400. **Authorization**:
   * Researcher role required.
   *
   * <p><b>200</b> - Demand status updated successfully.
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param demandId Unique identifier of the demand.
   * @param status Target lifecycle status to apply to the demand.
   * @return Demand
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public Demand updateDemandStatus(
      @jakarta.annotation.Nonnull String demandId, @jakarta.annotation.Nonnull DemandStatus status)
      throws RestClientResponseException {
    ParameterizedTypeReference<Demand> localVarReturnType = new ParameterizedTypeReference<>() {};
    return updateDemandStatusRequestCreation(demandId, status).body(localVarReturnType);
  }

  /**
   * Update the lifecycle status of a demand Transitions the demand to a new lifecycle status. Only
   * permitted status transitions are accepted; invalid transitions return 400. **Authorization**:
   * Researcher role required.
   *
   * <p><b>200</b> - Demand status updated successfully.
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param demandId Unique identifier of the demand.
   * @param status Target lifecycle status to apply to the demand.
   * @return ResponseEntity&lt;Demand&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<Demand> updateDemandStatusWithHttpInfo(
      @jakarta.annotation.Nonnull String demandId, @jakarta.annotation.Nonnull DemandStatus status)
      throws RestClientResponseException {
    ParameterizedTypeReference<Demand> localVarReturnType = new ParameterizedTypeReference<>() {};
    return updateDemandStatusRequestCreation(demandId, status).toEntity(localVarReturnType);
  }

  /**
   * Update the lifecycle status of a demand Transitions the demand to a new lifecycle status. Only
   * permitted status transitions are accepted; invalid transitions return 400. **Authorization**:
   * Researcher role required.
   *
   * <p><b>200</b> - Demand status updated successfully.
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param demandId Unique identifier of the demand.
   * @param status Target lifecycle status to apply to the demand.
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec updateDemandStatusWithResponseSpec(
      @jakarta.annotation.Nonnull String demandId, @jakarta.annotation.Nonnull DemandStatus status)
      throws RestClientResponseException {
    return updateDemandStatusRequestCreation(demandId, status);
  }
}
