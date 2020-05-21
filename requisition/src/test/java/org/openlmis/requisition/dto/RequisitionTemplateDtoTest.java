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

package org.openlmis.requisition.dto;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.openlmis.requisition.domain.RequisitionTemplateDataBuilder;

public class RequisitionTemplateDtoTest {

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(RequisitionTemplateDto.class)
        .withRedefinedSuperclass()
        .suppress(Warning.NONFINAL_FIELDS) // fields in DTO cannot be final
        .verify();
  }

  @Test
  public void shouldReturnProgramId() {
    UUID id = UUID.randomUUID();

    RequisitionTemplateDto dto = new RequisitionTemplateDataBuilder()
        .buildAsDto();
    dto.setProgram(new ObjectReferenceDto(id));

    assertThat(dto.getProgramId(), is(id));
  }

  @Test
  public void shouldReturnFacilityTypeIds() {
    Set<UUID> facilityTypeId = IntStream
        .range(0, 4)
        .mapToObj(idx -> UUID.randomUUID())
        .collect(Collectors.toSet());

    RequisitionTemplateDto dto = new RequisitionTemplateDataBuilder()
        .buildAsDto();

    facilityTypeId.forEach(id -> dto.getFacilityTypes().add(new ObjectReferenceDto(id)));
    assertThat(dto.getFacilityTypeIds(), hasSize(facilityTypeId.size()));
    assertThat(dto.getFacilityTypeIds(), hasItems(facilityTypeId.toArray(new UUID[0])));
  }

  @Test
  public void shouldReturnEmptySetOfFacilityTypeIdsIfThereIsNoTypes() {
    RequisitionTemplateDto dto = new RequisitionTemplateDataBuilder()
        .buildAsDto();

    assertThat(dto.getFacilityTypeIds(), hasSize(0));
  }
}
