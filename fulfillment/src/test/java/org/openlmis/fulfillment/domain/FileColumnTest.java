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

package org.openlmis.fulfillment.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.openlmis.fulfillment.FileTemplateBuilder;
import org.openlmis.fulfillment.web.util.FileColumnDto;

public class FileColumnTest {

  @Test
  public void equalsContract() {
    EqualsVerifier.forClass(FileColumn.class)
        .suppress(Warning.NONFINAL_FIELDS)
        .withRedefinedSuperclass()
        .withPrefabValues(FileTemplate.class,
            new FileTemplateBuilder().build(),
            new FileTemplateBuilder().build())
        .verify();
  }

  @Test
  public void shouldImportFromDto() {

    FileColumnDto columnDto = new FileColumnDto(UUID.randomUUID(), false, "Label2",
        "KeyPath", false, 5, "Format", "x.y.z", "p.a.t.h", "r.e.l.a.t.e", "p.a.t.h");

    FileColumn domainColumn = FileColumn.newInstance(columnDto);

    assertThat(domainColumn.getColumnLabel(), is(columnDto.getColumnLabel()));
    assertThat(domainColumn.getFormat(), is(columnDto.getFormat()));
    assertThat(domainColumn.getDataFieldLabel(), is(columnDto.getDataFieldLabel()));
    assertThat(domainColumn.getInclude(), is(columnDto.getInclude()));
    assertThat(domainColumn.getKeyPath(), is(columnDto.getKeyPath()));
    assertThat(domainColumn.getNested(), is(columnDto.getNested()));
    assertThat(domainColumn.getPosition(), is(columnDto.getPosition()));
    assertThat(domainColumn.getRelated(), is(columnDto.getRelated()));
    assertThat(domainColumn.getOpenLmisField(), is(columnDto.getOpenLmisField()));
    assertThat(domainColumn.getRelatedKeyPath(), is(columnDto.getRelatedKeyPath()));
  }

}