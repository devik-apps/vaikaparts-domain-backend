

# PartCreateRequest

Part details submitted as part of the demand creation multipart request. Fields are bound using Spring dot-notation: `part.name`, `part.car_brand`, etc. 

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**name** | **String** | Descriptive name of the part. |  |
|**carBrand** | **String** | Vehicle manufacturer. |  |
|**carModel** | **String** | Vehicle model designation. |  |
|**carYear** | **Integer** | Four-digit model year of the vehicle. |  |
|**partCategory** | **PartCategory** |  |  |
|**images** | **List&lt;File&gt;** | Reference photographs of the sought part. Bound under the multipart field name &#x60;part.images&#x60;. Optional; maximum 5 files.  |  [optional] |



