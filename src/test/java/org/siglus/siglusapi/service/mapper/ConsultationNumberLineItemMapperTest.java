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

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang.RandomStringUtils.random;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.junit.Assert.assertEquals;

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
import org.siglus.siglusapi.domain.ConsultationNumberLineItem;
import org.siglus.siglusapi.dto.ConsultationNumberColumnDto;
import org.siglus.siglusapi.dto.ConsultationNumberGroupDto;

@RunWith(MockitoJUnitRunner.class)
public class ConsultationNumberLineItemMapperTest {

  private static final int NOT_TOO_LONG = 10;

  @InjectMocks
  private ConsultationNumberLineItemMapper mapper;

  @Test
  public void shouldReturnLineItemsWhenFromGroupGivenGroup() {
    // given
    ConsultationNumberGroupDto groupDto = new ConsultationNumberGroupDto();
    groupDto.setName(random(NOT_TOO_LONG));
    Map<String, ConsultationNumberColumnDto> columns = new LinkedHashMap<>();
    groupDto.setColumns(columns);
    String columnName1 = random(NOT_TOO_LONG);
    ConsultationNumberColumnDto column1 = new ConsultationNumberColumnDto();
    column1.setId(randomUUID());
    column1.setValue(nextInt());
    columns.put(columnName1, column1);
    String columnName2 = random(NOT_TOO_LONG);
    ConsultationNumberColumnDto column2 = new ConsultationNumberColumnDto();
    column2.setId(randomUUID());
    column2.setValue(nextInt());
    columns.put(columnName2, column2);

    // when
    List<ConsultationNumberLineItem> lineItems = mapper
        .fromGroups(singletonList(groupDto));

    // then
    assertEquals(2, lineItems.size());
    ConsultationNumberLineItem lineItem1 = lineItems.get(0);
    assertEquals(groupDto.getName(), lineItem1.getGroup());
    assertEquals(columnName1, lineItem1.getColumn());
    assertEquals(column1.getId(), lineItem1.getId());
    assertEquals(column1.getValue(), lineItem1.getValue());
    ConsultationNumberLineItem lineItem2 = lineItems.get(1);
    assertEquals(groupDto.getName(), lineItem2.getGroup());
    assertEquals(columnName2, lineItem2.getColumn());
    assertEquals(column2.getId(), lineItem2.getId());
    assertEquals(column2.getValue(), lineItem2.getValue());
  }

  @Test
  public void shouldReturnEmptyWhenCallFromGivenNullColumns() {
    // given
    ConsultationNumberGroupDto groupDto = new ConsultationNumberGroupDto();
    groupDto.setName(random(NOT_TOO_LONG));

    // when
    List<ConsultationNumberLineItem> lineItems = mapper.fromGroups(singletonList(groupDto));

    // then
    assertEquals(0, lineItems.size());
  }

  @Test
  public void shouldReturnEmptyWhenFromGroupGivenEmptyColumns() {
    // given
    ConsultationNumberGroupDto groupDto = new ConsultationNumberGroupDto();
    groupDto.setName(random(NOT_TOO_LONG));
    groupDto.setColumns(Collections.emptyMap());

    // when
    List<ConsultationNumberLineItem> lineItems = mapper.fromGroups(singletonList(groupDto));

    // then
    assertEquals(0, lineItems.size());
  }

  @Test
  public void shouldReturnGroupsWhenFromColumnsGivenColumns() {
    // given
    List<ConsultationNumberLineItem> lineItems = new ArrayList<>();
    String groupName1 = random(NOT_TOO_LONG);
    ConsultationNumberLineItem lineItem1 = new ConsultationNumberLineItem();
    lineItems.add(lineItem1);
    lineItem1.setGroup(groupName1);
    lineItem1.setColumn(random(NOT_TOO_LONG));
    lineItem1.setId(randomUUID());
    lineItem1.setValue(nextInt());
    ConsultationNumberLineItem lineItem2 = new ConsultationNumberLineItem();
    lineItems.add(lineItem2);
    lineItem2.setGroup(groupName1);
    lineItem2.setColumn(random(NOT_TOO_LONG));
    lineItem2.setId(randomUUID());
    lineItem2.setValue(nextInt());
    ConsultationNumberLineItem lineItem3 = new ConsultationNumberLineItem();
    lineItems.add(lineItem3);
    lineItem3.setGroup(groupName1);
    lineItem3.setColumn(random(NOT_TOO_LONG));
    lineItem3.setId(randomUUID());
    lineItem3.setValue(nextInt());

    // when
    List<ConsultationNumberGroupDto> groups = mapper.fromLineItems(lineItems);

    // then
    assertEquals(1, groups.size());
    Map<String, ConsultationNumberGroupDto> groupMap = groups.stream()
        .collect(Collectors.toMap(ConsultationNumberGroupDto::getName, Function.identity()));
    ConsultationNumberGroupDto group = groupMap.get(groupName1);
    assertEquals(groupName1, group.getName());
    Map<String, ConsultationNumberColumnDto> columns = group.getColumns();
    assertEquals(3, columns.size());
    ConsultationNumberColumnDto group1Column1 = columns.get(lineItem1.getColumn());
    assertEquals(lineItem1.getId(), group1Column1.getId());
    assertEquals(lineItem1.getValue(), group1Column1.getValue());
    ConsultationNumberGroupDto group2 = groupMap.get(groupName1);
    assertEquals(groupName1, group2.getName());
    ConsultationNumberColumnDto group2Column1 = columns.get(lineItem2.getColumn());
    assertEquals(lineItem2.getId(), group2Column1.getId());
    assertEquals(lineItem2.getValue(), group2Column1.getValue());
    ConsultationNumberColumnDto group2Column2 = columns.get(lineItem3.getColumn());
    assertEquals(lineItem3.getId(), group2Column2.getId());
    assertEquals(lineItem3.getValue(), group2Column2.getValue());
  }

  @Test
  public void shouldReturnEmptyWhenFromColumnsGivenNullColumns() {
    // when
    List<ConsultationNumberGroupDto> groups = mapper.fromLineItems(null);

    // then
    assertEquals(0, groups.size());
  }

  @Test
  public void shouldReturnEmptyWhenFromColumnsGivenEmptyColumns() {
    // when
    List<ConsultationNumberGroupDto> groups = mapper.fromLineItems(Collections.emptyList());

    // then
    assertEquals(0, groups.size());
  }

}
