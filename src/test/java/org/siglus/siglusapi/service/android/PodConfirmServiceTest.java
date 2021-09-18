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

package org.siglus.siglusapi.service.android;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.CreationDetails;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.ProofOfDeliveryStatus;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.domain.naming.VvmStatus;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.PermissionService;
import org.openlmis.fulfillment.util.DateHelper;
import org.openlmis.fulfillment.web.NotFoundException;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.openlmis.stockmanagement.dto.referencedata.LotDto;
import org.openlmis.stockmanagement.service.referencedata.LotReferenceDataService;
import org.powermock.api.mockito.PowerMockito;
import org.siglus.siglusapi.domain.PodConfirmBackup;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.android.request.LotBasicRequest;
import org.siglus.siglusapi.dto.android.request.PodLotLineRequest;
import org.siglus.siglusapi.dto.android.request.PodProductLineRequest;
import org.siglus.siglusapi.dto.android.request.PodRequest;
import org.siglus.siglusapi.dto.android.response.LotBasicResponse;
import org.siglus.siglusapi.dto.android.response.PodLotLineResponse;
import org.siglus.siglusapi.dto.android.response.PodProductLineResponse;
import org.siglus.siglusapi.dto.android.response.PodResponse;
import org.siglus.siglusapi.repository.OrderLineItemRepository;
import org.siglus.siglusapi.repository.PodConfirmBackupRepository;
import org.siglus.siglusapi.repository.PodNativeSqlRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryLineItemRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SiglusShipmentLineItemRepository;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;
import org.siglus.siglusapi.repository.SyncUpHashRepository;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class PodConfirmServiceTest {

  private UserDto user;
  private String orderCode;
  private String lotCode;
  private String notes;
  private String rejectReason;
  private String orderProductCode;
  private String originNumber;
  private List<StockCardLineItem> stockCardLineItems;
  private Long orderableVersionNum;
  private Long quantityShipped;

  private final UUID orderOrderableId = UUID.randomUUID();
  private final UUID orderLotId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final String syncUpHash = "syncUpHash";
  private final UUID programId = UUID.randomUUID();
  private final UUID tradeItemId = UUID.randomUUID();
  private final UUID stockLineItemId = UUID.randomUUID();
  private final UUID facilityTypeId = UUID.randomUUID();

  @InjectMocks
  private PodConfirmService podConfirmService;

  @Mock
  private SiglusAuthenticationHelper authHelper;

  @Mock
  private SyncUpHashRepository syncUpHashRepository;

  @Mock
  private PermissionService fulfillmentPermissionService;

  @Mock
  private SiglusProgramService siglusProgramService;

  @Mock
  private SiglusApprovedProductReferenceDataService approvedProductReferenceDataService;

  @Mock
  private SiglusProofOfDeliveryRepository podRepository;

  @Mock
  private SiglusProofOfDeliveryLineItemRepository podLineItemRepository;

  @Mock
  private SiglusShipmentLineItemRepository shipmentLineItemRepository;

  @Mock
  private SiglusOrderableService orderableService;

  @Mock
  private LotReferenceDataService lotService;

  @Mock
  private SiglusStockCardLineItemRepository stockCardLineItemRepository;

  @Mock
  private DateHelper dateHelper;

  @Mock
  private OrderLineItemRepository orderLineItemRepository;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private SiglusValidReasonAssignmentService validReasonAssignmentService;

  @SuppressWarnings("unused")
  @Mock
  private PodNativeSqlRepository podNativeSqlRepository;

  @Mock
  private PodConfirmBackupRepository podConfirmBackupRepository;

  @Before
  public void prepare() {
    orderCode = "orderCode";
    lotCode = "lotCode";
    notes = "notes";
    rejectReason = "issue";
    orderProductCode = "26A01";
    originNumber = "orderCodeOrigin";
    orderableVersionNum = 1L;
    quantityShipped = 10L;
    user = PowerMockito.mock(UserDto.class);
    when(user.getHomeFacilityId()).thenReturn(facilityId);
    when(user.getId()).thenReturn(userId);
    when(authHelper.getCurrentUser()).thenReturn(user);
    stockCardLineItems = buildStockCardLineItems();
  }

  @Test(expected = ValidationException.class)
  public void shouldThrowValidationExceptionWhenPodIsConfirmed() {
    // given
    PodRequest podRequest = mockPodRequest();
    ProofOfDelivery toUpdate = mockPod(user, true);
    PodResponse podResponse = mockPodResponse();
    when(syncUpHashRepository.findOne(syncUpHash)).thenReturn(null);

    // when
    podConfirmService.confirmPod(podRequest, toUpdate, user, podResponse);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowNotFoundExceptionWhenContainsUnsupportedProduct() {
    // given
    PodRequest podRequest = mockPodRequest();
    when(syncUpHashRepository.findOne(syncUpHash)).thenReturn(null);
    when(siglusProgramService.getProgramIdByCode(podRequest.getProgramCode())).thenReturn(programId);
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setProductCode("02A01");
    ApprovedProductDto approvedProductDto = new ApprovedProductDto();
    approvedProductDto.setOrderable(orderableDto);
    when(approvedProductReferenceDataService.getApprovedProducts(facilityId, programId, Collections.emptyList()))
        .thenReturn(Collections.singletonList(approvedProductDto));
    ProofOfDelivery toUpdate = mockPod(user, false);
    PodResponse podResponse = mockPodResponse();

    // when
    podConfirmService.confirmPod(podRequest, toUpdate, user, podResponse);

    // then
    verify(fulfillmentPermissionService, times(1)).canManagePod(toUpdate);
  }

  @Test
  public void shouldUpdatePodWhenHappyPath() {
    // given
    PodRequest podRequest = mockPodRequest();
    podRequest.setOriginNumber(originNumber);
    when(syncUpHashRepository.findOne(syncUpHash)).thenReturn(null);
    when(siglusProgramService.getProgramIdByCode(podRequest.getProgramCode())).thenReturn(programId);
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setProductCode(orderProductCode);
    ApprovedProductDto approvedProductDto = new ApprovedProductDto();
    approvedProductDto.setOrderable(orderableDto);
    when(approvedProductReferenceDataService.getApprovedProducts(facilityId, programId, Collections.emptyList()))
        .thenReturn(Collections.singletonList(approvedProductDto));
    org.siglus.common.dto.referencedata.OrderableDto orderableDto1 =
        new org.siglus.common.dto.referencedata.OrderableDto();
    orderableDto1.setProductCode(orderProductCode);
    orderableDto1.setTradeItemIdentifier(tradeItemId);
    when(orderableService.getOrderableByCode(orderProductCode)).thenReturn(orderableDto1);
    when(lotService.getAllLotsOf(tradeItemId)).thenReturn(buildLotDto());
    when(stockCardLineItemRepository.findByFacilityIdAndLotIdIn(
        facilityId, originNumber, Collections.singleton(orderLotId))).thenReturn(stockCardLineItems);
    when(facilityReferenceDataService.getFacilityById(facilityId)).thenReturn(buildFacilityDto());
    when(validReasonAssignmentService.getValidReasonsForAllProducts(facilityTypeId, null, null))
        .thenReturn(buildReasonAsignment());
    ProofOfDelivery toUpdate = mockPod(user, false);
    PodResponse podResponse = mockPodResponse();

    // when
    podConfirmService.confirmPod(podRequest, toUpdate, user, podResponse);

    // then
    verify(podRepository).updatePodById(podRequest.getDeliveredBy(),
        podRequest.getReceivedBy(), podRequest.getReceivedDate(), ProofOfDeliveryStatus.CONFIRMED.toString(),
        toUpdate.getId());
    verify(podConfirmBackupRepository).save(any(PodConfirmBackup.class));
    verify(podLineItemRepository).deletePodLineItemByIdsIn(Collections.singleton(null));
    verify(shipmentLineItemRepository).deleteShipmentLineItemByIdsIn(
        Collections.singleton(null));
    verify(stockCardLineItemRepository).save(stockCardLineItems);
    verify(dateHelper).getCurrentDateTimeWithSystemZone();
    verify(orderLineItemRepository).save(anyListOf(OrderLineItem.class));
    verify(orderRepository).save(any(Order.class));
  }

  private PodRequest mockPodRequest() {
    LotBasicRequest lot = new LotBasicRequest();
    lot.setCode(lotCode);
    lot.setExpirationDate(LocalDate.now());
    PodLotLineRequest podLotLineRequest = new PodLotLineRequest();
    podLotLineRequest.setLot(lot);
    podLotLineRequest.setNotes(notes);
    podLotLineRequest.setRejectedReason(rejectReason);
    List<PodLotLineRequest> podLots = Collections.singletonList(podLotLineRequest);
    PodProductLineRequest podProductLineRequest = new PodProductLineRequest();
    podProductLineRequest.setCode(orderProductCode);
    podProductLineRequest.setOrderedQuantity(10);
    podProductLineRequest.setPartialFulfilledQuantity(0);
    podProductLineRequest.setLots(podLots);
    List<PodProductLineRequest> products = Collections.singletonList(podProductLineRequest);
    PodRequest podRequest = new PodRequest();
    podRequest.setOrderCode(orderCode);
    podRequest.setProducts(products);
    return podRequest;
  }

  private ProofOfDelivery mockPod(UserDto user, boolean isConfirm) {
    Order order = PowerMockito.mock(Order.class);
    CreationDetails shipDetails = new CreationDetails(user.getId(), ZonedDateTime.now());
    String notes = "notes";
    ShipmentLineItem shipmentLineItem = new ShipmentLineItem(
        new VersionEntityReference(orderOrderableId, orderableVersionNum), quantityShipped, new HashMap<>());
    List<ShipmentLineItem> shipmentLineItems = Collections.singletonList(shipmentLineItem);
    Shipment shipment = new Shipment(order, shipDetails, notes, shipmentLineItems, new HashMap<>());
    ProofOfDeliveryLineItem podLineItem = new ProofOfDeliveryLineItem(
        new VersionEntityReference(orderOrderableId, orderableVersionNum), orderLotId, 10,
        VvmStatus.STAGE_1, 0, UUID.randomUUID(), notes);
    List<ProofOfDeliveryLineItem> lineItems = Collections.singletonList(podLineItem);
    ProofOfDelivery proofOfDelivery;
    if (isConfirm) {
      proofOfDelivery = new ProofOfDelivery(
          shipment, ProofOfDeliveryStatus.CONFIRMED, lineItems, "re", "de", LocalDate.now());
    } else {
      proofOfDelivery = new ProofOfDelivery(
          shipment, ProofOfDeliveryStatus.INITIATED, lineItems, "re", "de", LocalDate.now());
    }
    return proofOfDelivery;
  }

  private List<LotDto> buildLotDto() {
    LotDto lotDto = LotDto.builder().lotCode(lotCode).id(orderLotId).build();
    return Collections.singletonList(lotDto);
  }

  private List<StockCardLineItem> buildStockCardLineItems() {
    StockCardLineItem stockCardLineItem = new StockCardLineItem();
    stockCardLineItem.setId(stockLineItemId);
    stockCardLineItem.setDocumentNumber(orderCode);
    return Collections.singletonList(stockCardLineItem);
  }

  private FacilityDto buildFacilityDto() {
    FacilityTypeDto facilityTypeDto = new FacilityTypeDto();
    facilityTypeDto.setId(facilityTypeId);
    return FacilityDto.builder().id(facilityId).type(facilityTypeDto).build();
  }

  private Collection<ValidReasonAssignmentDto> buildReasonAsignment() {
    StockCardLineItemReason stockCardLineItemReason = new StockCardLineItemReason();
    stockCardLineItemReason.setName(rejectReason);
    stockCardLineItemReason.setId(UUID.randomUUID());
    ValidReasonAssignmentDto reasonAssignmentDto = new ValidReasonAssignmentDto();
    reasonAssignmentDto.setFacilityTypeId(facilityTypeId);
    reasonAssignmentDto.setReason(stockCardLineItemReason);
    return Collections.singletonList(reasonAssignmentDto);
  }

  private PodResponse mockPodResponse() {
    LotBasicResponse lotBasic = new LotBasicResponse();
    lotBasic.setCode(lotCode);
    lotBasic.setExpirationDate(LocalDate.of(2021, 10, 10));
    PodLotLineResponse podLotLine = new PodLotLineResponse();
    podLotLine.setLot(lotBasic);
    podLotLine.setShippedQuantity(100);
    PodProductLineResponse podProductLine = new PodProductLineResponse();
    podProductLine.setCode(orderProductCode);
    podProductLine.setLots(Collections.singletonList(podLotLine));
    PodResponse podResponse = new PodResponse();
    podResponse.setProducts(Collections.singletonList(podProductLine));
    return podResponse;
  }
}