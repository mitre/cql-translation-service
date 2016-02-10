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
        "schemaIdentifier": {
          "id": "urn:hl7-org:elm",
          "version": "r1"
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
