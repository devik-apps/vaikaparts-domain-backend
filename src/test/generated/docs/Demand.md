

# Demand

A Researcher's request for a specific automotive part.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **UUID** | RFC 4122 compliant UUID |  |
|**description** | **String** | Additional context or specifications provided by the Researcher. |  [optional] |
|**attachedPhotosUrls** | **List&lt;URI&gt;** | URLs of photographs attached to the demand. |  [optional] |
|**researcher** | [**Researcher**](Researcher.md) |  |  |
|**part** | [**Part**](Part.md) |  |  |
|**status** | **DemandStatus** |  |  |
|**createdAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  |
|**updatedAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  [optional] |
|**canceledAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  [optional] |
|**suspendedAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  [optional] |



