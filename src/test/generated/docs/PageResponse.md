

# PageResponse

Paginated response wrapper matching the JSON serialization of org.springframework.data.domain.Page. The pagination metadata is nested under the `pageable` object, consistent with Spring Data defaults. 

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**content** | **List&lt;Object&gt;** | Page content — the list of elements for the current page. |  |
|**pageable** | [**PageResponsePageable**](PageResponsePageable.md) |  |  |
|**totalElements** | **Long** | Total number of elements across all pages. |  |
|**totalPages** | **Integer** | Total number of pages available. |  |
|**last** | **Boolean** | Whether this is the last page. |  |
|**first** | **Boolean** | Whether this is the first page. |  |
|**size** | **Integer** | Effective page size (number of elements requested per page). |  |
|**number** | **Integer** | Zero-based index of the current page. |  |
|**numberOfElements** | **Integer** | Number of elements on the current page. |  |
|**empty** | **Boolean** | Whether the current page has no content. |  |
|**sort** | [**SortDescriptor**](SortDescriptor.md) |  |  [optional] |



