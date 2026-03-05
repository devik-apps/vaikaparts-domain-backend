# StorageApi

All URIs are relative to *https://vaikaparts-domain-backend.onrender.com*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteProfilePhoto**](StorageApi.md#deleteProfilePhoto) | **DELETE** /users/me/profile-photo | Remove user profile photograph |
| [**uploadProfilePhoto**](StorageApi.md#uploadProfilePhoto) | **POST** /users/me/profile-photo | Upload user profile photograph |



## deleteProfilePhoto

> deleteProfilePhoto()

Remove user profile photograph

Deletes the current profile photograph from storage and removes the URL from the user profile.

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.StorageApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        StorageApi apiInstance = new StorageApi(defaultClient);
        try {
            apiInstance.deleteProfilePhoto();
        } catch (ApiException e) {
            System.err.println("Exception when calling StorageApi#deleteProfilePhoto");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters

This endpoint does not need any parameter.

### Return type

null (empty response body)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Profile photograph deleted successfully |  -  |


## uploadProfilePhoto

> UploadProfilePhoto200Response uploadProfilePhoto(photo)

Upload user profile photograph

Uploads a profile photograph to S3 storage and updates the user profile with the resulting URL. The photograph will be displayed in an anonymized format (blurred or masked) in public contexts until a contact unlock transaction is completed.  **File Requirements**: - Formats: JPEG, PNG - Maximum size: 5 MB - Recommended dimensions: 400x400 pixels minimum 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.StorageApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        StorageApi apiInstance = new StorageApi(defaultClient);
        File photo = new File("/path/to/file"); // File | 
        try {
            UploadProfilePhoto200Response result = apiInstance.uploadProfilePhoto(photo);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling StorageApi#uploadProfilePhoto");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **photo** | **File**|  | |

### Return type

[**UploadProfilePhoto200Response**](UploadProfilePhoto200Response.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: multipart/form-data
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Profile photograph uploaded successfully |  -  |
| **400** | Invalid file format or size exceeds limit |  -  |

