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
