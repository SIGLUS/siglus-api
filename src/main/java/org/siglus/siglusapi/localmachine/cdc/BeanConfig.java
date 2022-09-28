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

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {
  @Bean
  public DbSchemaReader schemaReader(ConfigBuilder configBuilder) {
    // read all schemas in db for replaying
    return DbSchemaReader.of(
        configBuilder.debeziumConfigBuilder().with("schema.include.list", ".+").build());
  }

  @Bean
  public CdcListener dummyListener() {
    // add a dummy listener to ensure non localmachine profile work, can be removed later on
    return new CdcListener() {
      @Override
      public void on(List<CdcRecord> records) {}

      @Override
      public String[] acceptedTables() {
        return new String[0];
      }
    };
  }
}