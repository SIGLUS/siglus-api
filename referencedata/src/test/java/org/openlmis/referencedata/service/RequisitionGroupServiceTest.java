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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.GeographicZoneRepository;
import org.openlmis.referencedata.repository.ProgramRepository;
import org.openlmis.referencedata.repository.RequisitionGroupRepository;
import org.openlmis.referencedata.repository.SupervisoryNodeRepository;
import org.openlmis.referencedata.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class RequisitionGroupServiceTest {

  private static final String CODE = "code";
  private static final String NAME = "name";
  private static final String PROGRAM = "program";
  private static final String ZONE = "zone";

  @Mock
  private RequisitionGroupRepository requisitionGroupRepository;

  @Mock
  private GeographicZoneRepository geographicZoneRepository;

  @Mock
  private SupervisoryNodeRepository supervisoryNodeRepository;

  @Mock
  private ProgramRepository programRepository;

  @Mock
  private Pageable pageable;

  @Mock
  private GeographicZone zone;

  @Mock
  private Program program;

  @Mock
  private SupervisoryNode supervisoryNode;

  @Mock
  private RequisitionGroup requisitionGroup1;

  @Mock
  private RequisitionGroup requisitionGroup2;

  private UUID requisitionGroup1Id = UUID.randomUUID();
  private List<RequisitionGroup> requisitionGroups;

  @InjectMocks
  private RequisitionGroupService requisitionGroupService = new RequisitionGroupService();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    requisitionGroups = Lists.newArrayList(requisitionGroup1, requisitionGroup2);
    when(requisitionGroup1.getId()).thenReturn(requisitionGroup1Id);
    when(requisitionGroup1.getName()).thenReturn("RG-1");
    when(requisitionGroup2.getName()).thenReturn("RG-2");
    when(pageable.getPageSize()).thenReturn(10);
    when(pageable.getPageNumber()).thenReturn(0);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfThereNotProvidedForSearch() {
    Map<String, Object> searchParams = new HashMap<>();
    searchParams.put("some-param", "some-value");
    requisitionGroupService.searchRequisitionGroups(searchParams, pageable);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfGeographicZoneDoesNotExist() {
    when(geographicZoneRepository.findOne(any(UUID.class))).thenReturn(null);

    Map<String, Object> searchParams = new HashMap<>();
    searchParams.put(ZONE, "zone-code");
    requisitionGroupService.searchRequisitionGroups(searchParams, pageable);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfProgramDoesNotExist() {
    when(programRepository.findByCode(any(Code.class))).thenReturn(null);

    Map<String, Object> searchParams = new HashMap<>();
    searchParams.put(PROGRAM, "program-code");
    requisitionGroupService.searchRequisitionGroups(searchParams, pageable);
  }

  @Test
  public void shouldReturnAllElementsIfNoSearchCriteriaProvided() {
    when(requisitionGroupRepository.findAll()).thenReturn(requisitionGroups);

    Page<RequisitionGroup> actual = requisitionGroupService
        .searchRequisitionGroups(new HashMap<>(), pageable);
    verify(requisitionGroupRepository).findAll();
    assertEquals(requisitionGroups, actual.getContent());
  }

  @Test
  public void shouldSearchForRequisitionGroupsWithAllParametersProvided() {
    when(geographicZoneRepository.findByCode(any(String.class))).thenReturn(zone);
    when(programRepository.findByCode(any(Code.class))).thenReturn(program);
    List<SupervisoryNode> nodes = Collections.singletonList(supervisoryNode);
    doReturn(new PageImpl(nodes, pageable, nodes.size()))
        .when(supervisoryNodeRepository).search(any(), any());
    when(requisitionGroupRepository.search(any(String.class), any(String.class),
        any(Program.class), any(List.class), any(Pageable.class)))
        .thenReturn(Pagination.getPage(requisitionGroups, null, 2));

    Map<String, Object> searchParams = new HashMap<>();
    searchParams.put(NAME, "name");
    searchParams.put(CODE, "code");
    searchParams.put(PROGRAM, "program-code");
    searchParams.put(ZONE, "zone-code");

    Page<RequisitionGroup> actual = requisitionGroupService
        .searchRequisitionGroups(searchParams, pageable);
    verify(requisitionGroupRepository).search("code", "name", program, nodes, pageable);
    assertEquals(requisitionGroups, actual.getContent());
  }
}
