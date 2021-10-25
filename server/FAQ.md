# FAQ:

### A. Documentation
Documentation is automatically generated using `iheart-radio/swagger`.
The additions are in `conf/swagger.yml` and `conf/api.routes`

##### A1. Swagger documentation not generated correctly:
Try the sbt `swagger` command that the `iheart/sbt-swagger-play` plugin adds.

##### A2. An endpoint on `/developers` does not get the response.
<details close>
<summary>
Reply example in swagger
</summary>

Either it's a bug or it's missing the response tag.
Update the relevant `api.routes` entry with:
```bash
#      responses:
#        '200':
#          description: Successful operation
```
See other examples on how to put a sample response.
</details>

##### A3. How to put a reply example in swagger:
<details close>
<summary>
Reply example in swagger
</summary>

Way 1:
```bash
#     examples:
#       application/json: |
#            {
#               "all_floors": [
#                   "<floor1 BASE64>",
#                   "<floor2 BASE64>"
#               ]
#            }
```

Way 2:
```bash
#  responses:
#    200:
#      description: success
#      schema:
#        $ref: '#/definitions/Version'
```
This requires a definition in `conf/swagger.yml`, under `definitions:`.
</details>

