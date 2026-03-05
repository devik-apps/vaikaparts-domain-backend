

# PremiumSubscription

Seller premium subscription details

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **UUID** | RFC 4122 compliant UUID |  [optional] |
|**sellerId** | **UUID** | RFC 4122 compliant UUID |  [optional] |
|**plan** | **PremiumPlan** |  |  [optional] |
|**status** | [**StatusEnum**](#StatusEnum) |  |  [optional] |
|**paymentReference** | **String** | Payment Service reference identifier |  [optional] |
|**features** | [**PremiumSubscriptionFeatures**](PremiumSubscriptionFeatures.md) |  |  [optional] |
|**activatedAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  [optional] |
|**expiresAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  [optional] |
|**autoRenew** | **Boolean** |  |  [optional] |



## Enum: StatusEnum

| Name | Value |
|---- | -----|
| ACTIVE | &quot;ACTIVE&quot; |
| EXPIRED | &quot;EXPIRED&quot; |
| CANCELLED | &quot;CANCELLED&quot; |



