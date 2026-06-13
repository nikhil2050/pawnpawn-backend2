

PSQL:

> docker exec -it project-pnpn-postgres psql -U postgres -d pnpn_db

Fetch ENUM values:
> SELECT enum_range(NULL::listing_condition);

mvn clean test -pl pnpn-shared
