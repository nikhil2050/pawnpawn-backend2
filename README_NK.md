

PSQL:

> docker exec -it project-pnpn-postgres psql -U postgres -d pnpn_db

Fetch ENUM values:
> SELECT enum_range(NULL::listing_condition);

mvn clean test -pl pnpn-shared
mvn test -pl pnpn-user-service -Dtest="UserEntityTest,UserRepositoryTest" -Dsurefire.failIfNoSpecifiedTests=false -am -q 2>&1