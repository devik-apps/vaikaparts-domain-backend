

# GetCurrentUser200Response


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **UUID** | RFC 4122 compliant UUID |  |
|**supabaseUserId** | **String** | Supabase authentication system user identifier |  |
|**name** | **String** | Full name of the user |  |
|**phoneNumber** | **String** | E.164 formatted international phone number |  |
|**email** | **String** | RFC 5322 compliant email address |  [optional] |
|**profileImgUrl** | **URI** | S3 object URL for profile photograph |  [optional] |
|**userType** | **UserType** |  |  |
|**status** | **UserStatus** |  |  |
|**createdAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  |
|**updatedAt** | **OffsetDateTime** | ISO 8601 formatted date-time string |  [optional] |
|**location** | [**Location**](Location.md) |  |  |
|**garageName** | **String** | Business or garage name |  [optional] |
|**latLon** | [**LatLon**](LatLon.md) |  |  [optional] |
|**role** | **ManagerRole** |  |  |



