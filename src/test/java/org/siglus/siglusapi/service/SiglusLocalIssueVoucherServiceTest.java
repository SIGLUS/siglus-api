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
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOCAL_ISSUE_VOUCHER_ID_INVALID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOCAL_ISSUE_VOUCHER_SUB_DRAFTS_MORE_THAN_TEN;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_ORDER_CODE_EXISTS;

import com.google.common.collect.Sets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.openlmis.fulfillment.domain.ProofOfDeliveryStatus;
import org.openlmis.fulfillment.service.OrderSearchParams;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.openlmis.fulfillment.web.util.ObjectReferenceDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryLineItemDto;
import org.openlmis.fulfillment.web.util.ShipmentObjectReferenceDto;
import org.openlmis.fulfillment.web.util.VersionObjectReferenceDto;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.repository.StatusChangeRepository;
import org.siglus.siglusapi.domain.LocalIssueVoucher;
import org.siglus.siglusapi.domain.PodLineItemsExtension;
import org.siglus.siglusapi.domain.PodSubDraft;
import org.siglus.siglusapi.domain.ProofsOfDeliveryExtension;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.GeographicLevelDto;
import org.siglus.siglusapi.dto.GeographicZoneDto;
import org.siglus.siglusapi.dto.LocalIssueVoucherDto;
import org.siglus.siglusapi.dto.enums.PodSubDraftStatusEnum;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.PodLineItemsRepository;
import org.siglus.siglusapi.repository.PodSubDraftRepository;
import org.siglus.siglusapi.repository.SiglusLocalIssueVoucherRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.dto.OrderDto;
import org.siglus.siglusapi.repository.dto.PodLineItemDto;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.OperateTypeEnum;
import org.siglus.siglusapi.web.request.PodExtensionRequest;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftRequest;
import org.siglus.siglusapi.web.response.PodExtensionResponse;
import org.siglus.siglusapi.web.response.PodSubDraftsSummaryResponse.SubDraftInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"unused"})
public class SiglusLocalIssueVoucherServiceTest {

  @InjectMocks
  private SiglusLocalIssueVoucherService service;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private PodLineItemsRepository podLineItemsRepository;

  @Mock
  private OrderController orderController;

  @Mock
  private SiglusPodService siglusPodService;

  @Mock
  private PodSubDraftRepository podSubDraftRepository;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private SiglusRequisitionRepository siglusRequisitionRepository;

  @Mock
  private StatusChangeRepository requisitionStatusChangeRepository;

  @Mock
  private SiglusLocalIssueVoucherRepository localIssueVoucherRepository;

  private final UUID externalId = UUID.randomUUID();
  private final UUID orderableId = UUID.randomUUID();
  private final UUID podId = UUID.randomUUID();
  private final UUID subDraftId = UUID.randomUUID();
  private final UUID lineItemId1 = UUID.randomUUID();
  private final UUID lineItemId2 = UUID.randomUUID();
  private final UUID orderId = UUID.randomUUID();
  private final UUID requisitionId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final UUID processingPeriodId = UUID.randomUUID();
  private final UUID lotId = UUID.randomUUID();
  private final UUID orderDtoId = UUID.randomUUID();
  private final UUID shipmentDtoId = UUID.randomUUID();
  private final String productCode = "product code";
  private final String productName = "product name";
  private final String serviceUrl = "serviceUrl";
  private final String facilityCode = "facility code";
  private final String lotCode = "lot code";
  private final String preparedBy = "prepared by user1";
  private final String conferredBy = "conferred by user2";
  private final String orderCode = "code-1";
  private final String operator = "Jimmy";
  private final Set<String> defaultExpands = Sets.newHashSet("shipment.order");
  private final UUID programId = UUID.randomUUID();
  private final UUID operatorId = UUID.randomUUID();
  private final UUID requestingFacilityId = UUID.randomUUID();
  private final UUID supplyingFacilityId = UUID.randomUUID();
  private final UUID localIssueVoucherId = UUID.randomUUID();
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
  private final PodSubDraft localIssueVoucherSubDraft = PodSubDraft.builder()
      .podId(localIssueVoucherId)
      .number(6)
      .status(PodSubDraftStatusEnum.NOT_YET_STARTED)
      .operatorId(operatorId)
      .build();
  private final BasicOrderDto basicOrderDto = new BasicOrderDto();

  @Test
  public void shouleUpdateWhenCallByService() {
    // when
    UpdatePodSubDraftRequest request = new UpdatePodSubDraftRequest();
    request.setPodDto(buildMockPodDtoWithOneLineItem());
    request.setOperateType(OperateTypeEnum.SAVE);
    service.updateSubDraft(request, subDraftId);

    verify(siglusPodService).updateSubDraft(request, subDraftId);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenOrderableDuplicated() {
    UpdatePodSubDraftRequest request = new UpdatePodSubDraftRequest();
    request.setPodDto(buildMockPodDtoWithOneLineItem());
    request.setOperateType(OperateTypeEnum.SAVE);
    List<UUID> orderableIds = request
        .getPodDto()
        .getLineItems()
        .stream()
        .map(proofOfDeliveryLineItemDto -> proofOfDeliveryLineItemDto.getOrderableIdentity().getId())
        .collect(Collectors.toList());
    UUID podId = request.getPodDto().getId();
    when(podLineItemsRepository.findDuplicatedOrderableLineItem(orderableIds, podId, subDraftId))
        .thenReturn(buildMockPodLineItems());
    service.updateSubDraft(request, subDraftId);
  }

  @Test
  public void shouldDeleteWhenCallByService() {
    service.deleteSubDraft(podId, subDraftId);

    verify(siglusPodService).deleteSubDraft(podId, subDraftId);
  }

  @Test
  public void shouldGetLineItemWhenCallByService() {
    service.getSubDraftDetail(podId, subDraftId, defaultExpands);

    verify(siglusPodService).getSubDraftDetail(podId, subDraftId, defaultExpands);
  }

  @Test
  public void shouldCreateLocalIssueVoucher() {
    localIssueVoucher.setId(localIssueVoucherId);
    when(orderController.searchOrders(any(OrderSearchParams.class), any(PageRequest.class)))
        .thenReturn(new PageImpl(Collections.emptyList()));
    when(localIssueVoucherRepository
        .findByOrderCodeAndProgramIdAndRequestingFacilityIdAndSupplyingFacilityId(orderCode, programId,
            requestingFacilityId, supplyingFacilityId)).thenReturn(Collections.emptyList());
    when(localIssueVoucherRepository.save(any(LocalIssueVoucher.class))).thenReturn(localIssueVoucher);

    LocalIssueVoucherDto localIssueVoucher = service.createLocalIssueVoucher(localIssueVoucherDto);

    assertEquals(localIssueVoucher.getId(), localIssueVoucherId);
    assertEquals(localIssueVoucher.getProgramId(), programId);
    assertEquals(localIssueVoucher.getOrderCode(), orderCode);
    assertEquals(localIssueVoucher.getRequestingFacilityId(), requestingFacilityId);
    assertEquals(localIssueVoucher.getSupplyingFacilityId(), supplyingFacilityId);
  }

  @Test
  public void shouldThrowExceptionWhenHasSameOrderCodeInBasicOrderDto() {
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_ORDER_CODE_EXISTS));

    basicOrderDto.setOrderCode(orderCode);
    ArrayList<BasicOrderDto> basicOrderDtos = Lists.newArrayList(basicOrderDto);
    when(orderController.searchOrders(any(OrderSearchParams.class), any(PageRequest.class)))
        .thenReturn(new PageImpl(basicOrderDtos));
    when(localIssueVoucherRepository
        .findByOrderCodeAndProgramIdAndRequestingFacilityIdAndSupplyingFacilityId(orderCode, programId,
            requestingFacilityId, supplyingFacilityId)).thenReturn(Collections.emptyList());

    service.createLocalIssueVoucher(localIssueVoucherDto);
  }

  @Test
  public void shouldThrowExceptionWhenHasSameCodeInLocalIssueVoucherList() {
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_ORDER_CODE_EXISTS));

    when(orderController.searchOrders(any(OrderSearchParams.class), any(PageRequest.class)))
        .thenReturn(new PageImpl(Collections.emptyList()));
    when(localIssueVoucherRepository
        .findByOrderCodeAndProgramIdAndRequestingFacilityIdAndSupplyingFacilityId(orderCode, programId,
            requestingFacilityId, supplyingFacilityId)).thenReturn(newArrayList(localIssueVoucher));

    service.createLocalIssueVoucher(localIssueVoucherDto);
  }

  @Test
  public void shouldDeleteLocalIssueVoucher() {
    when(localIssueVoucherRepository.findOne(localIssueVoucherId)).thenReturn(localIssueVoucher);

    service.deleteLocalIssueVoucher(localIssueVoucherId);

    verify(podSubDraftRepository).deleteAllByPodId(localIssueVoucherId);
    verify(localIssueVoucherRepository).delete(localIssueVoucherId);
  }

  @Test
  public void shouldThrowExceptionWhenDeleteLocalIssueVoucher() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_LOCAL_ISSUE_VOUCHER_ID_INVALID));

    when(localIssueVoucherRepository.findOne(localIssueVoucherId)).thenReturn(null);

    service.deleteLocalIssueVoucher(localIssueVoucherId);
  }

  @Test
  public void shouldCreateLocalIssueVoucherSubDraft() {
    when(localIssueVoucherRepository.findOne(localIssueVoucherId)).thenReturn(localIssueVoucher);
    when(podSubDraftRepository.countAllByPodId(localIssueVoucherId)).thenReturn(5);
    when(authenticationHelper.getUserNameByUserId(operatorId)).thenReturn(operator);
    when(podSubDraftRepository.save(any(PodSubDraft.class))).thenReturn(localIssueVoucherSubDraft);

    SubDraftInfo localIssueVoucherSubDraft = service.createLocalIssueVoucherSubDraft(localIssueVoucherId);

    assertEquals(PodSubDraftStatusEnum.NOT_YET_STARTED, localIssueVoucherSubDraft.getStatus());
    assertEquals(6, localIssueVoucherSubDraft.getGroupNum());
    assertEquals(operator, localIssueVoucherSubDraft.getSaver());
  }

  @Test
  public void shouldThrowExceptionWhenSubDraftQuantityMoreThanTen() {
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_LOCAL_ISSUE_VOUCHER_SUB_DRAFTS_MORE_THAN_TEN));

    when(localIssueVoucherRepository.findOne(localIssueVoucherId)).thenReturn(localIssueVoucher);
    when(podSubDraftRepository.countAllByPodId(localIssueVoucherId)).thenReturn(10);

    service.createLocalIssueVoucherSubDraft(localIssueVoucherId);
  }

  private PodExtensionRequest buildPodExtensionRequest() {
    PodExtensionRequest request = new PodExtensionRequest();
    request.setPodDto(buildMockPodDtoWithOneLineItem());
    request.setPreparedBy(preparedBy);
    request.setConferredBy(conferredBy);
    return request;
  }

  private PodExtensionRequest buildPodExtensionRequestWithPodTwoLineItems() {
    PodExtensionRequest request = new PodExtensionRequest();
    request.setPodDto(buildMockPodDtoWithTwoLineItems());
    request.setPreparedBy(preparedBy);
    request.setConferredBy(conferredBy);
    return request;
  }

  private ProofsOfDeliveryExtension buildMockPodExtension() {
    return ProofsOfDeliveryExtension.builder()
        .preparedBy(preparedBy)
        .conferredBy(conferredBy)
        .build();
  }

  private void mockForRequisitionCount() {
    when(siglusRequisitionRepository.findRequisitionIdsByOrderInfo(any(), any(), any(), anyBoolean(), anyList()))
        .thenReturn(buildMockRequisitions());
    when(requisitionStatusChangeRepository.findByRequisitionIdIn(anyList())).thenReturn(
        buildMockRequisitionSubmittedStatusChanges());
  }

  private List<String> buildMockRequisitions() {
    return Lists.newArrayList(requisitionId.toString());
  }

  private List<StatusChange> buildMockRequisitionStatusChanges() {
    StatusChange statusChange = new StatusChange();
    statusChange.setStatus(RequisitionStatus.RELEASED);
    return Lists.newArrayList(statusChange);
  }

  private List<StatusChange> buildMockRequisitionSubmittedStatusChanges() {
    List<StatusChange> statusChanges = Lists.newArrayList();
    ZonedDateTime now = ZonedDateTime.now();
    for (int i = 0; i < 100; i++) {
      StatusChange statusChange = new StatusChange();
      Requisition requisition = new Requisition();
      requisition.setId(UUID.randomUUID());
      statusChange.setRequisition(requisition);
      statusChange.setCreatedDate(now.plusSeconds(i));
      statusChange.setStatus(RequisitionStatus.SUBMITTED);
      statusChanges.add(statusChange);
    }
    StatusChange statusChange = new StatusChange();
    Requisition requisition = new Requisition();
    requisition.setId(requisitionId);
    statusChange.setRequisition(requisition);
    statusChange.setCreatedDate(now.plusDays(1));
    statusChange.setStatus(RequisitionStatus.SUBMITTED);
    statusChanges.add(statusChange);
    return statusChanges;
  }

  private List<StatusChange> buildMockRequisitionStatusChangesWithNoReleasedStatus() {
    StatusChange statusChange = new StatusChange();
    statusChange.setStatus(RequisitionStatus.APPROVED);
    return Lists.newArrayList(statusChange);
  }

  private List<PodLineItemDto> buildMockPodLineItemDtos() {
    PodLineItemDto dto = PodLineItemDto.builder()
        .productCode(productCode)
        .productName(productName)
        .lotId(lotId)
        .lotCode(lotCode)
        .lotExpirationDate(LocalDate.now().minusMonths(1L))
        .requestedQuantity(1L)
        .orderedQuantity(1L)
        .receivedQuantity(1L)
        .build();
    return Lists.newArrayList(dto);
  }

  private FacilityDto buildMockFacilityDtoWithLevel3() {
    GeographicZoneDto zoneDto = new GeographicZoneDto();
    zoneDto.setName("district");
    zoneDto.setLevel(new GeographicLevelDto(UUID.randomUUID(), "code", "name", 3));
    GeographicZoneDto parentDto = new GeographicZoneDto();
    parentDto.setName("province");
    zoneDto.setParent(parentDto);

    FacilityDto dto = new FacilityDto();
    dto.setGeographicZone(zoneDto);
    return dto;
  }

  private FacilityDto buildMockFacilityDtoWithLevel2() {
    GeographicZoneDto zoneDto = new GeographicZoneDto();
    zoneDto.setName("district");
    zoneDto.setLevel(new GeographicLevelDto(UUID.randomUUID(), "code", "name", 2));
    GeographicZoneDto parentDto = new GeographicZoneDto();
    parentDto.setName("province");
    zoneDto.setParent(parentDto);

    FacilityDto dto = new FacilityDto();
    dto.setGeographicZone(zoneDto);
    return dto;
  }

  private FacilityDto buildMockFacilityDtoWithLevel3AndNoParent() {
    GeographicZoneDto zoneDto = new GeographicZoneDto();
    zoneDto.setName("district");
    zoneDto.setLevel(new GeographicLevelDto(UUID.randomUUID(), "code", "name", 3));

    FacilityDto dto = new FacilityDto();
    dto.setGeographicZone(zoneDto);
    return dto;
  }

  private OrderDto buildMockOrderDto() {
    return OrderDto.builder()
        .id(orderId)
        .emergency(Boolean.FALSE)
        .receivingFacilityId(facilityId)
        .supplyingFacilityId(facilityId)
        .receivingFacilityCode(facilityCode)
        .requisitionId(requisitionId)
        .processingPeriodId(processingPeriodId)
        .periodEndDate(Date.from(Instant.now()))
        .build();
  }

  private OrderDto buildMockOrderDtoWithOutRequisitionId() {
    return OrderDto.builder()
        .id(orderId)
        .emergency(Boolean.TRUE)
        .receivingFacilityId(facilityId)
        .supplyingFacilityId(facilityId)
        .receivingFacilityCode(facilityCode)
        .processingPeriodId(processingPeriodId)
        .externalId(requisitionId)
        .periodEndDate(Date.from(Instant.now()))
        .build();
  }

  private List<SubDraftInfo> toSubDraftInfos(List<PodSubDraft> podSubDrafts) {
    return podSubDrafts.stream().map(podSubDraft ->
        SubDraftInfo.builder()
            .subDraftId(podSubDraft.getId())
            .groupNum(podSubDraft.getNumber())
            .saver(authenticationHelper.getUserNameByUserId(podSubDraft.getOperatorId()))
            .status(podSubDraft.getStatus())
            .build())
        .collect(Collectors.toList());
  }

  private PodExtensionResponse buildMockPodExtensionResponse() {
    PodExtensionResponse response = new PodExtensionResponse();
    response.setPodDto(buildMockPodDtoWithNoLineItems());
    response.setPreparedBy(preparedBy);
    response.setConferredBy(conferredBy);
    return response;
  }

  private ProofOfDeliveryDto buildMockPodDtoWithNoLineItems() {
    ProofOfDeliveryDto podDto = new ProofOfDeliveryDto();
    podDto.setId(podId);
    podDto.setStatus(ProofOfDeliveryStatus.CONFIRMED);
    podDto.setShipment(buildMockShipmentDto());
    podDto.setLineItems(Lists.newArrayList());
    return podDto;
  }

  private ProofOfDeliveryDto buildMockPodDtoWithOneLineItem() {
    ProofOfDeliveryDto podDto = new ProofOfDeliveryDto();
    podDto.setId(podId);
    podDto.setStatus(ProofOfDeliveryStatus.CONFIRMED);
    podDto.setShipment(buildMockShipmentDto());

    ProofOfDeliveryLineItemDto lineItemDto = new ProofOfDeliveryLineItemDto(serviceUrl,
        new VersionObjectReferenceDto(),
        new ObjectReferenceDto(UUID.randomUUID(), serviceUrl, "resourceName"), 10, Boolean.TRUE, null, 0,
        UUID.randomUUID(), "test notes");
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setProductCode(productCode);
    lineItemDto.setOrderable(orderableDto);
    lineItemDto.setId(lineItemId1);

    List<ProofOfDeliveryLineItemDto> lineItemDtos = Lists.newArrayList();
    lineItemDtos.add(lineItemDto);

    podDto.setLineItems(lineItemDtos);

    return podDto;
  }

  private ProofOfDeliveryDto buildMockPodDtoWithTwoLineItems() {
    ProofOfDeliveryDto podDto = new ProofOfDeliveryDto();
    podDto.setId(podId);
    podDto.setShipment(buildMockShipmentDto());

    ProofOfDeliveryLineItemDto lineItemDto1 = new ProofOfDeliveryLineItemDto(serviceUrl,
        new VersionObjectReferenceDto(),
        new ObjectReferenceDto(UUID.randomUUID(), serviceUrl, "resourceName"), 10, Boolean.TRUE, null, 0,
        UUID.randomUUID(), "test notes");
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setProductCode(productCode);
    lineItemDto1.setOrderable(orderableDto);
    lineItemDto1.setId(lineItemId1);

    ProofOfDeliveryLineItemDto lineItemDto2 = new ProofOfDeliveryLineItemDto();
    BeanUtils.copyProperties(lineItemDto1, lineItemDto2);
    lineItemDto2.setId(lineItemId2);

    List<ProofOfDeliveryLineItemDto> lineItemDtos = Lists.newArrayList();
    lineItemDtos.add(lineItemDto1);
    lineItemDtos.add(lineItemDto2);

    podDto.setLineItems(lineItemDtos);

    return podDto;
  }

  private ShipmentObjectReferenceDto buildMockShipmentDto() {
    OrderObjectReferenceDto orderDto = new OrderObjectReferenceDto(orderDtoId);
    orderDto.setExternalId(externalId);
    ShipmentObjectReferenceDto shipmentDto = new ShipmentObjectReferenceDto(shipmentDtoId);
    shipmentDto.setOrder(orderDto);
    return shipmentDto;
  }

  private List<PodSubDraft> buildMockSubDrafts() {
    return Lists.newArrayList(buildMockSubDraftNotYetStarted());
  }

  private List<PodSubDraft> buildMockSubDraftsAllSubmitted() {
    return Lists.newArrayList(buildMockSubDraftSubmitted());
  }

  private PodSubDraft buildMockSubDraftSubmitted() {
    PodSubDraft podSubDraft = PodSubDraft.builder()
        .podId(podId)
        .number(1)
        .status(PodSubDraftStatusEnum.SUBMITTED)
        .build();
    podSubDraft.setId(podId);
    return podSubDraft;
  }

  private PodSubDraft buildMockSubDraftNotYetStarted() {
    PodSubDraft podSubDraft = PodSubDraft.builder()
        .podId(podId)
        .number(1)
        .status(PodSubDraftStatusEnum.NOT_YET_STARTED)
        .build();
    podSubDraft.setId(podId);
    return podSubDraft;
  }

  private List<Orderable> buildMockOrderables() {
    Orderable orderable = new Orderable(Code.code(productCode), null, 0, 0, Boolean.FALSE, orderableId, 0L);
    return Lists.newArrayList(orderable);
  }

  private List<PodLineItemsExtension> buildMockPodLineItemsExtensions() {
    PodLineItemsExtension lineItemsExtension1 = PodLineItemsExtension.builder()
        .subDraftId(subDraftId)
        .podLineItemId(lineItemId1)
        .build();
    return Lists.newArrayList(lineItemsExtension1);
  }

  private List<ProofOfDeliveryLineItem> buildMockPodLineItems() {
    ProofOfDeliveryLineItem lineItem = new ProofOfDeliveryLineItem(null, UUID.randomUUID(), 10, null, 0,
        UUID.randomUUID(), "test notes");
    lineItem.setId(lineItemId1);
    return Lists.newArrayList(lineItem);
  }
}