# DemandsApi

All URIs are relative to *https://vaikaparts-domain-backend.onrender.com*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createDemand**](DemandsApi.md#createDemand) | **POST** /v1/demands | Create a new automotive parts demand |
| [**getDemandById**](DemandsApi.md#getDemandById) | **GET** /v1/demands/{demandId} | Retrieve a demand by its identifier |
| [**getOffersForDemand**](DemandsApi.md#getOffersForDemand) | **GET** /v1/demands/{demandId}/offers | Retrieve paginated offers submitted for a specific demand |
| [**getResearcherDemands**](DemandsApi.md#getResearcherDemands) | **GET** /v1/demands | List researcher demands with optional status filtering |
| [**updateDemandStatus**](DemandsApi.md#updateDemandStatus) | **PATCH** /v1/demands/{demandId}/status | Update the lifecycle status of a demand |



## createDemand

> Demand createDemand(description, part)

Create a new automotive parts demand

Creates a new demand on behalf of the authenticated Researcher. The request is submitted as &#x60;multipart/form-data&#x60;. Part fields are bound using Spring dot-notation (&#x60;part.name&#x60;, &#x60;part.carBrand&#x60;, etc.). Images are optional and bound under &#x60;part.images&#x60;; maximum 5 files per demand.  **Authorization**: Researcher role required. 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.DemandsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        DemandsApi apiInstance = new DemandsApi(defaultClient);
        String description = "description_example"; // String | Additional context or specifications for the sought part.
        PartCreateRequest part = new PartCreateRequest(); // PartCreateRequest | 
        try {
            Demand result = apiInstance.createDemand(description, part);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling DemandsApi#createDemand");
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
| **description** | **String**| Additional context or specifications for the sought part. | |
| **part** | [**PartCreateRequest**](PartCreateRequest.md)|  | |

### Return type

[**Demand**](Demand.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: multipart/form-data
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Demand created successfully. |  -  |
| **400** | Invalid request parameters or payload |  -  |
| **401** | Missing or invalid authentication credentials |  -  |
| **403** | Insufficient permissions for requested operation |  -  |


## getDemandById

> Demand getDemandById(demandId)

Retrieve a demand by its identifier

Returns the full detail view for a single demand. Access is restricted to the owning Researcher — requests for demands belonging to another principal return 404 to prevent information disclosure.  **Authorization**: Researcher role required. 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.DemandsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        DemandsApi apiInstance = new DemandsApi(defaultClient);
        String demandId = "demandId_example"; // String | Unique identifier of the demand.
        try {
            Demand result = apiInstance.getDemandById(demandId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling DemandsApi#getDemandById");
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
| **demandId** | **String**| Unique identifier of the demand. | |

### Return type

[**Demand**](Demand.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Demand retrieved successfully. |  -  |
| **401** | Missing or invalid authentication credentials |  -  |
| **404** | Requested resource does not exist |  -  |


## getOffersForDemand

> OfferPageResponse getOffersForDemand(demandId, page, size)

Retrieve paginated offers submitted for a specific demand

Returns a paginated list of all offers submitted in response to the specified demand. Access is restricted to the demand owner.  **Authorization**: Researcher role required. 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.DemandsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        DemandsApi apiInstance = new DemandsApi(defaultClient);
        String demandId = "demandId_example"; // String | Unique identifier of the demand.
        Integer page = 0; // Integer | Zero-indexed page number for pagination
        Integer size = 10; // Integer | Number of items per page
        try {
            OfferPageResponse result = apiInstance.getOffersForDemand(demandId, page, size);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling DemandsApi#getOffersForDemand");
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
| **demandId** | **String**| Unique identifier of the demand. | |
| **page** | **Integer**| Zero-indexed page number for pagination | [optional] [default to 0] |
| **size** | **Integer**| Number of items per page | [optional] [default to 10] |

### Return type

[**OfferPageResponse**](OfferPageResponse.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Offer list retrieved successfully. |  -  |
| **401** | Missing or invalid authentication credentials |  -  |
| **403** | Insufficient permissions for requested operation |  -  |
| **404** | Requested resource does not exist |  -  |


## getResearcherDemands

> DemandPageResponse getResearcherDemands(page, size, status)

List researcher demands with optional status filtering

Returns a paginated list of demands belonging to the authenticated Researcher. Results may be narrowed by lifecycle status.  **Authorization**: Researcher role required. 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.DemandsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        DemandsApi apiInstance = new DemandsApi(defaultClient);
        Integer page = 0; // Integer | Zero-indexed page number for pagination
        Integer size = 10; // Integer | Number of items per page
        DemandStatus status = DemandStatus.fromValue("DRAFT"); // DemandStatus | Filter by demand lifecycle status. Omit to return all statuses.
        try {
            DemandPageResponse result = apiInstance.getResearcherDemands(page, size, status);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling DemandsApi#getResearcherDemands");
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
| **status** | [**DemandStatus**](.md)| Filter by demand lifecycle status. Omit to return all statuses. | [optional] [enum: DRAFT, PUBLISHED, SUSPENDED, PENDING, CANCELED] |

### Return type

[**DemandPageResponse**](DemandPageResponse.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Demand list retrieved successfully. |  -  |
| **401** | Missing or invalid authentication credentials |  -  |
| **403** | Insufficient permissions for requested operation |  -  |


## updateDemandStatus

> Demand updateDemandStatus(demandId, status)

Update the lifecycle status of a demand

Transitions the demand to a new lifecycle status. Only permitted status transitions are accepted; invalid transitions return 400.  **Authorization**: Researcher role required. 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.DemandsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        DemandsApi apiInstance = new DemandsApi(defaultClient);
        String demandId = "demandId_example"; // String | Unique identifier of the demand.
        DemandStatus status = DemandStatus.fromValue("DRAFT"); // DemandStatus | Target lifecycle status to apply to the demand.
        try {
            Demand result = apiInstance.updateDemandStatus(demandId, status);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling DemandsApi#updateDemandStatus");
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
| **demandId** | **String**| Unique identifier of the demand. | |
| **status** | [**DemandStatus**](.md)| Target lifecycle status to apply to the demand. | [enum: DRAFT, PUBLISHED, SUSPENDED, PENDING, CANCELED] |

### Return type

[**Demand**](Demand.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Demand status updated successfully. |  -  |
| **400** | Invalid request parameters or payload |  -  |
| **401** | Missing or invalid authentication credentials |  -  |
| **403** | Insufficient permissions for requested operation |  -  |
| **404** | Requested resource does not exist |  -  |

