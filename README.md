# CQL to ELM Translation Service

A microservice wrapper for the CQL to ELM conversion library.

Build:

    mvn package

Executed via the command line:

    java -jar target/cqlTranslationServer-1.0-SNAPSHOT-jar-with-dependencies.jar

Example usage via HTTP request:

    POST /cql/translator HTTP/1.1
    Content-Type: application/cql
    Accept: application/elm+json
    Host: localhost:8080
    Connection: close
    Content-Length: 610

    library CMS146 version '2'

    using QUICK

    valueset "Acute Pharyngitis": '2.16.840.1.113883.3.464.1003.102.12.1011'
    ...

Will return

    HTTP/1.1 200 OK
    Content-Type: application/elm+json
    Date: Wed, 10 Feb 2016 22:15:33 GMT
    Connection: close
    Content-Length: 6932

    {
      "library": {
        "identifier": {
          "id": "CMS146",
          "version": "2"
        },
        "usings": {"def": [
          {
            "localIdentifier": "System",
            "uri": "urn:hl7-org:elm-types:r1"
          },
          {
            "localId": "1",
            "localIdentifier": "QUICK",
            "uri": "http://hl7.org/fhir"
          }
        ]},
        "valueSets": {"def": [{
          "localId": "2",
          "name": "Acute Pharyngitis",
          "id": "2.16.840.1.113883.3.464.1003.102.12.1011",
          "accessLevel": "Public"
        },...]},
        ...
      }
    }

The service also supports `POST` of multiple CQL libraries packaged as
`multipart/form-data`. The result will be a similar package with one ELM part for each
CQL part in the submitted package.

## Docker Deployment

You may deploy pre-built Docker images into your existing hosting environment with:

	docker run -d -p 8080:8080 --restart unless-stopped cqframework/cql-translation-service:latest # or any official tag

And you're done. No environment variables or further configuration are needed. Jedi's may use your existing Kubernetes, Open Shift etc installations as you see fit. :)

To build your own image:

	docker build -t cqframework/cql-translation-service:latest . # but use your your own repo and tag strings!

## License

Copyright 2016-2017 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
