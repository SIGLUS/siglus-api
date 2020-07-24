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

package org.siglus.siglusapi.service.mapper;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang.RandomStringUtils.random;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.PatientLineItem;
import org.siglus.siglusapi.dto.PatientColumnDto;
import org.siglus.siglusapi.dto.PatientGroupDto;

@RunWith(MockitoJUnitRunner.class)
public class PatientLineItemMapperTest {

  private static final int NOT_TOO_LONG = 10;

  @InjectMocks
  private PatientLineItemMapper mapper;

  @Test
  public void shouldReturnLineItemsWhenFromGroupGivenGroup() {
    // given
    PatientGroupDto patientGroup = new PatientGroupDto();
    patientGroup.setName(random(NOT_TOO_LONG));
    Map<String, PatientColumnDto> columns = new LinkedHashMap<>();
    patientGroup.setColumns(columns);
    String columnName1 = random(NOT_TOO_LONG);
    PatientColumnDto column1 = new PatientColumnDto();
    column1.setId(randomUUID());
    column1.setValue(nextInt());
    columns.put(columnName1, column1);
    String columnName2 = random(NOT_TOO_LONG);
    PatientColumnDto column2 = new PatientColumnDto();
    column2.setId(randomUUID());
    column2.setValue(nextInt());
    columns.put(columnName2, column2);

    // when
    List<PatientLineItem> lineItems = mapper.from(patientGroup);

    // then
    assertEquals(2, lineItems.size());
    PatientLineItem lineItem1 = lineItems.get(0);
    assertEquals(patientGroup.getName(), lineItem1.getGroup());
    assertEquals(columnName1, lineItem1.getColumn());
    assertEquals(column1.getId(), lineItem1.getId());
    assertEquals(column1.getValue(), lineItem1.getValue());
    PatientLineItem lineItem2 = lineItems.get(1);
    assertEquals(patientGroup.getName(), lineItem2.getGroup());
    assertEquals(columnName2, lineItem2.getColumn());
    assertEquals(column2.getId(), lineItem2.getId());
    assertEquals(column2.getValue(), lineItem2.getValue());
  }

  @Test
  public void shouldReturnNullWhenFromGroupGivenNullGroup() {
    // when
    List<PatientLineItem> lineItems = mapper.from((PatientGroupDto) null);

    // then
    assertNull(lineItems);
  }

  @Test
  public void shouldReturnEmptyWhenCallFromGivenNullColumns() {
    // given
    PatientGroupDto patientGroup = new PatientGroupDto();
    patientGroup.setName(random(NOT_TOO_LONG));

    // when
    List<PatientLineItem> lineItems = mapper.from(patientGroup);

    // then
    assertEquals(0, lineItems.size());
  }

  @Test
  public void shouldReturnEmptyWhenFromGroupGivenEmptyColumns() {
    // given
    PatientGroupDto patientGroup = new PatientGroupDto();
    patientGroup.setName(random(NOT_TOO_LONG));
    patientGroup.setColumns(Collections.emptyMap());

    // when
    List<PatientLineItem> lineItems = mapper.from(patientGroup);

    // then
    assertEquals(0, lineItems.size());
  }

  @Test
  public void shouldReturnGroupsWhenFromColumnsGivenColumns() {
    // given
    List<PatientLineItem> lineItems = new ArrayList<>();
    String groupName1 = random(NOT_TOO_LONG);
    PatientLineItem lineItem1 = new PatientLineItem();
    lineItems.add(lineItem1);
    lineItem1.setGroup(groupName1);
    lineItem1.setColumn(random(NOT_TOO_LONG));
    lineItem1.setId(randomUUID());
    lineItem1.setValue(nextInt());
    String groupName2 = random(NOT_TOO_LONG);
    PatientLineItem lineItem2 = new PatientLineItem();
    lineItems.add(lineItem2);
    lineItem2.setGroup(groupName2);
    lineItem2.setColumn(random(NOT_TOO_LONG));
    lineItem2.setId(randomUUID());
    lineItem2.setValue(nextInt());
    PatientLineItem lineItem3 = new PatientLineItem();
    lineItems.add(lineItem3);
    lineItem3.setGroup(groupName2);
    lineItem3.setColumn(random(NOT_TOO_LONG));
    lineItem3.setId(randomUUID());
    lineItem3.setValue(nextInt());

    // when
    List<PatientGroupDto> groups = mapper.from(lineItems);

    // then
    assertEquals(2, groups.size());
    Map<String, PatientGroupDto> groupMap = groups.stream()
        .collect(Collectors.toMap(PatientGroupDto::getName, Function.identity()));
    PatientGroupDto group1 = groupMap.get(groupName1);
    assertEquals(groupName1, group1.getName());
    Map<String, PatientColumnDto> group1Columns = group1.getColumns();
    assertEquals(1, group1Columns.size());
    PatientColumnDto group1Column1 = group1Columns.get(lineItem1.getColumn());
    assertEquals(lineItem1.getId(), group1Column1.getId());
    assertEquals(lineItem1.getValue(), group1Column1.getValue());
    PatientGroupDto group2 = groupMap.get(groupName2);
    assertEquals(groupName2, group2.getName());
    Map<String, PatientColumnDto> group2Columns = group2.getColumns();
    assertEquals(2, group2Columns.size());
    PatientColumnDto group2Column1 = group2Columns.get(lineItem2.getColumn());
    assertEquals(lineItem2.getId(), group2Column1.getId());
    assertEquals(lineItem2.getValue(), group2Column1.getValue());
    PatientColumnDto group2Column2 = group2Columns.get(lineItem3.getColumn());
    assertEquals(lineItem3.getId(), group2Column2.getId());
    assertEquals(lineItem3.getValue(), group2Column2.getValue());
  }

  @Test
  public void shouldReturnEmptyWhenFromColumnsGivenNullColumns() {
    // when
    List<PatientGroupDto> groups = mapper.from((List<PatientLineItem>) null);

    // then
    assertEquals(0, groups.size());
  }

  @Test
  public void shouldReturnEmptyWhenFromColumnsGivenEmptyColumns() {
    // when
    List<PatientGroupDto> groups = mapper.from(Collections.emptyList());

    // then
    assertEquals(0, groups.size());
  }

}
