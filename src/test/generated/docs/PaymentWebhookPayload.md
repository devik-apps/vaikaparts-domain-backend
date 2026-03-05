

# PaymentWebhookPayload

Payload structure for Payment Service transaction webhooks

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**unlockId** | **UUID** | RFC 4122 compliant UUID |  |
|**paymentId** | **String** | Payment Service payment identifier |  |
|**transactionId** | **String** | Mobile money provider transaction reference |  |
|**amount** | **Double** | Transaction amount in MGA |  |
|**paymentMethod** | [**PaymentMethodEnum**](#PaymentMethodEnum) |  |  [optional] |
|**status** | [**StatusEnum**](#StatusEnum) |  |  |
|**paidAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  [optional] |



## Enum: PaymentMethodEnum

| Name | Value |
|---- | -----|
| ORANGE_MONEY | &quot;ORANGE_MONEY&quot; |
| MVOLA | &quot;MVOLA&quot; |
| AIRTEL_MONEY | &quot;AIRTEL_MONEY&quot; |



## Enum: StatusEnum

| Name | Value |
|---- | -----|
| SUCCESS | &quot;SUCCESS&quot; |
| FAILED | &quot;FAILED&quot; |



