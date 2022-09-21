/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.siglus.siglusapi.localmachine.cdc;

import io.debezium.config.Configuration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigBuilder {
  @Value("${spring.datasource.url}")
  private String dbUrl;

  @Value("${spring.datasource.username}")
  private String dbUsername;

  @Value("${spring.datasource.password}")
  private String dbPassword;

  public Map<String, String> sinkConfig() {
    Map<String, String> config = new HashMap<>();
    config.put("connection.url", dbUrl);
    config.put("connection.user", dbUsername);
    config.put("connection.password", dbPassword);
    config.put("auto.create", "true");
    config.put("auto.evolve", "false");
    config.put("delete.enabled", "true");
    config.put("pk.mode", "record_key");
    config.put("insert.mode", "upsert");
    config.put("dialect.name", "PostgresDialect");
    return config;
  }

  public Configuration.Builder debeziumConfigBuilder() {
    String host = dbUrl.split("//")[1].split(":")[0];
    return Configuration.create()
        .with("database.hostname", host)
        .with("database.port", "5432")
        .with("database.user", dbUsername)
        .with("database.password", dbPassword)
        .with("database.dbname", "open_lmis")
        .with("database.allowPublicKeyRetrieval", "true")
        .with("database.server.id", "local")
        .with("database.server.name", "local")
        .with("time.precision.mode", "adaptive")
        .with("snapshot.mode", "never");
  }
}
