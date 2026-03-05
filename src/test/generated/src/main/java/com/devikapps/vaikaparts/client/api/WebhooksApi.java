package com.devikapps.vaikaparts.client.api;

import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.model.SupabaseDatabaseWebhook;
import com.devikapps.vaikaparts.client.model.SupabaseProfileWebhook200Response;
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
public class WebhooksApi {
  private ApiClient apiClient;

  public WebhooksApi() {
    this(new ApiClient());
  }

  public WebhooksApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Supabase Database webhook handler for profiles table Receives database lifecycle events from
   * Supabase Database Webhooks and synchronizes user profile data with the VaikaParts domain model.
   * This endpoint handles all Supabase Database webhook events for the public.profiles table: -
   * **INSERT**: Creates a new Researcher, Seller, or Manager profile based on
   * user_metadata.user_type - **UPDATE**: Synchronizes metadata changes to the existing user
   * profile - **DELETE**: Soft-deletes the user profile by setting status to DISABLED The user type
   * (RESEARCHER, SELLER, MANAGER) is determined from the record.user_metadata.user_type field. If
   * not specified, defaults to RESEARCHER. **Authentication**: Requests must include a valid
   * Supabase webhook signature or secret token in the Authorization header.
   *
   * <p><b>200</b> - Webhook processed successfully
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Invalid webhook signature or authorization token
   *
   * <p><b>500</b> - Internal server error during webhook processing
   *
   * @param supabaseDatabaseWebhook The supabaseDatabaseWebhook parameter
   * @return SupabaseProfileWebhook200Response
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec supabaseProfileWebhookRequestCreation(
      @jakarta.annotation.Nonnull SupabaseDatabaseWebhook supabaseDatabaseWebhook)
      throws RestClientResponseException {
    Object postBody = supabaseDatabaseWebhook;
    // verify the required parameter 'supabaseDatabaseWebhook' is set
    if (supabaseDatabaseWebhook == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'supabaseDatabaseWebhook' when calling"
              + " supabaseProfileWebhook",
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

    final String[] localVarAccepts = {"application/json"};
    final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    final String[] localVarContentTypes = {"application/json"};
    final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {"supabaseWebhookSignature"};

    ParameterizedTypeReference<SupabaseProfileWebhook200Response> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/v1/webhooks/spb/auth",
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
   * Supabase Database webhook handler for profiles table Receives database lifecycle events from
   * Supabase Database Webhooks and synchronizes user profile data with the VaikaParts domain model.
   * This endpoint handles all Supabase Database webhook events for the public.profiles table: -
   * **INSERT**: Creates a new Researcher, Seller, or Manager profile based on
   * user_metadata.user_type - **UPDATE**: Synchronizes metadata changes to the existing user
   * profile - **DELETE**: Soft-deletes the user profile by setting status to DISABLED The user type
   * (RESEARCHER, SELLER, MANAGER) is determined from the record.user_metadata.user_type field. If
   * not specified, defaults to RESEARCHER. **Authentication**: Requests must include a valid
   * Supabase webhook signature or secret token in the Authorization header.
   *
   * <p><b>200</b> - Webhook processed successfully
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Invalid webhook signature or authorization token
   *
   * <p><b>500</b> - Internal server error during webhook processing
   *
   * @param supabaseDatabaseWebhook The supabaseDatabaseWebhook parameter
   * @return SupabaseProfileWebhook200Response
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public SupabaseProfileWebhook200Response supabaseProfileWebhook(
      @jakarta.annotation.Nonnull SupabaseDatabaseWebhook supabaseDatabaseWebhook)
      throws RestClientResponseException {
    ParameterizedTypeReference<SupabaseProfileWebhook200Response> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return supabaseProfileWebhookRequestCreation(supabaseDatabaseWebhook).body(localVarReturnType);
  }

  /**
   * Supabase Database webhook handler for profiles table Receives database lifecycle events from
   * Supabase Database Webhooks and synchronizes user profile data with the VaikaParts domain model.
   * This endpoint handles all Supabase Database webhook events for the public.profiles table: -
   * **INSERT**: Creates a new Researcher, Seller, or Manager profile based on
   * user_metadata.user_type - **UPDATE**: Synchronizes metadata changes to the existing user
   * profile - **DELETE**: Soft-deletes the user profile by setting status to DISABLED The user type
   * (RESEARCHER, SELLER, MANAGER) is determined from the record.user_metadata.user_type field. If
   * not specified, defaults to RESEARCHER. **Authentication**: Requests must include a valid
   * Supabase webhook signature or secret token in the Authorization header.
   *
   * <p><b>200</b> - Webhook processed successfully
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Invalid webhook signature or authorization token
   *
   * <p><b>500</b> - Internal server error during webhook processing
   *
   * @param supabaseDatabaseWebhook The supabaseDatabaseWebhook parameter
   * @return ResponseEntity&lt;SupabaseProfileWebhook200Response&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<SupabaseProfileWebhook200Response> supabaseProfileWebhookWithHttpInfo(
      @jakarta.annotation.Nonnull SupabaseDatabaseWebhook supabaseDatabaseWebhook)
      throws RestClientResponseException {
    ParameterizedTypeReference<SupabaseProfileWebhook200Response> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return supabaseProfileWebhookRequestCreation(supabaseDatabaseWebhook)
        .toEntity(localVarReturnType);
  }

  /**
   * Supabase Database webhook handler for profiles table Receives database lifecycle events from
   * Supabase Database Webhooks and synchronizes user profile data with the VaikaParts domain model.
   * This endpoint handles all Supabase Database webhook events for the public.profiles table: -
   * **INSERT**: Creates a new Researcher, Seller, or Manager profile based on
   * user_metadata.user_type - **UPDATE**: Synchronizes metadata changes to the existing user
   * profile - **DELETE**: Soft-deletes the user profile by setting status to DISABLED The user type
   * (RESEARCHER, SELLER, MANAGER) is determined from the record.user_metadata.user_type field. If
   * not specified, defaults to RESEARCHER. **Authentication**: Requests must include a valid
   * Supabase webhook signature or secret token in the Authorization header.
   *
   * <p><b>200</b> - Webhook processed successfully
   *
   * <p><b>400</b> - Invalid request parameters or payload
   *
   * <p><b>401</b> - Invalid webhook signature or authorization token
   *
   * <p><b>500</b> - Internal server error during webhook processing
   *
   * @param supabaseDatabaseWebhook The supabaseDatabaseWebhook parameter
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec supabaseProfileWebhookWithResponseSpec(
      @jakarta.annotation.Nonnull SupabaseDatabaseWebhook supabaseDatabaseWebhook)
      throws RestClientResponseException {
    return supabaseProfileWebhookRequestCreation(supabaseDatabaseWebhook);
  }
}
