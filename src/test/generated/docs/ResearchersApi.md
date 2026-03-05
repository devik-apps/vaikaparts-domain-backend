# ResearchersApi

All URIs are relative to *https://vaikaparts-domain-backend.onrender.com*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getCurrentResearcher**](ResearchersApi.md#getCurrentResearcher) | **GET** /researchers/me | Retrieve the currently authenticated Researcher profile |
| [**getResearcherById**](ResearchersApi.md#getResearcherById) | **GET** /researchers/{researcherId} | Retrieve Researcher profile by ID |
| [**getResearchers**](ResearchersApi.md#getResearchers) | **GET** /researchers | List all Researcher profiles |



## getCurrentResearcher

> Researcher getCurrentResearcher()

Retrieve the currently authenticated Researcher profile

Returns the complete profile of the currently authenticated Researcher.

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.ResearchersApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        ResearchersApi apiInstance = new ResearchersApi(defaultClient);
        try {
            Researcher result = apiInstance.getCurrentResearcher();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ResearchersApi#getCurrentResearcher");
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

[**Researcher**](Researcher.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Researcher profile retrieved successfully. |  -  |
| **401** | Missing or invalid authentication credentials |  -  |
| **403** | Authenticated user is not a Researcher. |  -  |


## getResearcherById

> Researcher getResearcherById(researcherId)

Retrieve Researcher profile by ID

Returns the complete profile of a specific Researcher identified by UUID.  **Authorization**: Manager role required 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.ResearchersApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        ResearchersApi apiInstance = new ResearchersApi(defaultClient);
        UUID researcherId = UUID.randomUUID(); // UUID | Unique identifier of the Researcher
        try {
            Researcher result = apiInstance.getResearcherById(researcherId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ResearchersApi#getResearcherById");
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
| **researcherId** | **UUID**| Unique identifier of the Researcher | |

### Return type

[**Researcher**](Researcher.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Researcher profile retrieved successfully |  -  |
| **404** | Requested resource does not exist |  -  |
| **403** | Insufficient permissions for requested operation |  -  |


## getResearchers

> ResearcherPageResponse getResearchers(page, size)

List all Researcher profiles

Returns a paginated list of all Researcher profiles in the system.  **Authorization**: Manager role required. 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.ResearchersApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        ResearchersApi apiInstance = new ResearchersApi(defaultClient);
        Integer page = 0; // Integer | Zero-indexed page number for pagination
        Integer size = 10; // Integer | Number of items per page
        try {
            ResearcherPageResponse result = apiInstance.getResearchers(page, size);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ResearchersApi#getResearchers");
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

[**ResearcherPageResponse**](ResearcherPageResponse.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Researcher list retrieved successfully. |  -  |
| **401** | Missing or invalid authentication credentials |  -  |
| **403** | Insufficient permissions for requested operation |  -  |

