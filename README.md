# README

## Run

```bash
$ GOOGLE_APPLICATION_CREDENTIALS=${API_KEY}.json SLIDE4VR_PPTX2PNG_URL=${API_URL} ./mvnw compile quarkus:dev
```

## deploy proxy

First, `endpoints-runtime-serverless` should be delopyed.

```bash
$ gcloud run deploy endpoint-slide4vr-api \
    --image="gcr.io/endpoints-release/endpoints-runtime-serverless:2" \
    --allow-unauthenticated \
    --platform managed 
```

Next, open-api file should be updated and made integration build for add application data to ESD. 
Finally, endpoint-image should be deployed

```bash
$ gcloud endpoints services deploy openapi-run-v2.yaml
$ ../cli/gcloud_build_image -s endpoint-slide4vr-api-dnb6froqha-uc.a.run.app -c 2020-08-08r1 -p slide2vr
$ gcloud run deploy endpoint-slide4vr-api \
    --region us-central1 \
    --image="gcr.io/slide2vr/endpoints-runtime-serverless:2.14.0-endpoint-slide4vr-api-dnb6froqha-uc.a.run.app-2020-08-08r1" \
    --set-env-vars=ESPv2_ARGS=^++^--cors_preset=basic++--tracing_outgoing_context=traceparent++--tracing_sample_rate=1.0 \
    --allow-unauthenticated \
    --platform managed 
```

## Transform OpenAPI3 to Swagger2

```bash
$ docker run -it --rm -v (pwd):/data petrary/api-spec-converter
root@8e3ee0168170:/data# api-spec-converter -f openapi_3 -t swagger_2 openapi-run.yaml > openapi-run-v2.yaml
$ gcloud endpoints services deploy openapi-run-v2.yaml
```