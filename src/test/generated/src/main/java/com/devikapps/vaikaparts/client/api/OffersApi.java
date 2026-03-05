package com.devikapps.vaikaparts.client.api;

import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.model.Offer;
import com.devikapps.vaikaparts.client.model.OfferPageResponse;
import com.devikapps.vaikaparts.client.model.OfferStatus;
import com.devikapps.vaikaparts.client.model.PartInfoCreateRequest;
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
public class OffersApi {
  private ApiClient apiClient;

  public OffersApi() {
    this(new ApiClient());
  }

  public OffersApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Submit a new offer in response to a demand Creates a new offer on behalf of the authenticated
   * seller. The request is submitted as &#x60;multipart/form-data&#x60;. Part info fields are bound
   * using Spring dot-notation (&#x60;part_info.name&#x60;, &#x60;part_info.car_brand&#x60;, etc.).
   * Images are optional and bound under &#x60;part_info.images&#x60;; maximum 5 files per offer.
   * **Authorization**: Seller role required.
   *
   * <p><b>201</b> - Offer created successfully.
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * <p><b>404</b> - Demand not found.
   *
   * @param demandId Identifier of the demand this offer responds to.
   * @param description Additional context or condition details provided by the seller.
   * @param partInfo The partInfo parameter
   * @return Offer
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec createOfferRequestCreation(
      @jakarta.annotation.Nonnull String demandId,
      @jakarta.annotation.Nonnull String description,
      @jakarta.annotation.Nonnull PartInfoCreateRequest partInfo)
      throws RestClientResponseException {
    Object postBody = null;
    // verify the required parameter 'demandId' is set
    if (demandId == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'demandId' when calling createOffer",
          HttpStatus.BAD_REQUEST.value(),
          HttpStatus.BAD_REQUEST.getReasonPhrase(),
          null,
          null,
          null);
    }
    // verify the required parameter 'description' is set
    if (description == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'description' when calling createOffer",
          HttpStatus.BAD_REQUEST.value(),
          HttpStatus.BAD_REQUEST.getReasonPhrase(),
          null,
          null,
          null);
    }
    // verify the required parameter 'partInfo' is set
    if (partInfo == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'partInfo' when calling createOffer",
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

    if (demandId != null) formParams.add("demand_id", demandId);
    if (description != null) formParams.add("description", description);
    if (partInfo != null) formParams.add("part_info", partInfo);

    final String[] localVarAccepts = {"application/json"};
    final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    final String[] localVarContentTypes = {"multipart/form-data"};
    final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {"supabaseAuth"};

    ParameterizedTypeReference<Offer> localVarReturnType = new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/v1/offers",
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
   * Submit a new offer in response to a demand Creates a new offer on behalf of the authenticated
   * seller. The request is submitted as &#x60;multipart/form-data&#x60;. Part info fields are bound
   * using Spring dot-notation (&#x60;part_info.name&#x60;, &#x60;part_info.car_brand&#x60;, etc.).
   * Images are optional and bound under &#x60;part_info.images&#x60;; maximum 5 files per offer.
   * **Authorization**: Seller role required.
   *
   * <p><b>201</b> - Offer created successfully.
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * <p><b>404</b> - Demand not found.
   *
   * @param demandId Identifier of the demand this offer responds to.
   * @param description Additional context or condition details provided by the seller.
   * @param partInfo The partInfo parameter
   * @return Offer
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public Offer createOffer(
      @jakarta.annotation.Nonnull String demandId,
      @jakarta.annotation.Nonnull String description,
      @jakarta.annotation.Nonnull PartInfoCreateRequest partInfo)
      throws RestClientResponseException {
    ParameterizedTypeReference<Offer> localVarReturnType = new ParameterizedTypeReference<>() {};
    return createOfferRequestCreation(demandId, description, partInfo).body(localVarReturnType);
  }

  /**
   * Submit a new offer in response to a demand Creates a new offer on behalf of the authenticated
   * seller. The request is submitted as &#x60;multipart/form-data&#x60;. Part info fields are bound
   * using Spring dot-notation (&#x60;part_info.name&#x60;, &#x60;part_info.car_brand&#x60;, etc.).
   * Images are optional and bound under &#x60;part_info.images&#x60;; maximum 5 files per offer.
   * **Authorization**: Seller role required.
   *
   * <p><b>201</b> - Offer created successfully.
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * <p><b>404</b> - Demand not found.
   *
   * @param demandId Identifier of the demand this offer responds to.
   * @param description Additional context or condition details provided by the seller.
   * @param partInfo The partInfo parameter
   * @return ResponseEntity&lt;Offer&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<Offer> createOfferWithHttpInfo(
      @jakarta.annotation.Nonnull String demandId,
      @jakarta.annotation.Nonnull String description,
      @jakarta.annotation.Nonnull PartInfoCreateRequest partInfo)
      throws RestClientResponseException {
    ParameterizedTypeReference<Offer> localVarReturnType = new ParameterizedTypeReference<>() {};
    return createOfferRequestCreation(demandId, description, partInfo).toEntity(localVarReturnType);
  }

  /**
   * Submit a new offer in response to a demand Creates a new offer on behalf of the authenticated
   * seller. The request is submitted as &#x60;multipart/form-data&#x60;. Part info fields are bound
   * using Spring dot-notation (&#x60;part_info.name&#x60;, &#x60;part_info.car_brand&#x60;, etc.).
   * Images are optional and bound under &#x60;part_info.images&#x60;; maximum 5 files per offer.
   * **Authorization**: Seller role required.
   *
   * <p><b>201</b> - Offer created successfully.
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * <p><b>404</b> - Demand not found.
   *
   * @param demandId Identifier of the demand this offer responds to.
   * @param description Additional context or condition details provided by the seller.
   * @param partInfo The partInfo parameter
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec createOfferWithResponseSpec(
      @jakarta.annotation.Nonnull String demandId,
      @jakarta.annotation.Nonnull String description,
      @jakarta.annotation.Nonnull PartInfoCreateRequest partInfo)
      throws RestClientResponseException {
    return createOfferRequestCreation(demandId, description, partInfo);
  }

  /**
   * Retrieve an offer by its identifier Returns the full detail view for a single offer. Access is
   * restricted to the owning seller — requests for offers belonging to another principal return 404
   * to prevent information disclosure. **Authorization**: Seller role required.
   *
   * <p><b>200</b> - Offer retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param offerId Unique identifier of the offer.
   * @return Offer
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec getOfferByIdRequestCreation(@jakarta.annotation.Nonnull String offerId)
      throws RestClientResponseException {
    Object postBody = null;
    // verify the required parameter 'offerId' is set
    if (offerId == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'offerId' when calling getOfferById",
          HttpStatus.BAD_REQUEST.value(),
          HttpStatus.BAD_REQUEST.getReasonPhrase(),
          null,
          null,
          null);
    }
    // create path and map variables
    final Map<String, Object> pathParams = new HashMap<>();

    pathParams.put("offerId", offerId);

    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    final HttpHeaders headerParams = new HttpHeaders();
    final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
    final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();

    final String[] localVarAccepts = {"application/json"};
    final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    final String[] localVarContentTypes = {};
    final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {"supabaseAuth"};

    ParameterizedTypeReference<Offer> localVarReturnType = new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/v1/offers/{offerId}",
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
   * Retrieve an offer by its identifier Returns the full detail view for a single offer. Access is
   * restricted to the owning seller — requests for offers belonging to another principal return 404
   * to prevent information disclosure. **Authorization**: Seller role required.
   *
   * <p><b>200</b> - Offer retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param offerId Unique identifier of the offer.
   * @return Offer
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public Offer getOfferById(@jakarta.annotation.Nonnull String offerId)
      throws RestClientResponseException {
    ParameterizedTypeReference<Offer> localVarReturnType = new ParameterizedTypeReference<>() {};
    return getOfferByIdRequestCreation(offerId).body(localVarReturnType);
  }

  /**
   * Retrieve an offer by its identifier Returns the full detail view for a single offer. Access is
   * restricted to the owning seller — requests for offers belonging to another principal return 404
   * to prevent information disclosure. **Authorization**: Seller role required.
   *
   * <p><b>200</b> - Offer retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param offerId Unique identifier of the offer.
   * @return ResponseEntity&lt;Offer&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<Offer> getOfferByIdWithHttpInfo(@jakarta.annotation.Nonnull String offerId)
      throws RestClientResponseException {
    ParameterizedTypeReference<Offer> localVarReturnType = new ParameterizedTypeReference<>() {};
    return getOfferByIdRequestCreation(offerId).toEntity(localVarReturnType);
  }

  /**
   * Retrieve an offer by its identifier Returns the full detail view for a single offer. Access is
   * restricted to the owning seller — requests for offers belonging to another principal return 404
   * to prevent information disclosure. **Authorization**: Seller role required.
   *
   * <p><b>200</b> - Offer retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param offerId Unique identifier of the offer.
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec getOfferByIdWithResponseSpec(@jakarta.annotation.Nonnull String offerId)
      throws RestClientResponseException {
    return getOfferByIdRequestCreation(offerId);
  }

  /**
   * Retrieve paginated offers associated with a specific demand Returns a paginated list of offers
   * submitted in response to the specified demand, as visible to the authenticated seller.
   * **Authorization**: Seller role required.
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
  private ResponseSpec getOffersByDemandIdRequestCreation(
      @jakarta.annotation.Nonnull String demandId,
      @jakarta.annotation.Nullable Integer page,
      @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    Object postBody = null;
    // verify the required parameter 'demandId' is set
    if (demandId == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'demandId' when calling getOffersByDemandId",
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
        "/v1/offers/demand/{demandId}",
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
   * Retrieve paginated offers associated with a specific demand Returns a paginated list of offers
   * submitted in response to the specified demand, as visible to the authenticated seller.
   * **Authorization**: Seller role required.
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
  public OfferPageResponse getOffersByDemandId(
      @jakarta.annotation.Nonnull String demandId,
      @jakarta.annotation.Nullable Integer page,
      @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    ParameterizedTypeReference<OfferPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getOffersByDemandIdRequestCreation(demandId, page, size).body(localVarReturnType);
  }

  /**
   * Retrieve paginated offers associated with a specific demand Returns a paginated list of offers
   * submitted in response to the specified demand, as visible to the authenticated seller.
   * **Authorization**: Seller role required.
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
  public ResponseEntity<OfferPageResponse> getOffersByDemandIdWithHttpInfo(
      @jakarta.annotation.Nonnull String demandId,
      @jakarta.annotation.Nullable Integer page,
      @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    ParameterizedTypeReference<OfferPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getOffersByDemandIdRequestCreation(demandId, page, size).toEntity(localVarReturnType);
  }

  /**
   * Retrieve paginated offers associated with a specific demand Returns a paginated list of offers
   * submitted in response to the specified demand, as visible to the authenticated seller.
   * **Authorization**: Seller role required.
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
  public ResponseSpec getOffersByDemandIdWithResponseSpec(
      @jakarta.annotation.Nonnull String demandId,
      @jakarta.annotation.Nullable Integer page,
      @jakarta.annotation.Nullable Integer size)
      throws RestClientResponseException {
    return getOffersByDemandIdRequestCreation(demandId, page, size);
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
   * List offers submitted by the authenticated seller Returns a paginated list of offers submitted
   * by the currently authenticated seller. Results may be narrowed by lifecycle status.
   * **Authorization**: Seller role required.
   *
   * <p><b>200</b> - Offer list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @param status Filter by offer lifecycle status. Omit to return all statuses.
   * @return OfferPageResponse
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec getSellerOffersRequestCreation(
      @jakarta.annotation.Nullable Integer page,
      @jakarta.annotation.Nullable Integer size,
      @jakarta.annotation.Nullable OfferStatus status)
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

    ParameterizedTypeReference<OfferPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/v1/offers",
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
   * List offers submitted by the authenticated seller Returns a paginated list of offers submitted
   * by the currently authenticated seller. Results may be narrowed by lifecycle status.
   * **Authorization**: Seller role required.
   *
   * <p><b>200</b> - Offer list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @param status Filter by offer lifecycle status. Omit to return all statuses.
   * @return OfferPageResponse
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public OfferPageResponse getSellerOffers(
      @jakarta.annotation.Nullable Integer page,
      @jakarta.annotation.Nullable Integer size,
      @jakarta.annotation.Nullable OfferStatus status)
      throws RestClientResponseException {
    ParameterizedTypeReference<OfferPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getSellerOffersRequestCreation(page, size, status).body(localVarReturnType);
  }

  /**
   * List offers submitted by the authenticated seller Returns a paginated list of offers submitted
   * by the currently authenticated seller. Results may be narrowed by lifecycle status.
   * **Authorization**: Seller role required.
   *
   * <p><b>200</b> - Offer list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @param status Filter by offer lifecycle status. Omit to return all statuses.
   * @return ResponseEntity&lt;OfferPageResponse&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<OfferPageResponse> getSellerOffersWithHttpInfo(
      @jakarta.annotation.Nullable Integer page,
      @jakarta.annotation.Nullable Integer size,
      @jakarta.annotation.Nullable OfferStatus status)
      throws RestClientResponseException {
    ParameterizedTypeReference<OfferPageResponse> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return getSellerOffersRequestCreation(page, size, status).toEntity(localVarReturnType);
  }

  /**
   * List offers submitted by the authenticated seller Returns a paginated list of offers submitted
   * by the currently authenticated seller. Results may be narrowed by lifecycle status.
   * **Authorization**: Seller role required.
   *
   * <p><b>200</b> - Offer list retrieved successfully.
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * @param page Zero-indexed page number for pagination
   * @param size Number of items per page
   * @param status Filter by offer lifecycle status. Omit to return all statuses.
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec getSellerOffersWithResponseSpec(
      @jakarta.annotation.Nullable Integer page,
      @jakarta.annotation.Nullable Integer size,
      @jakarta.annotation.Nullable OfferStatus status)
      throws RestClientResponseException {
    return getSellerOffersRequestCreation(page, size, status);
  }

  /**
   * Update the lifecycle status of an offer Transitions the offer to a new lifecycle status. Only
   * permitted status transitions are accepted; invalid transitions return 400. **Authorization**:
   * Seller role required.
   *
   * <p><b>200</b> - Offer status updated successfully.
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param offerId Unique identifier of the offer.
   * @param status Target lifecycle status to apply to the offer.
   * @return Offer
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec updateOfferStatusRequestCreation(
      @jakarta.annotation.Nonnull String offerId, @jakarta.annotation.Nonnull OfferStatus status)
      throws RestClientResponseException {
    Object postBody = null;
    // verify the required parameter 'offerId' is set
    if (offerId == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'offerId' when calling updateOfferStatus",
          HttpStatus.BAD_REQUEST.value(),
          HttpStatus.BAD_REQUEST.getReasonPhrase(),
          null,
          null,
          null);
    }
    // verify the required parameter 'status' is set
    if (status == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'status' when calling updateOfferStatus",
          HttpStatus.BAD_REQUEST.value(),
          HttpStatus.BAD_REQUEST.getReasonPhrase(),
          null,
          null,
          null);
    }
    // create path and map variables
    final Map<String, Object> pathParams = new HashMap<>();

    pathParams.put("offerId", offerId);

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

    ParameterizedTypeReference<Offer> localVarReturnType = new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/v1/offers/{offerId}/status",
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
   * Update the lifecycle status of an offer Transitions the offer to a new lifecycle status. Only
   * permitted status transitions are accepted; invalid transitions return 400. **Authorization**:
   * Seller role required.
   *
   * <p><b>200</b> - Offer status updated successfully.
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param offerId Unique identifier of the offer.
   * @param status Target lifecycle status to apply to the offer.
   * @return Offer
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public Offer updateOfferStatus(
      @jakarta.annotation.Nonnull String offerId, @jakarta.annotation.Nonnull OfferStatus status)
      throws RestClientResponseException {
    ParameterizedTypeReference<Offer> localVarReturnType = new ParameterizedTypeReference<>() {};
    return updateOfferStatusRequestCreation(offerId, status).body(localVarReturnType);
  }

  /**
   * Update the lifecycle status of an offer Transitions the offer to a new lifecycle status. Only
   * permitted status transitions are accepted; invalid transitions return 400. **Authorization**:
   * Seller role required.
   *
   * <p><b>200</b> - Offer status updated successfully.
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param offerId Unique identifier of the offer.
   * @param status Target lifecycle status to apply to the offer.
   * @return ResponseEntity&lt;Offer&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<Offer> updateOfferStatusWithHttpInfo(
      @jakarta.annotation.Nonnull String offerId, @jakarta.annotation.Nonnull OfferStatus status)
      throws RestClientResponseException {
    ParameterizedTypeReference<Offer> localVarReturnType = new ParameterizedTypeReference<>() {};
    return updateOfferStatusRequestCreation(offerId, status).toEntity(localVarReturnType);
  }

  /**
   * Update the lifecycle status of an offer Transitions the offer to a new lifecycle status. Only
   * permitted status transitions are accepted; invalid transitions return 400. **Authorization**:
   * Seller role required.
   *
   * <p><b>200</b> - Offer status updated successfully.
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Missing or invalid authentication credentials
   *
   * <p><b>403</b> - Insufficient permissions for requested operation
   *
   * <p><b>404</b> - Requested resource does not exist
   *
   * @param offerId Unique identifier of the offer.
   * @param status Target lifecycle status to apply to the offer.
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec updateOfferStatusWithResponseSpec(
      @jakarta.annotation.Nonnull String offerId, @jakarta.annotation.Nonnull OfferStatus status)
      throws RestClientResponseException {
    return updateOfferStatusRequestCreation(offerId, status);
  }
}
