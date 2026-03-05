

# Offer

Seller response to a Researcher demand.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **UUID** | RFC 4122 compliant UUID |  |
|**description** | **String** | Part condition details and additional information. |  [optional] |
|**attachedPhotosUrls** | **List&lt;URI&gt;** | Photographs of the actual part. |  [optional] |
|**sellerId** | **UUID** | RFC 4122 compliant UUID |  |
|**partsInfo** | [**PartInfo**](PartInfo.md) |  |  |
|**demand** | [**Demand**](Demand.md) |  |  |
|**status** | **OfferStatus** |  |  |
|**createdAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  |
|**updatedAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  [optional] |
|**canceledAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  [optional] |
|**suspendedAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  [optional] |



