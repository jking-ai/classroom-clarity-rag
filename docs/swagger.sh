#!/bin/bash
docker run -p 9999:8080 -e SWAGGER_JSON=/app/openapi.yaml -v $PWD/openapi.yaml:/app/openapi.yaml docker.swagger.io/swaggerapi/swagger-ui