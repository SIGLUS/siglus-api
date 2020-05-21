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

package org.openlmis.requisition.utils;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.javers.common.collections.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionLineItemDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProcessingScheduleDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.dto.RequisitionLineItemDto;
import org.openlmis.requisition.service.referencedata.FacilityTypeApprovedProductReferenceDataService;
import org.openlmis.requisition.service.referencedata.OrderableReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.testutils.ApprovedProductDtoDataBuilder;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.testutils.ProgramOrderableDtoDataBuilder;


@SuppressWarnings({"PMD.TooManyMethods"})
@RunWith(MockitoJUnitRunner.class)
public class RequisitionExportHelperTest {

  private static final long PACK_SIZE = 2;

  private Requisition requisition;
  private OrderableDto orderableDto;
  private ApprovedProductDto approvedProductDto;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private ProcessingPeriodDto periodDto1;

  @Mock
  private ProcessingPeriodDto periodDto2;

  @Mock
  private ProcessingPeriodDto periodDto3;

  @Mock
  private OrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private FacilityTypeApprovedProductReferenceDataService
      facilityTypeApprovedProductReferenceDataService;

  @InjectMocks
  private RequisitionExportHelper requisitionExportHelper;

  @Captor
  private ArgumentCaptor<Set<VersionEntityReference>> argumentCaptor;

  private UUID program = UUID.randomUUID();
  private UUID period1 = UUID.randomUUID();
  private UUID period2 = UUID.randomUUID();
  private UUID period3 = UUID.randomUUID();
  private UUID productId = UUID.randomUUID();
  private UUID approvedProductId = UUID.randomUUID();

  @Before
  public void setUp() {
    generateInstances();
    mockRepositories();
  }

  @Test
  public void shouldExportRequisitionLinesToDtos() {
    RequisitionLineItem requisitionLineItem =
        generateRequisitionLineItemToExport(orderableDto.getId(), approvedProductDto.getId());

    when(orderableReferenceDataService.findByIdentities(argumentCaptor.capture()))
        .thenReturn(Collections.singletonList(orderableDto));
    when(facilityTypeApprovedProductReferenceDataService.findByIdentities(
        argumentCaptor.capture())).thenReturn(Collections.singletonList(approvedProductDto));

    List<RequisitionLineItemDto> items =
        requisitionExportHelper.exportToDtos(singletonList(requisitionLineItem));
    RequisitionLineItemDto item = items.get(0);
    assertNotNull(item);
    assertEquals(item.getId(), requisitionLineItem.getId());
    assertEquals(item.getOrderableIdentity().getId(), requisitionLineItem.getOrderable().getId());
    assertEquals(item.getBeginningBalance(), requisitionLineItem.getBeginningBalance());
    assertEquals(item.getTotalReceivedQuantity(), requisitionLineItem.getTotalReceivedQuantity());
    assertEquals(item.getTotalLossesAndAdjustments(),
        requisitionLineItem.getTotalLossesAndAdjustments());
    assertEquals(item.getStockOnHand(), requisitionLineItem.getStockOnHand());
    assertEquals(item.getRequestedQuantity(), requisitionLineItem.getRequestedQuantity());
    assertEquals(item.getTotalConsumedQuantity(), requisitionLineItem.getTotalConsumedQuantity());
    assertEquals(item.getRequestedQuantityExplanation(),
        requisitionLineItem.getRequestedQuantityExplanation());
    assertEquals(item.getRemarks(), requisitionLineItem.getRemarks());
    assertEquals(item.getApprovedQuantity(), requisitionLineItem.getApprovedQuantity());
    assertEquals(item.getTotalStockoutDays(), requisitionLineItem.getTotalStockoutDays());
    assertEquals(item.getTotal(), requisitionLineItem.getTotal());
    assertEquals(item.getNumberOfNewPatientsAdded(),
        requisitionLineItem.getNumberOfNewPatientsAdded());

    List<Set<VersionEntityReference>> searchedIdentities = argumentCaptor.getAllValues();
    assertThat(searchedIdentities, hasSize(2));
    assertThat(searchedIdentities,
        hasItem(Sets.asSet(new VersionEntityReference(orderableDto.getId(),
            orderableDto.getVersionNumber()))));
    assertThat(searchedIdentities,
        hasItem(Sets.asSet(new VersionEntityReference(approvedProductDto.getId(),
            approvedProductDto.getVersionNumber()))));
  }

  @Test
  public void exportShouldNotSetOrderableIfNoneReturned() {
    when(orderableReferenceDataService.findByIdentities(argumentCaptor.capture()))
        .thenReturn(Collections.emptyList());
    when(facilityTypeApprovedProductReferenceDataService.findByIdentities(
        argumentCaptor.capture())).thenReturn(Collections.emptyList());

    RequisitionLineItem requisitionLineItem =
        generateRequisitionLineItemToExport(orderableDto.getId(), approvedProductDto.getId());
    List<RequisitionLineItemDto> items =
        requisitionExportHelper.exportToDtos(singletonList(requisitionLineItem));
    RequisitionLineItemDto item = items.get(0);
    assertNotNull(item);

    assertNull(item.getOrderable());

    List<Set<VersionEntityReference>> searchedIdentities = argumentCaptor.getAllValues();
    assertThat(searchedIdentities, hasSize(2));
    assertThat(searchedIdentities,
        hasItem(Sets.asSet(new VersionEntityReference(orderableDto.getId(),
            orderableDto.getVersionNumber()))));
    assertThat(searchedIdentities,
        hasItem(Sets.asSet(new VersionEntityReference(approvedProductDto.getId(),
            approvedProductDto.getVersionNumber()))));
  }

  private RequisitionLineItem generateRequisitionLineItemToExport(UUID orderableDtoUuid,
      UUID approvedProductDtoId) {
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDtoDataBuilder()
        .withProgramId(program)
        .buildAsDto();
    Set<ProgramOrderableDto> products = new HashSet<>();
    products.add(programOrderableDto);
    orderableDto.setPrograms(products);

    return new RequisitionLineItemDataBuilder()
        .withRequisition(requisition)
        .withOrderable(orderableDtoUuid, 1L)
        .withFacilityTypeApprovedProduct(approvedProductDtoId, 1L)
        .withId(UUID.randomUUID())
        .withBeginningBalance(3)
        .withTotalReceivedQuantity(4)
        .withTotalLossesAndAdjustments(0)
        .withStockOnHand(1)
        .withRequestedQuantity(5)
        .withTotalConsumedQuantity(2)
        .withTotal(7)
        .withApprovedQuantity(5)
        .withTotalStockoutDays(6)
        .withNumberOfNewPatientsAdded(8)
        .build();
  }

  private void generateInstances() {
    requisition = createTestRequisition(UUID.randomUUID(), period1, program,
        RequisitionStatus.INITIATED);
    RequisitionLineItem requisitionLineItem = createTestRequisitionLineItem(
        10, 20, requisition
    );

    requisition.setRequisitionLineItems(new ArrayList<>(
        singletonList(requisitionLineItem)));
    orderableDto = new OrderableDtoDataBuilder()
        .withVersionNumber(1L)
        .withNetContent(PACK_SIZE)
        .buildAsDto();
    approvedProductDto = new ApprovedProductDtoDataBuilder()
        .withVersionNumber(1L)
        .buildAsDto();
  }

  private Requisition createTestRequisition(UUID facility, UUID period,
                                            UUID program, RequisitionStatus requisitionStatus) {
    return new RequisitionDataBuilder()
        .withFacilityId(facility)
        .withProgramId(program)
        .withProcessingPeriodId(period)
        .withStatus(requisitionStatus)
        .withEmergency(false)
        .build();
  }

  private RequisitionLineItem createTestRequisitionLineItem(Integer quantityRequested,
                                                            Integer stockOnHand,
                                                            Requisition requisition) {
    return new RequisitionLineItemDataBuilder()
        .withId(UUID.randomUUID())
        .withRequestedQuantity(quantityRequested)
        .withStockOnHand(stockOnHand)
        .withRequisition(requisition)
        .withOrderable(productId, 1L)
        .withFacilityTypeApprovedProduct(approvedProductId, 1L)
        .build();
  }

  private void mockRepositories() {
    when(programReferenceDataService
        .findOne(any()))
        .thenReturn(new ProgramDto());
    when(periodDto1.getProcessingSchedule())
        .thenReturn(new ProcessingScheduleDto());
    when(periodDto1.getStartDate())
        .thenReturn(LocalDate.of(2016, Month.MARCH, 10));
    when(periodDto2.getStartDate())
        .thenReturn(LocalDate.of(2016, Month.FEBRUARY, 10));
    when(periodDto3.getStartDate())
        .thenReturn(LocalDate.of(2016, Month.JANUARY, 10));
    when(periodDto1.getId())
        .thenReturn(period1);
    when(periodDto2.getId())
        .thenReturn(period2);
    when(periodDto3.getId())
        .thenReturn(period3);
  }
}
