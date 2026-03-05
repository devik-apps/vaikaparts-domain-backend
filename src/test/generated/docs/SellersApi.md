# SellersApi

All URIs are relative to *https://vaikaparts-domain-backend.onrender.com*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getCurrentSeller**](SellersApi.md#getCurrentSeller) | **GET** /sellers/me | Retrieve the currently authenticated Seller profile |
| [**getSellers**](SellersApi.md#getSellers) | **GET** /sellers | List all Seller profiles |



## getCurrentSeller

> Seller getCurrentSeller()

Retrieve the currently authenticated Seller profile

Returns the complete profile of the currently authenticated Seller including all contact details.  **Authorization**: Seller role required. 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.SellersApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        SellersApi apiInstance = new SellersApi(defaultClient);
        try {
            Seller result = apiInstance.getCurrentSeller();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling SellersApi#getCurrentSeller");
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

[**Seller**](Seller.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Seller profile retrieved successfully. |  -  |
| **401** | Missing or invalid authentication credentials |  -  |
| **403** | Authenticated user is not a Seller. |  -  |


## getSellers

> SellerPageResponse getSellers(page, size)

List all Seller profiles

Returns a paginated list of all Seller profiles in the system.  **Authorization**: Manager role required. 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.SellersApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        SellersApi apiInstance = new SellersApi(defaultClient);
        Integer page = 0; // Integer | Zero-indexed page number for pagination
        Integer size = 10; // Integer | Number of items per page
        try {
            SellerPageResponse result = apiInstance.getSellers(page, size);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling SellersApi#getSellers");
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

[**SellerPageResponse**](SellerPageResponse.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Seller list retrieved successfully. |  -  |
| **401** | Missing or invalid authentication credentials |  -  |
| **403** | Insufficient permissions for requested operation |  -  |

