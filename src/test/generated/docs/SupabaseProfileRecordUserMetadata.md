

# SupabaseProfileRecordUserMetadata

Custom user metadata containing user-type-specific fields

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**userType** | [**UserTypeEnum**](#UserTypeEnum) | Determines which user entity type to create |  |
|**location** | [**SupabaseProfileRecordUserMetadataLocation**](SupabaseProfileRecordUserMetadataLocation.md) |  |  [optional] |
|**latLon** | [**SupabaseProfileRecordUserMetadataLatLon**](SupabaseProfileRecordUserMetadataLatLon.md) |  |  [optional] |
|**garageName** | **String** | Garage name (for SELLER only) |  [optional] |
|**managerRole** | [**ManagerRoleEnum**](#ManagerRoleEnum) | Manager role (for MANAGER only) |  [optional] |



## Enum: UserTypeEnum

| Name | Value |
|---- | -----|
| RESEARCHER | &quot;RESEARCHER&quot; |
| SELLER | &quot;SELLER&quot; |
| MANAGER | &quot;MANAGER&quot; |



## Enum: ManagerRoleEnum

| Name | Value |
|---- | -----|
| ADMIN | &quot;ADMIN&quot; |
| SUPERVISOR | &quot;SUPERVISOR&quot; |
| STAFF | &quot;STAFF&quot; |



