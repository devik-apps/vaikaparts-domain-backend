

# SupabaseProfileRecord

Profile record from public.profiles table

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **UUID** | Profile UUID (references auth.users.id) |  |
|**email** | **String** | User email address |  |
|**phoneNumber** | **String** | User phone number |  [optional] |
|**name** | **String** | User display name |  [optional] |
|**profileImgUrl** | **URI** | URL to user&#39;s profile image |  [optional] |
|**userMetadata** | [**SupabaseProfileRecordUserMetadata**](SupabaseProfileRecordUserMetadata.md) |  |  |
|**appMetadata** | **Object** | Application metadata |  [optional] |
|**createdAt** | **OffsetDateTime** | Profile creation timestamp |  |
|**updatedAt** | **OffsetDateTime** | Profile last update timestamp |  |
|**deletedAt** | **OffsetDateTime** | Soft delete timestamp (null if not deleted) |  [optional] |



