

# PartInfoCreateRequest

Part information submitted as part of the offer creation multipart request. Fields are bound using Spring dot-notation: `part_info.name`, `part_info.car_brand`, etc. 

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**name** | **String** | Descriptive name of the part. |  |
|**carBrand** | **String** | Vehicle manufacturer. |  |
|**carModel** | **String** | Vehicle model designation. |  |
|**carYear** | **Integer** | Four-digit model year of the vehicle. |  |
|**partCategory** | **PartCategory** |  |  |
|**condition** | **PartCondition** |  |  |
|**price** | **Double** | Offered price in Malagasy Ariary. |  |
|**images** | **List&lt;File&gt;** | Photographs of the offered part. Bound under the multipart field name &#x60;part_info.images&#x60;. Optional; maximum 5 files.  |  [optional] |



