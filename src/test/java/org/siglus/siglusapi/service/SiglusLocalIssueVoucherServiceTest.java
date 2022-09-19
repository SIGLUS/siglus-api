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
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOCAL_ISSUE_VOUCHER_ID_INVALID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOCAL_ISSUE_VOUCHER_SUB_DRAFTS_MORE_THAN_TEN;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_ORDER_CODE_EXISTS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.List;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.service.OrderSearchParams;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.siglus.siglusapi.domain.LocalIssueVoucher;
import org.siglus.siglusapi.domain.LocalIssueVoucherSubDraftLineItem;
import org.siglus.siglusapi.domain.LocalIssueVoucherSubDraft;
import org.siglus.siglusapi.dto.LocalIssueVoucherSubDraftLineItemDto;
import org.siglus.siglusapi.dto.LocalIssueVoucherDto;
import org.siglus.siglusapi.dto.LocalIssueVoucherSubDraftDto;
import org.siglus.siglusapi.dto.enums.PodSubDraftStatusEnum;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.LocalIssueVoucherDraftLineItemRepository;
import org.siglus.siglusapi.repository.LocalIssueVoucherSubDraftRepository;
import org.siglus.siglusapi.repository.PodSubDraftRepository;
import org.siglus.siglusapi.repository.SiglusLocalIssueVoucherRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.OperateTypeEnum;
import org.siglus.siglusapi.web.response.PodSubDraftsSummaryResponse.SubDraftInfo;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@RunWith(MockitoJUnitRunner.class)
public class SiglusLocalIssueVoucherServiceTest {

  @InjectMocks
  private SiglusLocalIssueVoucherService service;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private LocalIssueVoucherDraftLineItemRepository localIssueVoucherDraftLineItemRepository;

  @Mock
  private OrderController orderController;

  @Mock
  private PodSubDraftRepository podSubDraftRepository;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private SiglusLocalIssueVoucherRepository localIssueVoucherRepository;

  @Mock
  private SiglusOrderableService siglusOrderableService;

  @Mock
  private LocalIssueVoucherSubDraftRepository localIssueVoucherSubDraftRepository;

  private final UUID orderableId = UUID.randomUUID();
  private final UUID orderableId2 = UUID.randomUUID();
  private final UUID podId = UUID.randomUUID();
  private final UUID subDraftId = UUID.randomUUID();
  private final UUID lineItemId1 = UUID.randomUUID();
  private final String orderCode = "code-1";
  private final UUID programId = UUID.randomUUID();
  private final UUID operatorId = UUID.randomUUID();
  private final UUID requestingFacilityId = UUID.randomUUID();
  private final UUID supplyingFacilityId = UUID.randomUUID();
  private final UUID localIssueVoucherId = UUID.randomUUID();
  private final UUID localIssueVoucherSubDraftId = UUID.randomUUID();
  private final LocalIssueVoucherDto localIssueVoucherDto = LocalIssueVoucherDto.builder()
      .orderCode(orderCode)
      .status(OrderStatus.SHIPPED)
      .programId(programId)
      .requestingFacilityId(requestingFacilityId)
      .supplyingFacilityId(supplyingFacilityId)
      .build();
  private final LocalIssueVoucher localIssueVoucher = LocalIssueVoucher.builder()
      .orderCode(orderCode)
      .status(OrderStatus.SHIPPED)
      .programId(programId)
      .requestingFacilityId(requestingFacilityId)
      .supplyingFacilityId(supplyingFacilityId)
      .build();
  private final BasicOrderDto basicOrderDto = new BasicOrderDto();

  @Test
  public void shouleUpdateWhenSaveCallByService() {
    // given
    LocalIssueVoucherSubDraft subDraft = new LocalIssueVoucherSubDraft();
    subDraft.setStatus(PodSubDraftStatusEnum.NOT_YET_STARTED);
    subDraft.setLocalIssueVoucherId(localIssueVoucherId);
    LocalIssueVoucherSubDraftDto subDraftDto = new LocalIssueVoucherSubDraftDto();
    subDraftDto.setOperateType(OperateTypeEnum.SAVE);
    subDraftDto.setLocalIssueVoucherId(localIssueVoucherId);
    subDraftDto.setLocalIssueVoucherSubDraftId(localIssueVoucherSubDraftId);
    LocalIssueVoucherSubDraftLineItemDto localIssueVoucherSubDraftLineItem = new LocalIssueVoucherSubDraftLineItemDto();
    localIssueVoucherSubDraftLineItem.setOrderableId(orderableId);
    subDraftDto.setLineItems(newArrayList(localIssueVoucherSubDraftLineItem));
    //when
    when(localIssueVoucherSubDraftRepository.findOne(subDraftId)).thenReturn(subDraft);
    when(localIssueVoucherDraftLineItemRepository.save(
        LocalIssueVoucherSubDraftLineItemDto.to(subDraftDto.getLineItems()))).thenReturn(null);
    when(localIssueVoucherDraftLineItemRepository.findDuplicatedOrderableLineItem(newArrayList(orderableId),
        localIssueVoucherId, localIssueVoucherSubDraftId)).thenReturn(null);
    service.updateSubDraft(localIssueVoucherId, subDraftDto, subDraftId);

    //then
    verify(localIssueVoucherSubDraftRepository).save(subDraft);
  }

  @Test
  public void shouleUpdateWhenSubmitCallByService() {
    // given
    LocalIssueVoucherSubDraft subDraft = new LocalIssueVoucherSubDraft();
    subDraft.setStatus(PodSubDraftStatusEnum.NOT_YET_STARTED);
    subDraft.setLocalIssueVoucherId(localIssueVoucherId);
    LocalIssueVoucherSubDraftDto subDraftDto = new LocalIssueVoucherSubDraftDto();
    subDraftDto.setOperateType(OperateTypeEnum.SUBMIT);
    subDraftDto.setLocalIssueVoucherId(localIssueVoucherId);
    subDraftDto.setLocalIssueVoucherSubDraftId(localIssueVoucherSubDraftId);
    LocalIssueVoucherSubDraftLineItemDto localIssueVoucherSubDraftLineItemDto = new LocalIssueVoucherSubDraftLineItemDto();
    localIssueVoucherSubDraftLineItemDto.setOrderableId(orderableId);
    subDraftDto.setLineItems(newArrayList(localIssueVoucherSubDraftLineItemDto));
    //when
    when(localIssueVoucherSubDraftRepository.findOne(subDraftId)).thenReturn(subDraft);
    when(localIssueVoucherDraftLineItemRepository.save(
        LocalIssueVoucherSubDraftLineItemDto.to(subDraftDto.getLineItems()))).thenReturn(null);
    when(localIssueVoucherDraftLineItemRepository.findDuplicatedOrderableLineItem(newArrayList(orderableId),
        localIssueVoucherId, localIssueVoucherSubDraftId)).thenReturn(null);
    service.updateSubDraft(localIssueVoucherId, subDraftDto, subDraftId);

    //then
    verify(localIssueVoucherSubDraftRepository).save(subDraft);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouleThrowExceptionWhenDuplicateOrderable() {
    // given
    LocalIssueVoucherSubDraft subDraft = new LocalIssueVoucherSubDraft();
    subDraft.setStatus(PodSubDraftStatusEnum.NOT_YET_STARTED);
    subDraft.setLocalIssueVoucherId(localIssueVoucherId);
    LocalIssueVoucherSubDraftDto subDraftDto = new LocalIssueVoucherSubDraftDto();
    subDraftDto.setOperateType(OperateTypeEnum.SUBMIT);
    subDraftDto.setLocalIssueVoucherId(localIssueVoucherId);
    subDraftDto.setLocalIssueVoucherSubDraftId(localIssueVoucherSubDraftId);
    LocalIssueVoucherSubDraftLineItemDto localIssueVoucherSubDraftLineItemDto = new LocalIssueVoucherSubDraftLineItemDto();
    localIssueVoucherSubDraftLineItemDto.setOrderableId(orderableId);
    subDraftDto.setLineItems(newArrayList(localIssueVoucherSubDraftLineItemDto));
    List<ProofOfDeliveryLineItem> proofOfDeliveryLineItems = buildMockPodLineItems();
    //when
    when(localIssueVoucherSubDraftRepository.findOne(subDraftId)).thenReturn(subDraft);
    when(localIssueVoucherDraftLineItemRepository.save(
        LocalIssueVoucherSubDraftLineItemDto.to(subDraftDto.getLineItems()))).thenReturn(null);
    when(localIssueVoucherDraftLineItemRepository.findDuplicatedOrderableLineItem(newArrayList(orderableId),
        localIssueVoucherId, subDraftId)).thenReturn(proofOfDeliveryLineItems);
    service.updateSubDraft(localIssueVoucherId, subDraftDto, subDraftId);
    //then
    verify(localIssueVoucherSubDraftRepository).save(subDraft);
  }

  @Test(expected = BusinessDataException.class)
  public void shouleThrowExceptionWhenCanNotOperate() {
    // given
    LocalIssueVoucherSubDraft subDraft = new LocalIssueVoucherSubDraft();
    subDraft.setStatus(PodSubDraftStatusEnum.SUBMITTED);
    subDraft.setLocalIssueVoucherId(localIssueVoucherId);
    LocalIssueVoucherSubDraftDto subDraftDto = new LocalIssueVoucherSubDraftDto();
    subDraftDto.setOperateType(OperateTypeEnum.SUBMIT);
    subDraftDto.setLocalIssueVoucherId(localIssueVoucherId);
    subDraftDto.setLocalIssueVoucherSubDraftId(localIssueVoucherSubDraftId);
    LocalIssueVoucherSubDraftLineItemDto localIssueVoucherSubDraftLineItemDto = new LocalIssueVoucherSubDraftLineItemDto();
    localIssueVoucherSubDraftLineItemDto.setOrderableId(orderableId);
    subDraftDto.setLineItems(newArrayList(localIssueVoucherSubDraftLineItemDto));
    List<ProofOfDeliveryLineItem> proofOfDeliveryLineItems = buildMockPodLineItems();
    //when
    when(localIssueVoucherSubDraftRepository.findOne(subDraftId)).thenReturn(subDraft);
    when(localIssueVoucherDraftLineItemRepository.save(
        LocalIssueVoucherSubDraftLineItemDto.to(subDraftDto.getLineItems()))).thenReturn(null);
    when(localIssueVoucherDraftLineItemRepository.findDuplicatedOrderableLineItem(newArrayList(orderableId),
        localIssueVoucherId, localIssueVoucherSubDraftId)).thenReturn(proofOfDeliveryLineItems);
    service.updateSubDraft(localIssueVoucherId, subDraftDto, subDraftId);
    //then
    verify(localIssueVoucherSubDraftRepository).save(subDraft);
  }

  @Test(expected = InputMismatchException.class)
  public void shouleThrowExceptionWhenIdNotMatchSubDraftId() {
    // given
    LocalIssueVoucherSubDraft subDraft = new LocalIssueVoucherSubDraft();
    subDraft.setStatus(PodSubDraftStatusEnum.SUBMITTED);
    subDraft.setLocalIssueVoucherId(localIssueVoucherId);
    subDraft.setLocalIssueVoucherId(localIssueVoucherId);
    LocalIssueVoucherSubDraftDto subDraftDto = new LocalIssueVoucherSubDraftDto();
    subDraftDto.setOperateType(OperateTypeEnum.SUBMIT);
    subDraftDto.setLocalIssueVoucherId(localIssueVoucherId);
    subDraftDto.setLocalIssueVoucherSubDraftId(localIssueVoucherSubDraftId);
    LocalIssueVoucherSubDraftLineItemDto localIssueVoucherSubDraftLineItemDto = new LocalIssueVoucherSubDraftLineItemDto();
    localIssueVoucherSubDraftLineItemDto.setOrderableId(orderableId);
    subDraftDto.setLineItems(newArrayList(localIssueVoucherSubDraftLineItemDto));
    List<ProofOfDeliveryLineItem> proofOfDeliveryLineItems = buildMockPodLineItems();
    //when
    when(localIssueVoucherSubDraftRepository.findOne(subDraftId)).thenReturn(subDraft);
    when(localIssueVoucherDraftLineItemRepository.save(
        LocalIssueVoucherSubDraftLineItemDto.to(subDraftDto.getLineItems()))).thenReturn(null);
    when(localIssueVoucherDraftLineItemRepository.findDuplicatedOrderableLineItem(newArrayList(orderableId),
        localIssueVoucherId, localIssueVoucherSubDraftId)).thenReturn(proofOfDeliveryLineItems);
    service.updateSubDraft(UUID.randomUUID(), subDraftDto, subDraftId);
  }

  @Test
  public void shouldClearFillingPageWhenCallByService() {
    LocalIssueVoucherSubDraft subDraft = new LocalIssueVoucherSubDraft();
    subDraft.setStatus(PodSubDraftStatusEnum.DRAFT);
    //when
    when(localIssueVoucherSubDraftRepository.findOne(subDraftId)).thenReturn(subDraft);
    service.clearFillingPage(subDraftId);
    //then
    verify(localIssueVoucherSubDraftRepository).save(subDraft);
  }

  @Test
  public void shouldGetLineItemWhenCallByService() {
    LocalIssueVoucherSubDraftLineItem localIssueVoucherSubDraftLineItemDto1 = new LocalIssueVoucherSubDraftLineItem();
    LocalIssueVoucherSubDraftLineItem localIssueVoucherSubDraftLineItemDto2 = new LocalIssueVoucherSubDraftLineItem();
    ArrayList<LocalIssueVoucherSubDraftLineItem> localIssueVoucherSubDraftLineItemDtos = newArrayList(
        localIssueVoucherSubDraftLineItemDto1, localIssueVoucherSubDraftLineItemDto2);
    //when
    when(localIssueVoucherDraftLineItemRepository.findByLocalIssueVoucherSubDraftId(subDraftId)).thenReturn(
        localIssueVoucherSubDraftLineItemDtos);
    LocalIssueVoucherSubDraftDto subDraftDetail = service.getSubDraftDetail(subDraftId);
    //then
    assertEquals(2, subDraftDetail.getLineItems().size());
  }

  @Test
  public void shouldCreateLocalIssueVoucher() {
    //when
    localIssueVoucher.setId(localIssueVoucherId);

    //when
    when(orderController.searchOrders(any(OrderSearchParams.class), any(PageRequest.class)))
        .thenReturn(new PageImpl(Collections.emptyList()));
    when(localIssueVoucherRepository
        .findByOrderCodeAndProgramIdAndRequestingFacilityIdAndSupplyingFacilityId(orderCode, programId,
            requestingFacilityId, supplyingFacilityId)).thenReturn(Collections.emptyList());
    when(localIssueVoucherRepository.save(any(LocalIssueVoucher.class))).thenReturn(localIssueVoucher);

    LocalIssueVoucherDto localIssueVoucher = service.createLocalIssueVoucher(localIssueVoucherDto);

    //then
    assertEquals(localIssueVoucher.getId(), localIssueVoucherId);
    assertEquals(localIssueVoucher.getProgramId(), programId);
    assertEquals(localIssueVoucher.getOrderCode(), orderCode);
    assertEquals(localIssueVoucher.getRequestingFacilityId(), requestingFacilityId);
    assertEquals(localIssueVoucher.getSupplyingFacilityId(), supplyingFacilityId);
  }

  @Test
  public void shouldThrowExceptionWhenHasSameOrderCodeInBasicOrderDto() {
    //then
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_ORDER_CODE_EXISTS));

    //given
    basicOrderDto.setOrderCode(orderCode);
    ArrayList<BasicOrderDto> basicOrderDtos = Lists.newArrayList(basicOrderDto);

    //when
    when(orderController.searchOrders(any(OrderSearchParams.class), any(PageRequest.class)))
        .thenReturn(new PageImpl(basicOrderDtos));
    when(localIssueVoucherRepository
        .findByOrderCodeAndProgramIdAndRequestingFacilityIdAndSupplyingFacilityId(orderCode, programId,
            requestingFacilityId, supplyingFacilityId)).thenReturn(Collections.emptyList());

    service.createLocalIssueVoucher(localIssueVoucherDto);
  }

  @Test
  public void shouldThrowExceptionWhenHasSameCodeInLocalIssueVoucherList() {
    //then
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_ORDER_CODE_EXISTS));

    //when
    when(orderController.searchOrders(any(OrderSearchParams.class), any(PageRequest.class)))
        .thenReturn(new PageImpl(Collections.emptyList()));
    when(localIssueVoucherRepository
        .findByOrderCodeAndProgramIdAndRequestingFacilityIdAndSupplyingFacilityId(orderCode, programId,
            requestingFacilityId, supplyingFacilityId)).thenReturn(newArrayList(localIssueVoucher));

    service.createLocalIssueVoucher(localIssueVoucherDto);
  }

  @Test
  public void shouldDeleteLocalIssueVoucher() {
    //when
    when(localIssueVoucherRepository.findOne(localIssueVoucherId)).thenReturn(localIssueVoucher);

    service.deleteLocalIssueVoucher(localIssueVoucherId);

    //then
    verify(localIssueVoucherDraftLineItemRepository).deleteAllByLocalIssueVoucherId(localIssueVoucherId);
    verify(localIssueVoucherSubDraftRepository).deleteAllByLocalIssueVoucherId(localIssueVoucherId);
    verify(localIssueVoucherRepository).delete(localIssueVoucherId);
  }

  @Test
  public void shouldThrowExceptionWhenDeleteLocalIssueVoucher() {
    //then
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_LOCAL_ISSUE_VOUCHER_ID_INVALID));

    //when
    when(localIssueVoucherRepository.findOne(localIssueVoucherId)).thenReturn(null);

    service.deleteLocalIssueVoucher(localIssueVoucherId);
  }

  @Test
  public void shouldCreateLocalIssueVoucherSubDraft() {
    //given
    LocalIssueVoucherSubDraft localIssueVoucherSubDraft = LocalIssueVoucherSubDraft.builder()
        .localIssueVoucherId(localIssueVoucherId)
        .number(6)
        .status(PodSubDraftStatusEnum.NOT_YET_STARTED)
        .operatorId(operatorId)
        .build();

    //when
    when(localIssueVoucherRepository.findOne(localIssueVoucherId)).thenReturn(localIssueVoucher);
    when(localIssueVoucherSubDraftRepository.countAllByLocalIssueVoucherId(localIssueVoucherId)).thenReturn(5);
    String operator = "Jimmy";
    when(authenticationHelper.getUserNameByUserId(operatorId)).thenReturn(operator);
    when(localIssueVoucherSubDraftRepository.save(any(LocalIssueVoucherSubDraft.class))).thenReturn(localIssueVoucherSubDraft);

    SubDraftInfo savedLocalIssueVoucherSubDraft = service.createLocalIssueVoucherSubDraft(localIssueVoucherId);

    //then
    assertEquals(PodSubDraftStatusEnum.NOT_YET_STARTED, savedLocalIssueVoucherSubDraft.getStatus());
    assertEquals(6, savedLocalIssueVoucherSubDraft.getGroupNum());
    assertEquals(operator, savedLocalIssueVoucherSubDraft.getSaver());
  }

  @Test
  public void shouldThrowExceptionWhenSubDraftQuantityMoreThanTen() {
    //then
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_LOCAL_ISSUE_VOUCHER_SUB_DRAFTS_MORE_THAN_TEN));

    //when
    when(localIssueVoucherRepository.findOne(localIssueVoucherId)).thenReturn(localIssueVoucher);
    when(localIssueVoucherSubDraftRepository.countAllByLocalIssueVoucherId(localIssueVoucherId)).thenReturn(10);

    service.createLocalIssueVoucherSubDraft(localIssueVoucherId);
  }

  @Test
  public void shouldReturnAvailableOrderablesWhenCallByService() {
    org.openlmis.referencedata.dto.OrderableDto orderableDto1 = new org.openlmis.referencedata.dto.OrderableDto();
    org.openlmis.referencedata.dto.OrderableDto orderableDto2 = new org.openlmis.referencedata.dto.OrderableDto();
    orderableDto1.setId(orderableId);
    orderableDto2.setId(orderableId2);
    when(localIssueVoucherDraftLineItemRepository.findUsedOrderableByPodId(podId)).thenReturn(
        Collections.singletonList(orderableId));
    when(siglusOrderableService.getAllProducts()).thenReturn(Lists.newArrayList(orderableDto1, orderableDto2));
    assertEquals(1, service.getAvailableOrderables(podId).size());
  }

  @Test
  public void shouldReturnAvailableOrderablesWhenCallByServiceAndNoUsedOrderables() {
    org.openlmis.referencedata.dto.OrderableDto orderableDto1 = new org.openlmis.referencedata.dto.OrderableDto();
    org.openlmis.referencedata.dto.OrderableDto orderableDto2 = new org.openlmis.referencedata.dto.OrderableDto();
    orderableDto1.setId(orderableId);
    orderableDto2.setId(orderableId2);
    when(localIssueVoucherDraftLineItemRepository.findUsedOrderableByPodId(podId)).thenReturn(null);
    when(siglusOrderableService.getAllProducts()).thenReturn(Lists.newArrayList(orderableDto1, orderableDto2));
    assertEquals(2, service.getAvailableOrderables(podId).size());
  }

  private List<ProofOfDeliveryLineItem> buildMockPodLineItems() {
    ProofOfDeliveryLineItem lineItem = new ProofOfDeliveryLineItem(null, UUID.randomUUID(), 10, null, 0,
        UUID.randomUUID(), "test notes");
    lineItem.setId(lineItemId1);
    return Lists.newArrayList(lineItem);
  }
}