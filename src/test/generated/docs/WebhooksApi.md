# WebhooksApi

All URIs are relative to *https://vaikaparts-domain-backend.onrender.com*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**supabaseProfileWebhook**](WebhooksApi.md#supabaseProfileWebhook) | **POST** /v1/webhooks/spb/auth | Supabase Database webhook handler for profiles table |



## supabaseProfileWebhook

> SupabaseProfileWebhook200Response supabaseProfileWebhook(supabaseDatabaseWebhook)

Supabase Database webhook handler for profiles table

Receives database lifecycle events from Supabase Database Webhooks and synchronizes user profile data with the VaikaParts domain model.  This endpoint handles all Supabase Database webhook events for the public.profiles table: - **INSERT**: Creates a new Researcher, Seller, or Manager profile based on user_metadata.user_type - **UPDATE**: Synchronizes metadata changes to the existing user profile - **DELETE**: Soft-deletes the user profile by setting status to DISABLED  The user type (RESEARCHER, SELLER, MANAGER) is determined from the record.user_metadata.user_type field. If not specified, defaults to RESEARCHER.  **Authentication**: Requests must include a valid Supabase webhook signature or secret token in the Authorization header. 

### Example

```java
// Import classes:
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.invoker.ApiException;
import com.devikapps.vaikaparts.client.invoker.Configuration;
import com.devikapps.vaikaparts.client.invoker.auth.*;
import com.devikapps.vaikaparts.client.invoker.models.*;
import com.devikapps.vaikaparts.client.api.WebhooksApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://vaikaparts-domain-backend.onrender.com");
        
        // Configure API key authorization: supabaseWebhookSignature
        ApiKeyAuth supabaseWebhookSignature = (ApiKeyAuth) defaultClient.getAuthentication("supabaseWebhookSignature");
        supabaseWebhookSignature.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //supabaseWebhookSignature.setApiKeyPrefix("Token");

        WebhooksApi apiInstance = new WebhooksApi(defaultClient);
        SupabaseDatabaseWebhook supabaseDatabaseWebhook = new SupabaseDatabaseWebhook(); // SupabaseDatabaseWebhook | 
        try {
            SupabaseProfileWebhook200Response result = apiInstance.supabaseProfileWebhook(supabaseDatabaseWebhook);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling WebhooksApi#supabaseProfileWebhook");
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
| **supabaseDatabaseWebhook** | [**SupabaseDatabaseWebhook**](SupabaseDatabaseWebhook.md)|  | |

### Return type

[**SupabaseProfileWebhook200Response**](SupabaseProfileWebhook200Response.md)

### Authorization

[supabaseWebhookSignature](../README.md#supabaseWebhookSignature)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Webhook processed successfully |  -  |
| **400** | Invalid request parameters or payload |  -  |
| **401** | Invalid webhook signature or authorization token |  -  |
| **500** | Internal server error during webhook processing |  -  |

