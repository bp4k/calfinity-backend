# Locationlens backend

## Prerequisite

Install Java 21 and make it available as `java`. Use the included Maven wrapper;
a separate Maven installation is not required.

## Test and run locally

Run the build and tests:

```sh
./mvnw --batch-mode clean verify
```

Start the application:

```sh
./mvnw spring-boot:run
```

The application listens on the port in `PORT`, defaulting to `8080` when it is
unset. For example:

```sh
PORT=8081 ./mvnw spring-boot:run
```

Google Cloud Run supplies `PORT` at runtime. Google Cloud Build can build the
Maven project using Google Cloud Buildpacks; the GitHub Actions workflow runs
CI only.

## Calendar API

The bearer-token-protected endpoints are under `/api/v1/calendars`. The
authenticated JWT `sub` claim is used as the owner/member identifier; clients
cannot choose another user's ID.

| Method and path | Behavior |
| --- | --- |
| `GET /api/v1/calendars` | Lists active calendars owned by or shared with the caller; an empty list is returned when there are none. |
| `POST /api/v1/calendars` | Creates a calendar; the creator is returned as `OWNER` and the response carries a `Location` header for the new resource. |

Owned calendars are returned with the `OWNER` role, followed by calendars
shared with the caller at their membership role. Soft-deleted calendars and
revoked (soft-deleted) memberships are excluded. Shared access is read from
active `calendar_memberships` records; this API does not yet include an
endpoint to create or manage those memberships. Duplicate calendar/user
membership rows are prevented by a database constraint. The remaining CRUD
endpoints (read one, update, delete) land in follow-up changes.

On create, names are trimmed and must be non-blank and at most 255
characters. Colors must be `#RRGGBB`; omitting color uses `#4285F4`. Only one
active personal calendar is allowed per owner (enforced both in the service
and by a partial unique index); soft-deleted personal calendars do not count
against the limit.

## Calendar persistence

Calendar persistence uses Spring Data JPA with PostgreSQL, a straightforward
choice for a new Cloud SQL database. Configure `DATABASE_URL`,
`DATABASE_USERNAME`, and `DATABASE_PASSWORD` in the environment before starting
the application. `DATABASE_URL` must be a PostgreSQL JDBC URL; Cloud Run's
Cloud SQL connection should be configured to make the instance reachable to the
application.

Flyway applies the calendar and membership schema from
`src/main/resources/db/migration`. Calendar deletion is soft deletion, so the
calendar and its memberships remain in the database but are excluded from
normal reads. Active personal calendars are unique per owner.
Hibernate validates the schema at startup instead of creating or modifying
production tables.
# LocationLens
## Command to start and run Docker

```
docker run -p 127.0.0.1:8084:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  -v kcdata:/opt/keycloak/data \
  --name keycloak \
  quay.io/keycloak/keycloak:26.7.4 start-dev
```

1. Go to localhost:8084 in your browser and login with admin/admin.
1. Click on Manage Realms -- Create Realm.
1. Type realm name as `locationlens`.
1. click create
1. Ensure the current realm is locationlens
1. Go to Users -- create a new user. I am using linus, linus@example.com,
Linus Sebastian
1. Click on the Credentials tab -- Set Password, and set a password. Disable
temporary. I used 11111 as a very secure and totally not easy to guess password.
1. Now select Clients, Create client. Name it locationlens, type OIDC (which is
OAuth2)
1. Enable Client authentication, click on next
1. Valid redirect uri: http://localhost:8080/login/oauth2/code/common-idp
1. The client ID is locationlens, client secret is found under credentials tab.

create .env file:
```
IDP_CLIENT_ID=<clientid>
IDP_CLIENT_SECRET=<clientsecret>
IDP_URI=http://localhost:8084/realms/locationlens
```

run:
```
set -a
. .env
set +a
```

now run the app

```
./mvnw spring:run
```
