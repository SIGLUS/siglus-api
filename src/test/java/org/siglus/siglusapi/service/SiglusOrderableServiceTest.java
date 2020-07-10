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

package org.siglus.siglusapi.service;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.referencedata.dto.ProgramOrderableDto;
import org.openlmis.referencedata.repository.OrderableRepository;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.web.QueryOrderableSearchParams;
import org.siglus.common.domain.ProgramExtension;
import org.siglus.common.repository.ProgramExtensionRepository;
import org.siglus.siglusapi.dto.SiglusOrderableDto;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.testutils.ProgramExtensionDataBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RunWith(MockitoJUnitRunner.class)
public class SiglusOrderableServiceTest {

  @InjectMocks
  private SiglusOrderableService siglusOrderableService;

  @Mock
  private ProgramExtensionRepository programExtensionRepository;

  @Mock
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private SiglusArchiveProductService archiveProductService;

  @Mock
  private OrderableRepository orderableRepository;

  @Mock
  private QueryOrderableSearchParams searchParams;

  private Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);

  private UUID facilityId = UUID.randomUUID();

  private UUID programId = UUID.randomUUID();

  private UUID orderableId = UUID.randomUUID();

  private UUID parentId = UUID.randomUUID();

  @Before
  public void prepare() {
    ProgramExtension programExtension = new ProgramExtensionDataBuilder()
        .withProgramId(programId)
        .withParentId(parentId)
        .build();
    when(programExtensionRepository.findAll()).thenReturn(newArrayList(programExtension));
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setProgramId(programId);
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setPrograms(newHashSet(programOrderableDto));
    when(orderableReferenceDataService.searchOrderables(searchParams, pageable)).thenReturn(
        Pagination.getPage(newArrayList(orderableDto), pageable, 1));
  }

  @Test
  public void shouldReturnDataWithParentIdWhenSearchOrderables() {
    when(archiveProductService.searchArchivedProducts(facilityId)).thenReturn(newHashSet());

    Page<SiglusOrderableDto> orderableDtoPage = siglusOrderableService
        .searchOrderables(searchParams, pageable, facilityId);

    assertEquals(1, orderableDtoPage.getContent().size());
    orderableDtoPage.getContent().forEach(orderable -> {
      assertFalse(orderable.getArchived());
      assertEquals(1, orderable.getPrograms().size());
      orderable.getPrograms().forEach(programOrderable ->
          assertEquals(parentId, programOrderable.getParentId()));
    });
  }

  @Test
  public void shouldReturnDataWithArchivedWhenSearchOrderables() {
    when(archiveProductService.searchArchivedProducts(facilityId))
        .thenReturn(newHashSet(orderableId.toString()));

    Page<SiglusOrderableDto> orderableDtoPage = siglusOrderableService
        .searchOrderables(searchParams, pageable, facilityId);

    assertEquals(1, orderableDtoPage.getContent().size());
    orderableDtoPage.getContent().forEach(orderable -> assertTrue(orderable.getArchived()));
  }

  @Test
  public void shouldCallFindExpirationDateWhenGetOrderableExpirationDate() {
    Set<UUID> orderableIds = newHashSet(orderableId);

    siglusOrderableService.getOrderableExpirationDate(orderableIds);

    verify(orderableRepository).findExpirationDate(orderableIds);
  }
}
