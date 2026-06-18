

PSQL:

> docker exec -it project-pnpn-postgres psql -U postgres -d pnpn_db

Fetch ENUM values:
> SELECT enum_range(NULL::listing_condition);

mvn clean test -pl pnpn-shared
mvn test -pl pnpn-user-service -Dtest="UserEntityTest,UserRepositoryTest" -Dsurefire.failIfNoSpecifiedTests=false -am -q 2>&1
mvn test -pl pnpn-user-service -am 2>&1 | grep -E "Tests run:|BUILD|Failures:|Errors:|FAILURE|Caused by:"

```shell
# Compile catalog-service and dependencies
$ cd /Users/n/pawnpawn-backend2 && mvn compile -pl pnpn-catalog-service -am -q 2>&1

# Run catalog-service tests
$ cd /Users/n/pawnpawn-backend2 && mvn test -pl pnpn-catalog-service -am -DfailIfNoTests=false 2>&1 | tail -30

# Compile and run all new tests
$ cd /Users/n/pawnpawn-backend2 && mvn test -pl pnpn-catalog-service,pnpn-shared -am 2>&1 | tail -60
```
