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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;

public class FhirLocationTest {
  private TestFhirLocation location = new TestFhirLocation();

  @Test
  public void shouldReturnTrueIfIsManagedExternallyFlagIsSetToTrue() {
    location.changeFlag(Boolean.TRUE.toString());
    assertThat(location.isManagedExternally()).isTrue();
  }

  @Test
  public void shouldReturnFalseIfIsManagedExternallyFlagIsSetToFalse() {
    location.changeFlag(Boolean.FALSE.toString());
    assertThat(location.isManagedExternally()).isFalse();
  }

  @Test
  public void shouldReturnFalseIfIsManagedExternallyFlagHasIncorrectValue() {
    location.changeFlag("def");
    assertThat(location.isManagedExternally()).isFalse();
  }

  @Test
  public void shouldReturnFalseIfIsManagedExternallyFlagIsNotSet() {
    location.changeFlag(null);
    assertThat(location.isManagedExternally()).isFalse();
  }

  private static final class TestFhirLocation implements FhirLocation {
    private Map<String, Object> map = Maps.newHashMap();

    @Override
    public UUID getId() {
      return UUID.randomUUID();
    }

    @Override
    public Map<String, Object> getExtraData() {
      return map;
    }

    void changeFlag(Object value) {
      if (null == value) {
        map.remove("isManagedExternally");
      } else {
        map.put("isManagedExternally", value);
      }
    }
  }

}
