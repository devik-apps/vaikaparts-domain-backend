# OffersApi

All URIs are relative to *https://vaikaparts-domain-backend.onrender.com*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createOffer**](OffersApi.md#createOffer) | **POST** /v1/offers | Submit a new offer in response to a demand |
| [**getOfferById**](OffersApi.md#getOfferById) | **GET** /v1/offers/{offerId} | Retrieve an offer by its identifier |
| [**getOffersByDemandId**](OffersApi.md#getOffersByDemandId) | **GET** /v1/offers/demand/{demandId} | Retrieve paginated offers associated with a specific demand |
| [**getOffersForDemand**](OffersApi.md#getOffersForDemand) | **GET** /v1/demands/{demandId}/offers | Retrieve paginated offers submitted for a specific demand |
| [**getSellerOffers**](OffersApi.md#getSellerOffers) | **GET** /v1/offers | List offers submitted by the authenticated seller |
| [**updateOfferStatus**](OffersApi.md#updateOfferStatus) | **PATCH** /v1/offers/{offerId}/status | Update the lifecycle status of an offer |



## createOffer

> Offer createOffer(demandId, description, partInfo)

Submit a new offer in response to a demand

Creates a new offer on behalf of the authenticated seller. The request is submitted as &#x60;multipart/form-data&#x60;. Part info fields are bound using Spring dot-notation (&#x60;part_info.name&#x60;, &#x60;part_info.car_brand&#x60;, etc.). Images are optional and bound under &#x60;part_info.images&#x60;; maximum 5 files per offer.  **Authorization**: Seller role required. 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.OffersApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        OffersApi apiInstance = new OffersApi(defaultClient);
        String demandId = "demandId_example"; // String | Identifier of the demand this offer responds to.
        String description = "description_example"; // String | Additional context or condition details provided by the seller.
        PartInfoCreateRequest partInfo = new PartInfoCreateRequest(); // PartInfoCreateRequest | 
        try {
            Offer result = apiInstance.createOffer(demandId, description, partInfo);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling OffersApi#createOffer");
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
| **demandId** | **String**| Identifier of the demand this offer responds to. | |
| **description** | **String**| Additional context or condition details provided by the seller. | |
| **partInfo** | [**PartInfoCreateRequest**](PartInfoCreateRequest.md)|  | |

### Return type

[**Offer**](Offer.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: multipart/form-data
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Offer created successfully. |  -  |
| **400** | Invalid request parameters or payload |  -  |
| **401** | Missing or invalid authentication credentials |  -  |
| **403** | Insufficient permissions for requested operation |  -  |
| **404** | Demand not found. |  -  |


## getOfferById

> Offer getOfferById(offerId)

Retrieve an offer by its identifier

Returns the full detail view for a single offer. Access is restricted to the owning seller — requests for offers belonging to another principal return 404 to prevent information disclosure.  **Authorization**: Seller role required. 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.OffersApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        OffersApi apiInstance = new OffersApi(defaultClient);
        String offerId = "offerId_example"; // String | Unique identifier of the offer.
        try {
            Offer result = apiInstance.getOfferById(offerId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling OffersApi#getOfferById");
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
| **offerId** | **String**| Unique identifier of the offer. | |

### Return type

[**Offer**](Offer.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Offer retrieved successfully. |  -  |
| **401** | Missing or invalid authentication credentials |  -  |
| **404** | Requested resource does not exist |  -  |


## getOffersByDemandId

> OfferPageResponse getOffersByDemandId(demandId, page, size)

Retrieve paginated offers associated with a specific demand

Returns a paginated list of offers submitted in response to the specified demand, as visible to the authenticated seller.  **Authorization**: Seller role required. 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.OffersApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        OffersApi apiInstance = new OffersApi(defaultClient);
        String demandId = "demandId_example"; // String | Unique identifier of the demand.
        Integer page = 0; // Integer | Zero-indexed page number for pagination
        Integer size = 10; // Integer | Number of items per page
        try {
            OfferPageResponse result = apiInstance.getOffersByDemandId(demandId, page, size);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling OffersApi#getOffersByDemandId");
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
import com.devikapps.vaikaparts.client.api.OffersApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        OffersApi apiInstance = new OffersApi(defaultClient);
        String demandId = "demandId_example"; // String | Unique identifier of the demand.
        Integer page = 0; // Integer | Zero-indexed page number for pagination
        Integer size = 10; // Integer | Number of items per page
        try {
            OfferPageResponse result = apiInstance.getOffersForDemand(demandId, page, size);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling OffersApi#getOffersForDemand");
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


## getSellerOffers

> OfferPageResponse getSellerOffers(page, size, status)

List offers submitted by the authenticated seller

Returns a paginated list of offers submitted by the currently authenticated seller. Results may be narrowed by lifecycle status.  **Authorization**: Seller role required. 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.OffersApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        OffersApi apiInstance = new OffersApi(defaultClient);
        Integer page = 0; // Integer | Zero-indexed page number for pagination
        Integer size = 10; // Integer | Number of items per page
        OfferStatus status = OfferStatus.fromValue("PENDING"); // OfferStatus | Filter by offer lifecycle status. Omit to return all statuses.
        try {
            OfferPageResponse result = apiInstance.getSellerOffers(page, size, status);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling OffersApi#getSellerOffers");
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
| **status** | [**OfferStatus**](.md)| Filter by offer lifecycle status. Omit to return all statuses. | [optional] [enum: PENDING, SELECTED, REJECTED, WITHDRAWN] |

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


## updateOfferStatus

> Offer updateOfferStatus(offerId, status)

Update the lifecycle status of an offer

Transitions the offer to a new lifecycle status. Only permitted status transitions are accepted; invalid transitions return 400.  **Authorization**: Seller role required. 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.OffersApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        OffersApi apiInstance = new OffersApi(defaultClient);
        String offerId = "offerId_example"; // String | Unique identifier of the offer.
        OfferStatus status = OfferStatus.fromValue("PENDING"); // OfferStatus | Target lifecycle status to apply to the offer.
        try {
            Offer result = apiInstance.updateOfferStatus(offerId, status);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling OffersApi#updateOfferStatus");
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
| **offerId** | **String**| Unique identifier of the offer. | |
| **status** | [**OfferStatus**](.md)| Target lifecycle status to apply to the offer. | [enum: PENDING, SELECTED, REJECTED, WITHDRAWN] |

### Return type

[**Offer**](Offer.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Offer status updated successfully. |  -  |
| **400** | Invalid request parameters or payload |  -  |
| **401** | Missing or invalid authentication credentials |  -  |
| **403** | Insufficient permissions for requested operation |  -  |
| **404** | Requested resource does not exist |  -  |

