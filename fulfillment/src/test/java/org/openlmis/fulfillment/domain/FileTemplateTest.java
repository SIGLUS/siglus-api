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

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.openlmis.fulfillment.FileColumnBuilder;
import org.openlmis.fulfillment.web.util.FileColumnDto;
import org.openlmis.fulfillment.web.util.FileTemplateDto;

public class FileTemplateTest {

  @Test
  public void equalsContract() {
    EqualsVerifier.forClass(FileTemplate.class)
        .suppress(Warning.NONFINAL_FIELDS)
        .withRedefinedSuperclass()
        .withPrefabValues(FileColumn.class,
            new FileColumnBuilder().build(),
            new FileColumnBuilder().build())
        .verify();
  }

  @Test
  public void shouldExport() {
    FileTemplate template = new FileTemplate("O", false, TemplateType.ORDER,
        Collections.emptyList());

    FileTemplateDto dto = new FileTemplateDto();
    template.export(dto);

    assertThat(dto.getFilePrefix(), is(template.getFilePrefix()));
    assertThat(dto.getHeaderInFile(), is(template.getHeaderInFile()));
    assertThat(dto.getTemplateType().name(), is(template.getTemplateType().name()));
  }

  @Test
  public void shouldImport() {
    FileColumnDto columnDto = new FileColumnDto(UUID.randomUUID(), false, "Label2",
        "KeyPath", false, 5, "Format", "m.y.z", "p.a.t.h", "r.e.l.a.t.e", "p.a.t.h");
    FileTemplateDto templateDto = new FileTemplateDto(UUID.randomUUID(), "O", false,
        asList(columnDto), TemplateType.ORDER);

    FileTemplate template = new FileTemplate();
    template.setFileColumns(new ArrayList<>());
    template.importDto(templateDto);

    assertThat(templateDto.getFilePrefix(), is(template.getFilePrefix()));
    assertThat(templateDto.getHeaderInFile(), is(template.getHeaderInFile()));
    assertThat(templateDto.getTemplateType().name(), is(template.getTemplateType().name()));

    FileColumn domainColumn = template.getFileColumns().get(0);
    assertThat(domainColumn.getColumnLabel(), Matchers.is(columnDto.getColumnLabel()));
    assertThat(domainColumn.getFormat(), Matchers.is(columnDto.getFormat()));
    assertThat(domainColumn.getDataFieldLabel(), Matchers.is(columnDto.getDataFieldLabel()));
    assertThat(domainColumn.getInclude(), Matchers.is(columnDto.getInclude()));
    assertThat(domainColumn.getKeyPath(), Matchers.is(columnDto.getKeyPath()));
    assertThat(domainColumn.getNested(), Matchers.is(columnDto.getNested()));
    assertThat(domainColumn.getPosition(), Matchers.is(columnDto.getPosition()));
    assertThat(domainColumn.getRelated(), Matchers.is(columnDto.getRelated()));
    assertThat(domainColumn.getOpenLmisField(), Matchers.is(columnDto.getOpenLmisField()));
    assertThat(domainColumn.getRelatedKeyPath(), Matchers.is(columnDto.getRelatedKeyPath()));
  }

}