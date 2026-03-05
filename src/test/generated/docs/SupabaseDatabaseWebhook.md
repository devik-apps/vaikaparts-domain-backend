

# SupabaseDatabaseWebhook

Webhook payload from Supabase Database Webhooks

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**type** | [**TypeEnum**](#TypeEnum) | The type of database operation that triggered the webhook |  |
|**table** | **String** | The table name (always &#39;profiles&#39; for this webhook) |  |
|**schema** | **String** | The schema name (always &#39;public&#39; for this webhook) |  |
|**record** | [**SupabaseProfileRecord**](SupabaseProfileRecord.md) | The new/current record data (null for DELETE operations) |  |
|**oldRecord** | [**SupabaseProfileRecord**](SupabaseProfileRecord.md) | The previous record data (null for INSERT operations, present for UPDATE and DELETE) |  |



## Enum: TypeEnum

| Name | Value |
|---- | -----|
| INSERT | &quot;INSERT&quot; |
| UPDATE | &quot;UPDATE&quot; |
| DELETE | &quot;DELETE&quot; |



