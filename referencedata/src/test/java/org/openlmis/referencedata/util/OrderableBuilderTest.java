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

package org.openlmis.referencedata.util;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Dispensable;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.ProgramOrderable;
import org.openlmis.referencedata.dto.ObjectReferenceDto;
import org.openlmis.referencedata.dto.OrderableChildDto;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.referencedata.dto.ProgramOrderableDto;
import org.openlmis.referencedata.repository.OrderableRepository;
import org.openlmis.referencedata.repository.ProgramRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@RunWith(MockitoJUnitRunner.class)
public class OrderableBuilderTest {

  @Mock
  private ProgramRepository programRepository;

  @Mock
  private OrderableRepository orderableRepository;

  @InjectMocks
  private OrderableBuilder orderableBuilder;

  @Test
  public void shouldCreateOrderableWithSingleProgram() throws Exception {
    Program program = createProgram("test_program");
    Orderable orderable = createOrderable(program.getId());

    assertOrderable(orderable, program);
  }

  @Test
  public void shouldCreateOrderableWithMultiplePrograms() throws Exception {
    Program program1 = createProgram("test_program_1");
    Program program2 = createProgram("test_program_2");

    Orderable orderable = createOrderable(program1.getId(), program2.getId());

    assertOrderable(orderable, program1, program2);
  }

  @Test
  public void shouldCreateOrderableWithChildren() {

    Set<OrderableChildDto> childSet = new HashSet<>();
    ObjectReferenceDto objectReferenceDto = new ObjectReferenceDto();
    objectReferenceDto.setId(UUID.randomUUID());
    OrderableChildDto childDto = new OrderableChildDto(objectReferenceDto, 20L);
    childSet.add(childDto);
    OrderableDto orderableDto = new OrderableDto();

    orderableDto.setChildren(childSet);
    orderableDto.setNetContent(100L);
    orderableDto.setPackRoundingThreshold(1L);
    orderableDto.setRoundToZero(true);
    orderableDto.setDispensable(Dispensable.createNew("each"));

    Orderable child = createOrderable();

    Page<Orderable> orderablePage = new PageImpl<>(asList(child), new PageRequest(1, 100), 1);

    when(orderableRepository
        .findAllLatestByIds(any(), any()))
        .thenReturn(orderablePage);

    Orderable orderable = orderableBuilder.newOrderable(orderableDto, null);
    assertThat(orderable.getChildren().size(), is(1));
  }

  @Test
  public void shouldCreateOrderableWithVersion1() {
    Orderable orderable = createOrderable();

    assertThat(orderable.getVersionNumber(), is(1L));
  }

  @Test
  public void shouldIncrementVersionNumber() {

    Orderable orderable = createOrderable();
    OrderableDto orderableDto = createOrderableDto();

    Orderable updatedOrderable = Orderable.updateFrom(orderable, orderableDto);
    assertThat(updatedOrderable.getVersionNumber(), is(2L));
  }

  private Program createProgram(String code) {
    Program program = new Program(UUID.randomUUID());
    program.setCode(Code.code(code));

    when(programRepository.findOne(program.getId())).thenReturn(program);

    return program;
  }

  private void assertOrderable(Orderable orderable, Program... programs) {
    for (Program program : programs) {
      ProgramOrderable programOrderable = orderable.getProgramOrderable(program);

      assertThat(programOrderable, is(notNullValue()));
      assertThat(programOrderable.getProgram().getCode(), is(equalTo(program.getCode())));
    }
  }

  private Orderable createOrderable(UUID... programIds) {
    OrderableDto orderableDto = createOrderableDto(programIds);
    return orderableBuilder.newOrderable(orderableDto, null);
  }

  private OrderableDto createOrderableDto(UUID... programIds) {
    Set<ProgramOrderableDto> programs = Sets.newHashSet();

    for (UUID programId : programIds) {
      ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
      programOrderableDto.setProgramId(programId);
      programOrderableDto.setActive(true);

      programs.add(programOrderableDto);
    }

    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setNetContent(100L);
    orderableDto.setPackRoundingThreshold(1L);
    orderableDto.setRoundToZero(true);
    orderableDto.setPrograms(programs);
    orderableDto.setDispensable(Dispensable.createNew("each"));
    return orderableDto;
  }

}