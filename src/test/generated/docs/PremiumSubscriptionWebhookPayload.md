

# PremiumSubscriptionWebhookPayload

Payload structure for premium subscription payment webhooks

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**sellerId** | **UUID** | RFC 4122 compliant UUID |  |
|**subscriptionId** | **String** | Payment Service subscription identifier |  |
|**plan** | **PremiumPlan** |  |  |
|**amount** | **Double** |  |  [optional] |
|**status** | [**StatusEnum**](#StatusEnum) |  |  |
|**paidAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  |
|**expiresAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  [optional] |



## Enum: StatusEnum

| Name | Value |
|---- | -----|
| ACTIVATED | &quot;ACTIVATED&quot; |
| RENEWED | &quot;RENEWED&quot; |
| CANCELLED | &quot;CANCELLED&quot; |



