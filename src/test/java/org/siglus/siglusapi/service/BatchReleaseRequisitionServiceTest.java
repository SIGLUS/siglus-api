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

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_REQUISITION_EXPIRED;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import junit.framework.TestCase;
import org.javers.common.collections.Lists;
import org.javers.common.collections.Sets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ReleasableRequisitionBatchDto;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.web.BatchRequisitionController;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.repository.OrdersRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;

@RunWith(MockitoJUnitRunner.class)
public class BatchReleaseRequisitionServiceTest extends TestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private BatchReleaseRequisitionService batchReleaseRequisitionService;

  @Mock
  private SiglusRequisitionRepository siglusRequisitionRepository;

  @Mock
  private OrdersRepository ordersRepository;

  @Mock
  private SiglusProcessingPeriodReferenceDataService siglusProcessingPeriodReferenceDataService;

  @Mock
  private BatchRequisitionController batchRequisitionController;

  private final UUID requisitionId1 = UUID.randomUUID();
  private final UUID requisitionId2 = UUID.randomUUID();
  private final UUID supplyingDepotId = UUID.randomUUID();
  private final UUID programId1 = UUID.randomUUID();
  private final UUID processingPeriodId1 = UUID.randomUUID();
  private final UUID processingPeriodId2 = UUID.randomUUID();
  private final List<OrderStatus> orderStatuses = Lists
      .asList(OrderStatus.ORDERED, OrderStatus.FULFILLING, OrderStatus.PARTIALLY_FULFILLED);
  private final ReleasableRequisitionBatchDto releasableRequisitionBatchDto = new ReleasableRequisitionBatchDto();
  private final LocalDate endDate1 = LocalDate.of(2022, 8, 20);
  private final LocalDate endDate2 = LocalDate.of(2022, 9, 20);


  @Test
  public void shouldThrowIllegalArgumentExceptionWhenRequisitionListIsEmpty() {
    //then
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("not found releasable requisition"));

    //given
    releasableRequisitionBatchDto.setRequisitionsToRelease(emptyList());

    //when
    batchReleaseRequisitionService.getRequisitionsProcessingStatusDtoResponse(releasableRequisitionBatchDto);
  }

  @Test
  public void shouldThrowBusinessDataExceptionWhenRequisitionExpired() {
    //then
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_REQUISITION_EXPIRED));

    //given
    when(siglusRequisitionRepository.findAll(Sets.asSet(requisitionId1, requisitionId2)))
        .thenReturn(createRequisitionWithSameProgramIdList());
    when(ordersRepository.findBySupplyingFacilityIdAndProgramIdAndStatusIn(supplyingDepotId, programId1, orderStatuses))
        .thenReturn(createOrderWithDifferentPeriod());
    when(siglusProcessingPeriodReferenceDataService.findByIds(Sets.asSet(processingPeriodId1, processingPeriodId2)))
        .thenReturn(createDifferentProcessingPeriodDtos());

    //when
    batchReleaseRequisitionService.getRequisitionsProcessingStatusDtoResponse(createReleasableRequisitionBatchDto());
  }

  @Test
  public void shouldDoNothingWhenOrderListIsEmpty() {
    //given
    when(siglusRequisitionRepository.findAll(Sets.asSet(requisitionId1, requisitionId2)))
        .thenReturn(createRequisitionWithSameProgramIdList());
    when(ordersRepository.findBySupplyingFacilityIdAndProgramIdAndStatusIn(supplyingDepotId, programId1, orderStatuses))
        .thenReturn(emptyList());

    //when
    batchReleaseRequisitionService.getRequisitionsProcessingStatusDtoResponse(createReleasableRequisitionBatchDto());

    //then
    verify(batchRequisitionController).batchReleaseRequisitions(createReleasableRequisitionBatchDto());
  }

  @Test
  public void shouldUpdateOrderStatusWhenOrderExpired() {
    //when
    ProcessingPeriodDto processingPeriodDto1 = new ProcessingPeriodDto();
    processingPeriodDto1.setId(processingPeriodId1);
    processingPeriodDto1.setEndDate(endDate1);

    ProcessingPeriodDto processingPeriodDto2 = new ProcessingPeriodDto();
    processingPeriodDto2.setId(processingPeriodId1);
    processingPeriodDto2.setEndDate(endDate2);

    Order order = new Order();
    order.setSupplyingFacilityId(supplyingDepotId);
    order.setStatus(OrderStatus.CLOSED);
    order.setProcessingPeriodId(processingPeriodId1);
    //given
    when(siglusRequisitionRepository.findAll(Sets.asSet(requisitionId1, requisitionId2)))
        .thenReturn(createRequisitionWithSameProgramIdList());
    when(siglusRequisitionRepository.findAll(Sets.asSet(requisitionId2)))
        .thenReturn(createMaxPeriodRequisition());
    when(ordersRepository.findBySupplyingFacilityIdAndProgramIdAndStatusIn(supplyingDepotId, programId1, orderStatuses))
        .thenReturn(createOrderWithSamePeriod());

    when(siglusProcessingPeriodReferenceDataService.findByIds(Sets.asSet(processingPeriodId1)))
        .thenReturn(Lists.asList(processingPeriodDto1));
    when(siglusProcessingPeriodReferenceDataService.findByIds(Sets.asSet(processingPeriodId2)))
        .thenReturn(Lists.asList(processingPeriodDto2));

    when(siglusProcessingPeriodReferenceDataService.findOne(processingPeriodId1)).thenReturn(processingPeriodDto1);

    //when
    batchReleaseRequisitionService
        .getRequisitionsProcessingStatusDtoResponse(createMaxPeriodReleasableRequisitionBatchDto());

    //then
    verify(ordersRepository, times(1)).save(Lists.asList(order, order));
    verify(batchRequisitionController).batchReleaseRequisitions(createMaxPeriodReleasableRequisitionBatchDto());
  }

  private List<ProcessingPeriodDto> createDifferentProcessingPeriodDtos() {

    ProcessingPeriodDto processingPeriodDto1 = new ProcessingPeriodDto();
    processingPeriodDto1.setId(processingPeriodId1);
    processingPeriodDto1.setEndDate(endDate1);

    ProcessingPeriodDto processingPeriodDto2 = new ProcessingPeriodDto();
    processingPeriodDto2.setId(processingPeriodId2);
    processingPeriodDto2.setEndDate(endDate2);

    return Lists.asList(processingPeriodDto1, processingPeriodDto2);
  }

  private List<ProcessingPeriodDto> createSameProcessingPeriodDtos() {

    ProcessingPeriodDto processingPeriodDto1 = new ProcessingPeriodDto();
    processingPeriodDto1.setId(processingPeriodId1);
    processingPeriodDto1.setEndDate(endDate1);

    ProcessingPeriodDto processingPeriodDto2 = new ProcessingPeriodDto();
    processingPeriodDto2.setId(processingPeriodId1);
    processingPeriodDto2.setEndDate(endDate1);

    return Lists.asList(processingPeriodDto1, processingPeriodDto2);
  }

  private List<Order> createOrderWithDifferentPeriod() {
    Order order1 = new Order();
    order1.setSupplyingFacilityId(supplyingDepotId);
    order1.setStatus(OrderStatus.FULFILLING);
    order1.setProcessingPeriodId(processingPeriodId1);

    Order order2 = new Order();
    order2.setSupplyingFacilityId(supplyingDepotId);
    order2.setStatus(OrderStatus.PARTIALLY_FULFILLED);
    order2.setProcessingPeriodId(processingPeriodId2);

    return Lists.asList(order1, order2);
  }

  private List<Order> createOrderWithSamePeriod() {
    Order order1 = new Order();
    order1.setSupplyingFacilityId(supplyingDepotId);
    order1.setStatus(OrderStatus.FULFILLING);
    order1.setProcessingPeriodId(processingPeriodId1);

    Order order2 = new Order();
    order2.setSupplyingFacilityId(supplyingDepotId);
    order2.setStatus(OrderStatus.PARTIALLY_FULFILLED);
    order2.setProcessingPeriodId(processingPeriodId1);

    return Lists.asList(order1, order2);
  }

  private List<Requisition> createRequisitionWithSameProgramIdList() {
    Requisition requisition1 = new Requisition();
    requisition1.setId(requisitionId1);
    requisition1.setProgramId(programId1);
    requisition1.setProcessingPeriodId(processingPeriodId1);

    Requisition requisition2 = new Requisition();
    requisition2.setId(requisitionId2);
    requisition2.setProgramId(programId1);
    requisition2.setProcessingPeriodId(processingPeriodId2);

    return Lists.asList(requisition1, requisition2);
  }

  private List<Requisition> createMaxPeriodRequisition() {
    Requisition requisition2 = new Requisition();
    requisition2.setId(requisitionId2);
    requisition2.setProgramId(programId1);
    requisition2.setProcessingPeriodId(processingPeriodId2);

    return Lists.asList(requisition2);
  }

  private ReleasableRequisitionBatchDto createReleasableRequisitionBatchDto() {
    ReleasableRequisitionDto releasableRequisitionDto1 = new ReleasableRequisitionDto();
    releasableRequisitionDto1.setRequisitionId(requisitionId1);
    releasableRequisitionDto1.setSupplyingDepotId(supplyingDepotId);

    ReleasableRequisitionDto releasableRequisitionDto2 = new ReleasableRequisitionDto();
    releasableRequisitionDto2.setRequisitionId(requisitionId2);
    releasableRequisitionDto2.setSupplyingDepotId(supplyingDepotId);
    releasableRequisitionBatchDto
        .setRequisitionsToRelease(Lists.asList(releasableRequisitionDto1, releasableRequisitionDto2));

    return releasableRequisitionBatchDto;
  }

  private ReleasableRequisitionBatchDto createMaxPeriodReleasableRequisitionBatchDto() {

    ReleasableRequisitionDto releasableRequisitionDto = new ReleasableRequisitionDto();
    releasableRequisitionDto.setRequisitionId(requisitionId2);
    releasableRequisitionDto.setSupplyingDepotId(supplyingDepotId);
    releasableRequisitionBatchDto.setRequisitionsToRelease(Lists.asList(releasableRequisitionDto));

    return releasableRequisitionBatchDto;
  }


}