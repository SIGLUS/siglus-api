## Prerequisites
* Docker 1.11+
* Docker Compose 1.6+

All other dependencies, such as Java, are delivered automatically via the Docker image. It is unnecessary to install them locally to run the service, though often helpful to do so for the sake of development. See the _Tech_ section of [openlmis/dev](https://hub.docker.com/r/openlmis/dev/) for a list of these optional dependencies.

## <a name="building">Building & Testing</a>
Gradle is our usual build tool.  This template includes common tasks that most Services will find useful:

- `clean` to remove build artifacts
- `build` to build all source. `build`, after building sources, also runs unit tests. Build will be successful only if all tests pass.
- `generateMigration -PmigrationName=<yourMigrationName>` to create a "blank" database migration file. The file will be generated under `src/main/resources/db/migration`. Put your migration SQL into it.
- `test` to run unit tests
- `sonarqube` to execute the SonarQube analysis.

The **test results** are shown in the console.

While Gradle is our usual build tool, OpenLMIS v3+ is a collection of 
Independent Services where each Gradle build produces 1 Service. 
To help work with these Services, we use Docker to develop, build and 
publish these.

## Environment variables

The following environment variables are common to our services. They can be set either directly in compose files for images or provided as an environment file. See [docker-compose.yml](https://raw.githubusercontent.com/OpenLMIS/openlmis-ref-distro/master/docker-compose.yml) in the reference distribution for example usage. Also take a look at the sample [.env file](https://raw.githubusercontent.com/OpenLMIS/openlmis-config/master/.env) we provide. 

* **BASE_URL** - The base url of the OpenLMIS distribution. Will be used for communication between services. Each service should communicate with others using BASE_URL as the base in order to avoid direct communication, which might not work in more complex deployments. If the PUBLIC_URL variable is not set, this variable will be used for the generated links. This should be an url, for example: https://example.openlmis.org
* **VIRTUAL_HOST** - This is used by the nginx server as the virtual host under which the services are made avialble. This should be a host, for example: example.openlmis.org
* **PUBLIC_URL** - The public url of the OpenLMIS distribution. Will be used in generated links pointing to this distribution. If this variable is not set, the BASE_URL will be used for the generated links. We extract this usage of BASE_URL to another environmental variable because in more complex deployments the BASE_URL does not have to be the base domain name. This should be an url, for example: https://example.openlmis.org
* **CONSUL_HOST** - Identifies the IP address or DNS name of the Consul server. Set this to the host or IP under which the distribution is available and Consul listens for connections. Services should register with Consul under this address. This should be a host or an IP, for example 8.8.8.8.
* **CONSUL_PORT** - The port used by the Consul server - services should use this port to register with Consul. This should be a port number, for example 8500. 8500 is used by default.
* **REQUIRE_SSL** - Whether HTTPS is required. If set to `true`, nginx will redirect all incoming HTTP connections to HTTPS. By default SSL will not be required - either leave it blank or set to `false` if you wish to allow HTTP connections.
* **LOCALE** - Default localized system language. It will be applied to all running services, if this variable is missing default "en" value will be used.
* **CORS_ALLOWED_ORIGINS** - Comma-separated list of origins that are allowed, for example: `https://test.openlmis.org,http://some.external.domain`. `*` allows all origins. Leave empty to disable CORS.
* **CORS_ALLOWED_METHODS** - Comma-separated list of HTTP methods that are allowed for the above origins.

These variables are used by services for their connection to the database (none of these have defaults):

* **DATABASE_URL** - The JDBC url under which the database is accessible. Our services use `jdbc:postgresql://db:5432/open_lmis` for connecting to the PostgreSQL database running in a container.
* **POSTGRES_USER** - The username of the database user that the services should use. This variable is also used by our PostgreSQL container to create a user.
* **POSTGRES_PASSWORD** - The password of the database user that the services should use. This variable is also used by our PostgreSQL container to create a user.

These variables are used by our builds in order to integrate with the [Transifex](https://www.transifex.com/) translation management system:

* **TRANSIFEX_USER** - The username to use with Transifex for updating translations.
* **TRANSIFEX_PASSWORD** - The password to use with Transifex for updating translations.


## Config Talisman

Talisman is a tool that installs a hook to your repository to ensure that potential secrets or sensitive information do not leave the developer's workstation.
It validates the outgoing changeset for things that look suspicious - such as potential SSH keys, authorization tokens, private keys etc.

```
# download the talisman binary
curl https://thoughtworks.github.io/talisman/install.sh > ~/install-talisman.sh
chmod +x ~/install-talisman.sh
# go to project
cd siglus-api
# delete pre-push if existed
rm .git/hooks/pre-push
# install new pre-push hook
~/install-talisman.sh
```

### Dependency Check Hawkeye
The Hawkeye scanner-cli is a project security, vulnerability and general risk highlighting tool. 
It is meant to be integrated into your pre-commit hooks and your pipelines.

```
# The docker image is hands-down the easiest way to the scanner. Please note that your project root (e.g. $PWD) needs to be mounted to /target.
docker run --rm -v $PWD:/target hawkeyesec/scanner-cli:latest
```
