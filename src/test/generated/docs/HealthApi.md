# HealthApi

All URIs are relative to *https://vaikaparts-domain-backend.onrender.com*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**healthBucketCheck**](HealthApi.md#healthBucketCheck) | **GET** /health/bucket | Check bucket health by uploading, downloading, and presigning a file |
| [**healthDbGet**](HealthApi.md#healthDbGet) | **GET** /health/db | Health check for the dummy database |
| [**pong**](HealthApi.md#pong) | **GET** /ping | Check if the server is alive |
| [**sendHealthEmail**](HealthApi.md#sendHealthEmail) | **GET** /health/email | Send health check email |
| [**triggerDummyEvents**](HealthApi.md#triggerDummyEvents) | **GET** /health/message | Trigger dummy health check events |



## healthBucketCheck

> String healthBucketCheck()

Check bucket health by uploading, downloading, and presigning a file

This endpoint uploads a randomly generated text file to the storage bucket,  verifies its content by downloading it, and returns a presigned URL for the file. 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.HealthApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        HealthApi apiInstance = new HealthApi(defaultClient);
        try {
            String result = apiInstance.healthBucketCheck();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling HealthApi#healthBucketCheck");
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

**String**

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successfully uploaded, verified, and presigned file. |  -  |
| **500** | Uploaded and downloaded content mismatch or other error occurred. |  -  |


## healthDbGet

> HealthDbGet200Response healthDbGet(page, size)

Health check for the dummy database

Returns a paginated list of Dummy entities.

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.HealthApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        HealthApi apiInstance = new HealthApi(defaultClient);
        Integer page = 0; // Integer | Page number (0-based)
        Integer size = 10; // Integer | Page size
        try {
            HealthDbGet200Response result = apiInstance.healthDbGet(page, size);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling HealthApi#healthDbGet");
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
| **page** | **Integer**| Page number (0-based) | [optional] [default to 0] |
| **size** | **Integer**| Page size | [optional] [default to 10] |

### Return type

[**HealthDbGet200Response**](HealthDbGet200Response.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful database health check |  -  |


## pong

> String pong()

Check if the server is alive

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.HealthApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        HealthApi apiInstance = new HealthApi(defaultClient);
        try {
            String result = apiInstance.pong();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling HealthApi#pong");
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

**String**

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | A message showing that the server is alive |  -  |


## sendHealthEmail

> String sendHealthEmail(to)

Send health check email

Sends a test email to verify email service functionality

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.HealthApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");

        HealthApi apiInstance = new HealthApi(defaultClient);
        String to = "to_example"; // String | Email address to send the health check email to
        try {
            String result = apiInstance.sendHealthEmail(to);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling HealthApi#sendHealthEmail");
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
| **to** | **String**| Email address to send the health check email to | |

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Email sent successfully |  -  |
| **400** | Invalid email address format |  -  |


## triggerDummyEvents

> List&lt;String&gt; triggerDummyEvents(nbEvent, waitInSeconds)

Trigger dummy health check events

This endpoint triggers one or more dummy events through the event producer  for testing the messaging system (e.g., RabbitMQ or Kafka). 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.HealthApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        HealthApi apiInstance = new HealthApi(defaultClient);
        Integer nbEvent = 1; // Integer | Number of events to trigger (1–500)
        Integer waitInSeconds = 2; // Integer | Duration (in seconds) to simulate event processing
        try {
            List<String> result = apiInstance.triggerDummyEvents(nbEvent, waitInSeconds);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling HealthApi#triggerDummyEvents");
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
| **nbEvent** | **Integer**| Number of events to trigger (1–500) | [optional] [default to 1] |
| **waitInSeconds** | **Integer**| Duration (in seconds) to simulate event processing | [optional] [default to 2] |

### Return type

**List&lt;String&gt;**

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | List of UUIDs corresponding to triggered dummy events |  -  |
| **400** | Invalid parameter values (e.g., nbEvent not in 1–500) |  -  |

