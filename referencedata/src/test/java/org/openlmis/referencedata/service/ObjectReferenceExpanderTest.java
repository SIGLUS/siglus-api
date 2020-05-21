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

package org.openlmis.referencedata.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.javers.common.collections.Sets.asSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.openlmis.referencedata.domain.SupplyLine;
import org.openlmis.referencedata.dto.SupplyLineObjectReferenceDto;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.testbuilder.RequisitionGroupDataBuilder;
import org.openlmis.referencedata.testbuilder.SupervisoryNodeDataBuilder;
import org.openlmis.referencedata.testbuilder.SupplyLineDataBuilder;

public class ObjectReferenceExpanderTest {

  @InjectMocks
  private ObjectReferenceExpander objectReferenceExpander = new ObjectReferenceExpander();

  private SupplyLine supplyLine = new SupplyLineDataBuilder()
      .withSupervisoryNode(new SupervisoryNodeDataBuilder()
          .withRequisitionGroup(new RequisitionGroupDataBuilder().build())
          .build())
      .build();
  private SupplyLineObjectReferenceDto supplyLineDto = new SupplyLineObjectReferenceDto();

  @Before
  public void setUp() {
    supplyLine.export(supplyLineDto);
  }

  @Test
  public void shouldExpandDtoWithField() {
    objectReferenceExpander.expandDto(supplyLineDto, supplyLine, asSet("supervisoryNode"));

    assertThat(supplyLineDto.getSupervisoryNode().getFacility(), notNullValue());
  }

  @Test
  public void shouldExpandDtoWithNestedField() {
    objectReferenceExpander
        .expandDto(supplyLineDto, supplyLine, asSet("supervisoryNode.requisitionGroup"));

    assertThat(supplyLineDto.getSupervisoryNode().getRequisitionGroup().getSupervisoryNode(),
        notNullValue());
  }

  @Test
  public void shouldExpandDtoWithCollection() {
    objectReferenceExpander.expandDto(supplyLineDto,
        supplyLine, asSet("supervisoryNode.requisitionGroup.memberFacilities"));

    assertThat(supplyLineDto.getSupervisoryNode().getRequisitionGroup().getMemberFacilities(),
        notNullValue());
  }

  @Test
  public void shouldNotThrowAnExceptionIfEntityFieldIsNull() {
    supplyLine.setSupervisoryNode(null);

    objectReferenceExpander.expandDto(supplyLineDto, supplyLine, asSet("supervisoryNode"));

    assertThat(supplyLineDto.getSupervisoryNode().getRequisitionGroup(), nullValue());
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfExpandFieldIsNotPresent() {
    objectReferenceExpander.expandDto(supplyLineDto, supplyLine, asSet("unknownField"));
  }
}
