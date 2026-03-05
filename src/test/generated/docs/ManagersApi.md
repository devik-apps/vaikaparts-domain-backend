# ManagersApi

All URIs are relative to *https://vaikaparts-domain-backend.onrender.com*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getCurrentManager**](ManagersApi.md#getCurrentManager) | **GET** /managers/me | Retrieve the currently authenticated Manager profile |
| [**getManagers**](ManagersApi.md#getManagers) | **GET** /managers | List all Manager profiles |



## getCurrentManager

> Manager getCurrentManager()

Retrieve the currently authenticated Manager profile

Returns the complete profile of the currently authenticated Manager.  **Authorization**: Manager role required. 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.ManagersApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        ManagersApi apiInstance = new ManagersApi(defaultClient);
        try {
            Manager result = apiInstance.getCurrentManager();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ManagersApi#getCurrentManager");
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

[**Manager**](Manager.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Manager profile retrieved successfully. |  -  |
| **401** | Missing or invalid authentication credentials |  -  |
| **403** | Authenticated user is not a Manager. |  -  |


## getManagers

> ManagerPageResponse getManagers(page, size)

List all Manager profiles

Returns a paginated list of all Manager profiles in the system.  **Authorization**: Manager role required. 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.ManagersApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        ManagersApi apiInstance = new ManagersApi(defaultClient);
        Integer page = 0; // Integer | Zero-indexed page number for pagination
        Integer size = 10; // Integer | Number of items per page
        try {
            ManagerPageResponse result = apiInstance.getManagers(page, size);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ManagersApi#getManagers");
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
| **page** | **Integer**| Zero-indexed page number for pagination | [optional] [default to 0] |
| **size** | **Integer**| Number of items per page | [optional] [default to 10] |

### Return type

[**ManagerPageResponse**](ManagerPageResponse.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Manager list retrieved successfully. |  -  |
| **401** | Missing or invalid authentication credentials |  -  |
| **403** | Insufficient permissions for requested operation |  -  |

