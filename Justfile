# default - list targets
default:
    @just --list

# db - start server
start-db: 
    docker compose up -d

# db - stop server
stop-db:
    docker compose down

# run web app
run:
    gradle bootRun

# run curl test
test-api:
    #!/usr/bin/env bash
    set -euo pipefail

    # create
    EVENT_ID=$(curl -s -X POST http://localhost:8080/api/events \
      -H "Content-Type: application/json" \
      -d '{"source":"test-service","severity":"WARN","message":"integration test event"}' \
      | jq -r '.id')
    echo "created: $EVENT_ID"

    # read
    curl -i http://localhost:8080/api/events/$EVENT_ID

    # update
    curl -i -X PUT http://localhost:8080/api/events/$EVENT_ID \
      -H "Content-Type: application/json" \
      -d '{"message":"message rewritten by mutation"}'

    # delete
    curl -i -X DELETE http://localhost:8080/api/events/$EVENT_ID

    # confirm gone (expect 404)
    curl -i http://localhost:8080/api/events/$EVENT_ID

    # lists recent events
    curl -i "http://localhost:8080/api/events?limit=10"

    # aggregates stats
    curl -i http://localhost:8080/api/stats
    
# run bruno tests
test-bruno:
    cd bruno-collection && bru run --reporter-html ../results.html
    