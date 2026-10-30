# Company building bulk creation

`POST /api/v1/buildings/company/bulk` accepts a JSON array of 1 to 500 existing `BuildingRequest` objects. Authenticate using the normal API authentication. The current database user must have the `PropertyManager` role and a company. The company and property manager are taken from that user, never from the request; `id` and `managerId` must be absent or null.

On success the response is HTTP 200 and a JSON array of newly created building IDs in input order. Each building has a joined PropertyManager membership and no local building manager. The batch is transactional: any persistence failure rolls back the whole batch.

Validation errors return HTTP 400 as `{"errors":[{"row":2,"field":"floors","message":"Must be a non-negative integer"}]}`. Rows are one-based array positions, not Excel sheet numbers; row 0 denotes a whole-request error. Bean validation messages retain the existing `BuildingRequest` message codes. Malformed JSON or incompatible field types return a body-level error; they cannot reliably be associated with a row. Authorization failures return HTTP 403 in the existing API error format.

In addition to `BuildingRequest` Bean Validation, floors must be a non-negative integer; counts and finite areas/capacity must be non-negative; common area cannot exceed total area; heating type must be a `HeatingType` enum value (case-insensitive); mapped text fields are limited to 255 characters. A batch cannot contain the same street, street number, city, postal code and country after trimming and case folding. No address lookup against existing buildings is performed. Concurrent submissions are independent and may create the same address; the endpoint is not idempotent. Building codes retain the existing unique database constraint: a collision fails and rolls back the batch.

The frontend PM form is not included in this repository, so these domain rules should be compared with that form before relying on exact UI parity.
