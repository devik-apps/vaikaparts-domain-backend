

# ErrorResponse

Standard error response format

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**timestamp** | **OffsetDateTime** | ISO 8601 formatted date-time string |  |
|**status** | **Integer** | HTTP status code |  |
|**error** | **String** | HTTP status reason phrase |  |
|**message** | **String** | Human-readable error description |  |
|**path** | **String** | Request path that generated the error |  |
|**errorCode** | [**ErrorCodeEnum**](#ErrorCodeEnum) | Application-specific error code |  [optional] |
|**details** | **Object** | Additional error context |  [optional] |



## Enum: ErrorCodeEnum

| Name | Value |
|---- | -----|
| AUTHORIZATION_DENIED | &quot;AUTHORIZATION_DENIED&quot; |
| ENTITY_NOT_FOUND | &quot;ENTITY_NOT_FOUND&quot; |
| CONSTRAINT_VIOLATION_ON_FIELDS | &quot;CONSTRAINT_VIOLATION_ON_FIELDS&quot; |
| DUPLICATE_ENTITY | &quot;DUPLICATE_ENTITY&quot; |
| BAD_FORM | &quot;BAD_FORM&quot; |
| MISSING_AUTHORIZATION | &quot;MISSING_AUTHORIZATION&quot; |
| INVALID_AUTHORIZATION_FORMAT | &quot;INVALID_AUTHORIZATION_FORMAT&quot; |
| INVALID_TOKEN | &quot;INVALID_TOKEN&quot; |
| TOKEN_NOT_FOUND | &quot;TOKEN_NOT_FOUND&quot; |
| USER_DEACTIVATED | &quot;USER_DEACTIVATED&quot; |
| AUTHENTICATION_FAILED | &quot;AUTHENTICATION_FAILED&quot; |
| INTERNAL_SERVER_ERROR | &quot;INTERNAL_SERVER_ERROR&quot; |
| MISSING_REQUIRED_PARAMETER | &quot;MISSING_REQUIRED_PARAMETER&quot; |
| INVALID_PARAMETER | &quot;INVALID_PARAMETER&quot; |
| MALFORMED_JSON | &quot;MALFORMED_JSON&quot; |
| METHOD_NOT_ALLOWED | &quot;METHOD_NOT_ALLOWED&quot; |
| ALREADY_UNLOCKED | &quot;ALREADY_UNLOCKED&quot; |
| UNLOCK_NOT_PAID | &quot;UNLOCK_NOT_PAID&quot; |
| REFUND_NOT_ELIGIBLE | &quot;REFUND_NOT_ELIGIBLE&quot; |
| ONLY_RESEARCHER_S_ALLOWED | &quot;ONLY_ResearcherS_ALLOWED&quot; |
| ONLY_SELLERS_ALLOWED | &quot;ONLY_SELLERS_ALLOWED&quot; |
| ONLY_MANAGERS_ALLOWED | &quot;ONLY_MANAGERS_ALLOWED&quot; |



