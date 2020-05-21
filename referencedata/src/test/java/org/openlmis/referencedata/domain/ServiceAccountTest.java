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

package org.openlmis.referencedata.domain;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Maps;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.openlmis.referencedata.ToStringTestUtils;
import org.openlmis.referencedata.testbuilder.ServiceAccountDataBuilder;

public class ServiceAccountTest {

  @Test
  public void shouldCreateInstanceBasedOnImporter() {
    ServiceAccount expected = new ServiceAccountDataBuilder().build();
    ServiceAccount.Importer importer = new ServiceAccount.Importer() {

      @Override
      public UUID getToken() {
        return expected.getToken();
      }

      @Override
      public UUID getCreatedBy() {
        return expected.getCreationDetails().getCreatedBy();
      }

      @Override
      public ZonedDateTime getCreatedDate() {
        return expected.getCreationDetails().getCreatedDate();
      }
    };

    ServiceAccount actual = ServiceAccount.newServiceAccount(importer);
    assertThat(actual, is(equalTo(expected)));
  }

  @Test
  public void shouldExportValues() {
    Map<String, Object> values = Maps.newHashMap();
    ServiceAccount.Exporter exporter = new ServiceAccount.Exporter() {

      @Override
      public void setToken(UUID apiKey) {
        values.put("apiKey", apiKey);
      }

      @Override
      public void setCreatedBy(UUID createdBy) {
        values.put("createdBy", createdBy);
      }

      @Override
      public void setCreatedDate(ZonedDateTime createdDate) {
        values.put("createdDate", createdDate);
      }
    };

    ServiceAccount account = new ServiceAccountDataBuilder().build();
    account.export(exporter);

    assertThat(values, hasEntry("apiKey", account.getToken()));
    assertThat(values, hasEntry("createdBy", account.getCreationDetails().getCreatedBy()));
    assertThat(values, hasEntry("createdDate", account.getCreationDetails().getCreatedDate()));
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(ServiceAccount.class)
        .withRedefinedSuperclass()
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    ServiceAccount account = new ServiceAccountDataBuilder().build();
    ToStringTestUtils.verify(ServiceAccount.class, account);
  }

}
