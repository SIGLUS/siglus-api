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

package org.openlmis.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.openlmis.notification.testutils.DigestConfigurationDataBuilder;
import org.openlmis.notification.testutils.ToStringTestUtils;

public class DigestConfigurationTest {


  private static final String ID = "id";
  private static final String MESSAGE = "message";
  private static final String TAG = "tag";

  @Test
  public void shouldExportData() {
    Map<String, Object> map = Maps.newHashMap();
    DummyExporter exporter = new DummyExporter(map);

    DigestConfiguration configuration = new DigestConfigurationDataBuilder().build();
    configuration.export(exporter);

    assertThat(map)
        .containsEntry(ID, configuration.getId())
        .containsEntry(MESSAGE, configuration.getMessage())
        .containsEntry(TAG, configuration.getTag());
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(DigestConfiguration.class)
        .withRedefinedSuperclass()
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    ToStringTestUtils.verify(DigestConfiguration.class, new DigestConfiguration());
  }

  @AllArgsConstructor
  private static final class DummyExporter implements DigestConfiguration.Exporter {

    private Map<String, Object> map;


    @Override
    public void setId(UUID id) {
      map.put(ID, id);
    }

    @Override
    public void setMessage(String message) {
      map.put(MESSAGE, message);
    }

    @Override
    public void setTag(String tag) {
      map.put(TAG, tag);
    }
  }

}
