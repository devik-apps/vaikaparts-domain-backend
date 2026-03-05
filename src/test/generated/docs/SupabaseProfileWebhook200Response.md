

# SupabaseProfileWebhook200Response


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**message** | **String** |  |  [optional] |
|**userId** | **UUID** | The VaikaParts user ID (same as Supabase profile ID) |  [optional] |
|**eventType** | [**EventTypeEnum**](#EventTypeEnum) |  |  [optional] |



## Enum: EventTypeEnum

| Name | Value |
|---- | -----|
| INSERT | &quot;INSERT&quot; |
| UPDATE | &quot;UPDATE&quot; |
| DELETE | &quot;DELETE&quot; |



