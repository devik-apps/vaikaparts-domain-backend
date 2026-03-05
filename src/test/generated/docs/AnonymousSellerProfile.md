

# AnonymousSellerProfile

Anonymized seller profile visible before contact unlock

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **UUID** | RFC 4122 compliant UUID |  |
|**maskedProfilePhotoUrl** | **URI** | Blurred or placeholder profile photograph URL |  [optional] |
|**garageName** | **String** | Business name (visible only for premium sellers) |  [optional] |
|**location** | [**AnonymousSellerProfileLocation**](AnonymousSellerProfileLocation.md) |  |  |
|**averageRating** | **Float** |  |  |
|**totalRatings** | **Integer** |  |  [optional] |
|**successfulUnlocks** | **Integer** |  |  [optional] |
|**isPremium** | **Boolean** | Premium subscription status |  [optional] |
|**memberSince** | **OffsetDateTime** | ISO 8601 formatted date-time string |  |



