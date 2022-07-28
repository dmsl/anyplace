# DOCUMENTATION

## 1. Generating OpenAPI docs:

#### 1.1 Auto-generation using: **sbt-swagger-play**

- See: [conf/api.routes](../conf/api.routes)  
  
  The route file must follow a **specific** convention for
  automatically generating the documentation.

  It uses [conf/swagger.yml](../../conf/swagger.yml) as a `YAML` base file.  


- The generated file will be placed at:
  `target/swagger/swagger.json`


- and it will be mounted at:
  `assets/swagger.json`

#### 1.2 Manual generation:  
If there issues while generating the `swagger` documentation, run in the `sbt shell`:
```bash
swagger
```

- **READ MORE**: [iheartradio/play-swagger](https://github.com/iheartradio/play-swagger)

## 2. Rendering the docs:

[Swagger-UI](https://swagger.io/tools/swagger-ui/) is used to render the
generated `swagger.json` file. it follows the OpenAPI convention.
