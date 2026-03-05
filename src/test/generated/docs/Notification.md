

# Notification

User notification record

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **UUID** | RFC 4122 compliant UUID |  |
|**seller** | [**Seller**](Seller.md) |  |  [optional] |
|**notificationRequestedId** | **String** | Notification event id |  [optional] |
|**message** | **String** | Notification body text |  |
|**demand** | [**Demand**](Demand.md) |  |  [optional] |
|**notificationType** | **NotificationType** |  |  [optional] |
|**read** | **Boolean** |  |  [optional] |
|**createdAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  [optional] |
|**clickAction** | **Object** |  |  [optional] |
|**readAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  [optional] |



