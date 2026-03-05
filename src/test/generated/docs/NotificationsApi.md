# NotificationsApi

All URIs are relative to *https://vaikaparts-domain-backend.onrender.com*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**fetchNotifications**](NotificationsApi.md#fetchNotifications) | **GET** /v1/notifications | Retrieve seller notifications |
| [**getNotification**](NotificationsApi.md#getNotification) | **GET** /v1/notifications/{notificationId} | Get notification with given id |
| [**markAsRead**](NotificationsApi.md#markAsRead) | **PATCH** /v1/notifications/mark-as-read/{notificationId} | Mark notification as read |



## fetchNotifications

> NotificationPageResponse fetchNotifications(page, size)

Retrieve seller notifications

Returns a paginated list of notifications for the current active seller.

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.NotificationsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        NotificationsApi apiInstance = new NotificationsApi(defaultClient);
        Integer page = 0; // Integer | Zero-indexed page number for pagination
        Integer size = 10; // Integer | Number of items per page
        try {
            NotificationPageResponse result = apiInstance.fetchNotifications(page, size);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling NotificationsApi#fetchNotifications");
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

[**NotificationPageResponse**](NotificationPageResponse.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Notifications retrieved successfully |  -  |


## getNotification

> Notification getNotification(notificationId)

Get notification with given id

This endpoints allows sellers to fetch the notifications object with the given id.

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.NotificationsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        NotificationsApi apiInstance = new NotificationsApi(defaultClient);
        UUID notificationId = UUID.randomUUID(); // UUID | Unique identifier of the notification
        try {
            Notification result = apiInstance.getNotification(notificationId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling NotificationsApi#getNotification");
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
| **notificationId** | **UUID**| Unique identifier of the notification | |

### Return type

[**Notification**](Notification.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: appliction/json, application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Fetched notification with the corresponding given id |  -  |
| **404** | Requested resource does not exist |  -  |


## markAsRead

> Notification markAsRead(notificationId)

Mark notification as read

Updates the read status of a specific notification.

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.NotificationsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure HTTP bearer authorization: supabaseAuth
        HttpBearerAuth supabaseAuth = (HttpBearerAuth) defaultClient.getAuthentication("supabaseAuth");
        supabaseAuth.setBearerToken("BEARER TOKEN");

        NotificationsApi apiInstance = new NotificationsApi(defaultClient);
        UUID notificationId = UUID.randomUUID(); // UUID | Unique identifier of the notification
        try {
            Notification result = apiInstance.markAsRead(notificationId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling NotificationsApi#markAsRead");
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
| **notificationId** | **UUID**| Unique identifier of the notification | |

### Return type

[**Notification**](Notification.md)

### Authorization

[supabaseAuth](../README.md#supabaseAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: appliction/json, application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **202** | Notification marked as read |  -  |
| **404** | Requested resource does not exist |  -  |

