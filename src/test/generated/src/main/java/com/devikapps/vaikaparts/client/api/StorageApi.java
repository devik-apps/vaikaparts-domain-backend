package com.devikapps.vaikaparts.client.api;

import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.model.UploadProfilePhoto200Response;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
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
public class StorageApi {
  private ApiClient apiClient;

  public StorageApi() {
    this(new ApiClient());
  }

  public StorageApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Remove user profile photograph Deletes the current profile photograph from storage and removes
   * the URL from the user profile.
   *
   * <p><b>204</b> - Profile photograph deleted successfully
   *
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec deleteProfilePhotoRequestCreation() throws RestClientResponseException {
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
        "/users/me/profile-photo",
        HttpMethod.DELETE,
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
   * Remove user profile photograph Deletes the current profile photograph from storage and removes
   * the URL from the user profile.
   *
   * <p><b>204</b> - Profile photograph deleted successfully
   *
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public void deleteProfilePhoto() throws RestClientResponseException {
    ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<>() {};
    deleteProfilePhotoRequestCreation().body(localVarReturnType);
  }

  /**
   * Remove user profile photograph Deletes the current profile photograph from storage and removes
   * the URL from the user profile.
   *
   * <p><b>204</b> - Profile photograph deleted successfully
   *
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<Void> deleteProfilePhotoWithHttpInfo() throws RestClientResponseException {
    ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<>() {};
    return deleteProfilePhotoRequestCreation().toEntity(localVarReturnType);
  }

  /**
   * Remove user profile photograph Deletes the current profile photograph from storage and removes
   * the URL from the user profile.
   *
   * <p><b>204</b> - Profile photograph deleted successfully
   *
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec deleteProfilePhotoWithResponseSpec() throws RestClientResponseException {
    return deleteProfilePhotoRequestCreation();
  }

  /**
   * Upload user profile photograph Uploads a profile photograph to S3 storage and updates the user
   * profile with the resulting URL. The photograph will be displayed in an anonymized format
   * (blurred or masked) in public contexts until a contact unlock transaction is completed. **File
   * Requirements**: - Formats: JPEG, PNG - Maximum size: 5 MB - Recommended dimensions: 400x400
   * pixels minimum
   *
   * <p><b>200</b> - Profile photograph uploaded successfully
   *
   * <p><b>400</b> - Invalid file format or size exceeds limit
   *
   * @param photo The photo parameter
   * @return UploadProfilePhoto200Response
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  private ResponseSpec uploadProfilePhotoRequestCreation(@jakarta.annotation.Nonnull File photo)
      throws RestClientResponseException {
    Object postBody = null;
    // verify the required parameter 'photo' is set
    if (photo == null) {
      throw new RestClientResponseException(
          "Missing the required parameter 'photo' when calling uploadProfilePhoto",
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

    if (photo != null) formParams.add("photo", new FileSystemResource(photo));

    final String[] localVarAccepts = {"application/json"};
    final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
    final String[] localVarContentTypes = {"multipart/form-data"};
    final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] {"supabaseAuth"};

    ParameterizedTypeReference<UploadProfilePhoto200Response> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return apiClient.invokeAPI(
        "/users/me/profile-photo",
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
   * Upload user profile photograph Uploads a profile photograph to S3 storage and updates the user
   * profile with the resulting URL. The photograph will be displayed in an anonymized format
   * (blurred or masked) in public contexts until a contact unlock transaction is completed. **File
   * Requirements**: - Formats: JPEG, PNG - Maximum size: 5 MB - Recommended dimensions: 400x400
   * pixels minimum
   *
   * <p><b>200</b> - Profile photograph uploaded successfully
   *
   * <p><b>400</b> - Invalid file format or size exceeds limit
   *
   * @param photo The photo parameter
   * @return UploadProfilePhoto200Response
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public UploadProfilePhoto200Response uploadProfilePhoto(@jakarta.annotation.Nonnull File photo)
      throws RestClientResponseException {
    ParameterizedTypeReference<UploadProfilePhoto200Response> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return uploadProfilePhotoRequestCreation(photo).body(localVarReturnType);
  }

  /**
   * Upload user profile photograph Uploads a profile photograph to S3 storage and updates the user
   * profile with the resulting URL. The photograph will be displayed in an anonymized format
   * (blurred or masked) in public contexts until a contact unlock transaction is completed. **File
   * Requirements**: - Formats: JPEG, PNG - Maximum size: 5 MB - Recommended dimensions: 400x400
   * pixels minimum
   *
   * <p><b>200</b> - Profile photograph uploaded successfully
   *
   * <p><b>400</b> - Invalid file format or size exceeds limit
   *
   * @param photo The photo parameter
   * @return ResponseEntity&lt;UploadProfilePhoto200Response&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseEntity<UploadProfilePhoto200Response> uploadProfilePhotoWithHttpInfo(
      @jakarta.annotation.Nonnull File photo) throws RestClientResponseException {
    ParameterizedTypeReference<UploadProfilePhoto200Response> localVarReturnType =
        new ParameterizedTypeReference<>() {};
    return uploadProfilePhotoRequestCreation(photo).toEntity(localVarReturnType);
  }

  /**
   * Upload user profile photograph Uploads a profile photograph to S3 storage and updates the user
   * profile with the resulting URL. The photograph will be displayed in an anonymized format
   * (blurred or masked) in public contexts until a contact unlock transaction is completed. **File
   * Requirements**: - Formats: JPEG, PNG - Maximum size: 5 MB - Recommended dimensions: 400x400
   * pixels minimum
   *
   * <p><b>200</b> - Profile photograph uploaded successfully
   *
   * <p><b>400</b> - Invalid file format or size exceeds limit
   *
   * @param photo The photo parameter
   * @return ResponseSpec
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  public ResponseSpec uploadProfilePhotoWithResponseSpec(@jakarta.annotation.Nonnull File photo)
      throws RestClientResponseException {
    return uploadProfilePhotoRequestCreation(photo);
  }
}
