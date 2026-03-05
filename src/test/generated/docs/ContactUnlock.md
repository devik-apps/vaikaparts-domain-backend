

# ContactUnlock

Contact information unlock transaction

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **UUID** | RFC 4122 compliant UUID |  |
|**demandId** | **UUID** | RFC 4122 compliant UUID |  |
|**offerId** | **UUID** | RFC 4122 compliant UUID |  |
|**researcherId** | **UUID** | RFC 4122 compliant UUID |  |
|**sellerId** | **UUID** | RFC 4122 compliant UUID |  |
|**researcherContact** | [**ContactUnlockResearcherContact**](ContactUnlockResearcherContact.md) |  |  [optional] |
|**sellerContact** | [**ContactUnlockSellerContact**](ContactUnlockSellerContact.md) |  |  [optional] |
|**status** | **ContactUnlockStatus** |  |  |
|**contactFee** | **Double** | Unlock fee in Malagasy Ariary |  |
|**paymentReference** | **String** | Payment Service reference identifier |  [optional] |
|**transactionId** | **String** | Mobile money transaction reference |  [optional] |
|**paidAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  [optional] |
|**confirmedAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  [optional] |
|**confirmationDeadline** | **OffsetDateTime** | ISO 8601 formatted date-time string |  [optional] |
|**createdAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  |
|**updatedAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  [optional] |



