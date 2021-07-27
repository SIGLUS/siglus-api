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

package org.siglus.siglusapi.web.android;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.service.referencedata.ProgramDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.siglus.common.dto.referencedata.FacilityTypeDto;
import org.siglus.common.dto.referencedata.LotDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.service.SiglusOrderService;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.siglus.siglusapi.service.android.SiglusMeService;
import org.siglus.siglusapi.service.android.mapper.LotMapperImpl;
import org.siglus.siglusapi.service.android.mapper.PodLotLineMapper;
import org.siglus.siglusapi.service.android.mapper.PodLotLineMapperImpl;
import org.siglus.siglusapi.service.android.mapper.PodMapper;
import org.siglus.siglusapi.service.android.mapper.PodMapperImpl;
import org.siglus.siglusapi.service.android.mapper.PodOrderMapperImpl;
import org.siglus.siglusapi.service.android.mapper.PodProductLineMapperImpl;
import org.siglus.siglusapi.service.android.mapper.PodRequisitionMapperImpl;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {MappingJackson2HttpMessageConverter.class, PodMapperImpl.class,
    ObjectMapper.class, PodOrderMapperImpl.class, PodRequisitionMapperImpl.class, PodProductLineMapperImpl.class,
    PodLotLineMapperImpl.class, LotMapperImpl.class})
public class SiglusMeControllerProofOfDeliveryMvcTest extends FileBasedTest {

  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private MappingJackson2HttpMessageConverter jackson2HttpMessageConverter;
  @Autowired
  private PodMapper podMapper;
  @Autowired
  private PodLotLineMapper podLotLineMapper;

  @InjectMocks
  private SiglusMeService service;
  @Mock
  private SiglusProofOfDeliveryRepository podRepo;
  @Mock
  private SiglusAuthenticationHelper authHelper;
  @Mock
  private SiglusFacilityReferenceDataService facilityReferenceDataService;
  @Mock
  private SiglusOrderService orderService;
  @Mock
  private SiglusOrderableReferenceDataService orderableDataService;
  @Mock
  private SiglusLotReferenceDataService lotReferenceDataService;
  @Mock
  private SiglusValidReasonAssignmentService validReasonAssignmentService;

  @Captor
  private ArgumentCaptor<UUID> facilityIdCaptor;
  @Captor
  private ArgumentCaptor<LocalDate> sinceCaptor;
  @Captor
  private ArgumentCaptor<OrderStatus> statuesCaptor;

  private final UUID homeFacilityId = UUID.randomUUID();
  private final UUID order1Id = UUID.randomUUID();
  private final UUID order2Id = UUID.randomUUID();
  private final UUID order3Id = UUID.randomUUID();
  private final UUID product1Id = UUID.randomUUID();
  private final UUID product2Id = UUID.randomUUID();
  private final UUID product1Lot1Id = UUID.randomUUID();
  private final UUID product2Lot1Id = UUID.randomUUID();
  private final UUID reasonId = UUID.randomUUID();

  @Before
  public void setup() throws JsonProcessingException {
    ReflectionTestUtils.setField(service, "podMapper", podMapper);
    ReflectionTestUtils.setField(service, "podLotLineMapper", podLotLineMapper);
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    objectMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
    objectMapper.registerModule(new JavaTimeModule());
    jackson2HttpMessageConverter.setObjectMapper(objectMapper);
    SiglusMeController controller = new SiglusMeController(service);
    this.mockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setMessageConverters(jackson2HttpMessageConverter)
        .build();
    mockAuth();
    mockPods();
    mockOrders();
    mockProducts();
    mockLots();
    mockReasons();
  }

  @Test
  public void shouldReturnAllWhenGetPodsGivenNoParams() throws Exception {
    // given
    RequestBuilder request = get("/api/siglusapi/android/me/facility/pods")
        .contentType(MediaType.APPLICATION_JSON)
        .characterEncoding("utf-8");

    // when
    ResultActions resultActions = mockMvc.perform(request).andDo(print());

    // then
    resultActions.andExpect(status().isOk())
        .andExpect(jsonPath("[0].shippedDate").value("2020-09-02"))
        .andExpect(jsonPath("[0].receivedDate").value("2020-10-01"))
        .andExpect(jsonPath("[0].deliveredBy").value("qla"))
        .andExpect(jsonPath("[0].receivedBy").value("zjj"))
        .andExpect(jsonPath("[0].order.code").value("ORDER-AS20JF"))
        .andExpect(jsonPath("[0].order.supplyFacilityName").value("Centro de Saude de ntopa"))
        .andExpect(jsonPath("[0].order.createdDate").value(1598976000000L))
        .andExpect(jsonPath("[0].order.lastModifiedDate").value(1599012900000L))
        .andExpect(jsonPath("[0].order.status").value("RECEIVED"))
        .andExpect(jsonPath("[0].order.requisition.number").value("RNR-NO01050119-0"))
        .andExpect(jsonPath("[0].order.requisition.isEmergency").value("false"))
        .andExpect(jsonPath("[0].order.requisition.programCode").value("VC"))
        .andExpect(jsonPath("[0].order.requisition.startDate").value("2020-07-21"))
        .andExpect(jsonPath("[0].order.requisition.endDate").value("2020-08-20"))
        .andExpect(jsonPath("[0].products[0].code").value("22A01"))
        .andExpect(jsonPath("[0].products[0].orderedQuantity").value(20))
        .andExpect(jsonPath("[0].products[0].partialFulfilledQuantity").value(0))
        .andExpect(jsonPath("[0].products[0].lots[0].lot.code").value("SME-LOTE-22A01-062023"))
        .andExpect(jsonPath("[0].products[0].lots[0].lot.expirationDate").value("2023-06-30"))
        .andExpect(jsonPath("[0].products[0].lots[0].shippedQuantity").value(20))
        .andExpect(jsonPath("[0].products[0].lots[0].acceptedQuantity").value(10))
        .andExpect(jsonPath("[0].products[0].lots[0].rejectedReason").value("reject"))
        .andExpect(jsonPath("[0].products[0].lots[0].notes").value("123"))
        .andExpect(jsonPath("[1].shippedDate").value("2020-10-02"))
        .andExpect(jsonPath("[1].receivedDate").value("2020-11-01"))
        .andExpect(jsonPath("[1].deliveredBy").value("qla"))
        .andExpect(jsonPath("[1].receivedBy").value("zjj"))
        .andExpect(jsonPath("[1].order.code").value("ORDER-AS21JF"))
        .andExpect(jsonPath("[1].order.supplyFacilityName").value("Centro de Saude de ntopa"))
        .andExpect(jsonPath("[1].order.createdDate").value(1601568000000L))
        .andExpect(jsonPath("[1].order.lastModifiedDate").value(1601604900000L))
        .andExpect(jsonPath("[1].order.status").value("RECEIVED"))
        .andExpect(jsonPath("[1].order.requisition.number").value("RNR-NO01050120-0"))
        .andExpect(jsonPath("[1].order.requisition.isEmergency").value("false"))
        .andExpect(jsonPath("[1].order.requisition.programCode").value("VC"))
        .andExpect(jsonPath("[1].order.requisition.startDate").value("2020-08-21"))
        .andExpect(jsonPath("[1].order.requisition.endDate").value("2020-09-20"))
        .andExpect(jsonPath("[1].products[0].code").value("22B01"))
        .andExpect(jsonPath("[1].products[0].orderedQuantity").value(20))
        .andExpect(jsonPath("[1].products[0].partialFulfilledQuantity").value(0))
        .andExpect(jsonPath("[1].products[0].lots[0].lot.code").value("SME-LOTE-22B01-062023"))
        .andExpect(jsonPath("[1].products[0].lots[0].lot.expirationDate").value("2023-06-30"))
        .andExpect(jsonPath("[1].products[0].lots[0].shippedQuantity").value(20))
        .andExpect(jsonPath("[1].products[0].lots[0].acceptedQuantity").value(20))
        .andExpect(jsonPath("[1].products[0].lots[0].rejectedReason").isEmpty())
        .andExpect(jsonPath("[1].products[0].lots[0].notes").value("123"))
        .andExpect(jsonPath("[2].shippedDate").value("2020-11-02"))
        .andExpect(jsonPath("[2].receivedDate").isEmpty())
        .andExpect(jsonPath("[2].deliveredBy").isEmpty())
        .andExpect(jsonPath("[2].receivedBy").isEmpty())
        .andExpect(jsonPath("[2].order.code").value("ORDER-AS22JF"))
        .andExpect(jsonPath("[2].order.supplyFacilityName").value("Centro de Saude de ntopa"))
        .andExpect(jsonPath("[2].order.createdDate").value(1604246400000L))
        .andExpect(jsonPath("[2].order.lastModifiedDate").value(1604283300000L))
        .andExpect(jsonPath("[2].order.status").value("SHIPPED"))
        .andExpect(jsonPath("[2].order.requisition.number").value("RNR-NO01050121-0"))
        .andExpect(jsonPath("[2].order.requisition.isEmergency").value("false"))
        .andExpect(jsonPath("[2].order.requisition.programCode").value("VC"))
        .andExpect(jsonPath("[2].order.requisition.startDate").value("2020-09-21"))
        .andExpect(jsonPath("[2].order.requisition.endDate").value("2020-10-20"))
        .andExpect(jsonPath("[2].products[0].code").value("22A01"))
        .andExpect(jsonPath("[2].products[0].orderedQuantity").value(20))
        .andExpect(jsonPath("[2].products[0].partialFulfilledQuantity").value(0))
        .andExpect(jsonPath("[2].products[0].lots[0].lot.code").value("SME-LOTE-22A01-062023"))
        .andExpect(jsonPath("[2].products[0].lots[0].lot.expirationDate").value("2023-06-30"))
        .andExpect(jsonPath("[2].products[0].lots[0].shippedQuantity").value(20))
        .andExpect(jsonPath("[2].products[0].lots[0].acceptedQuantity").isEmpty())
        .andExpect(jsonPath("[2].products[0].lots[0].rejectedReason").isEmpty())
        .andExpect(jsonPath("[2].products[0].lots[0].notes").isEmpty());
  }

  @Test
  public void shouldReturnAllWhenGetPodsGivenStartDateAndShippedOnly() throws Exception {
    // given
    RequestBuilder request = get("/api/siglusapi/android/me/facility/pods?startDate=2020-09-11&shippedOnly=true")
        .contentType(MediaType.APPLICATION_JSON)
        .characterEncoding("utf-8");

    // when
    ResultActions resultActions = mockMvc.perform(request).andDo(print());

    // then
    resultActions.andExpect(status().isOk());
    verify(podRepo).findAllByFacilitySince(facilityIdCaptor.capture(), sinceCaptor.capture(), statuesCaptor.capture());
    assertEquals(homeFacilityId, facilityIdCaptor.getValue());
    assertEquals(LocalDate.of(2020, 9, 11), sinceCaptor.getValue());
    List<OrderStatus> statuses = statuesCaptor.getAllValues();
    assertEquals(1, statuses.size());
    assertEquals(OrderStatus.SHIPPED, statuses.get(0));
  }

  @Test
  public void shouldReturnAllWhenGetPodsGivenStartDate() throws Exception {
    // given
    RequestBuilder request = get("/api/siglusapi/android/me/facility/pods?startDate=2020-09-11")
        .contentType(MediaType.APPLICATION_JSON)
        .characterEncoding("utf-8");

    // when
    ResultActions resultActions = mockMvc.perform(request).andDo(print());

    // then
    resultActions.andExpect(status().isOk());
    verify(podRepo).findAllByFacilitySince(facilityIdCaptor.capture(), sinceCaptor.capture(), statuesCaptor.capture());
    assertEquals(homeFacilityId, facilityIdCaptor.getValue());
    assertEquals(LocalDate.of(2020, 9, 11), sinceCaptor.getValue());
    List<OrderStatus> statuses = statuesCaptor.getAllValues();
    assertEquals(2, statuses.size());
    assertEquals(OrderStatus.SHIPPED, statuses.get(0));
    assertEquals(OrderStatus.RECEIVED, statuses.get(1));
  }


  @Test
  public void shouldReturnAllWhenGetPodsGivenShippedOnly() throws Exception {
    // given
    RequestBuilder request = get("/api/siglusapi/android/me/facility/pods?shippedOnly=true")
        .contentType(MediaType.APPLICATION_JSON)
        .characterEncoding("utf-8");

    // when
    ResultActions resultActions = mockMvc.perform(request).andDo(print());

    // then
    resultActions.andExpect(status().isOk());
    verify(podRepo).findAllByFacilitySince(facilityIdCaptor.capture(), sinceCaptor.capture(), statuesCaptor.capture());
    assertEquals(homeFacilityId, facilityIdCaptor.getValue());
    assertEquals(getLastYear(), sinceCaptor.getValue());
    List<OrderStatus> statuses = statuesCaptor.getAllValues();
    assertEquals(1, statuses.size());
    assertEquals(OrderStatus.SHIPPED, statuses.get(0));
  }

  private void mockProducts() {
    org.siglus.common.dto.referencedata.OrderableDto product1 =
        mock(org.siglus.common.dto.referencedata.OrderableDto.class);
    when(product1.getId()).thenReturn(product1Id);
    when(product1.getProductCode()).thenReturn("22A01");
    org.siglus.common.dto.referencedata.OrderableDto product2 =
        mock(org.siglus.common.dto.referencedata.OrderableDto.class);
    when(product2.getId()).thenReturn(product2Id);
    when(product2.getProductCode()).thenReturn("22B01");
    when(orderableDataService.findByIds(any())).thenReturn(asList(product1, product2));
  }

  private void mockLots() {
    LotDto product1Lot1 = mock(LotDto.class);
    when(product1Lot1.getId()).thenReturn(product1Lot1Id);
    when(product1Lot1.getLotCode()).thenReturn("SME-LOTE-22A01-062023");
    when(product1Lot1.getExpirationDate()).thenReturn(LocalDate.of(2023, 6, 30));
    LotDto product2Lot1 = mock(LotDto.class);
    when(product2Lot1.getId()).thenReturn(product2Lot1Id);
    when(product2Lot1.getLotCode()).thenReturn("SME-LOTE-22B01-062023");
    when(product2Lot1.getExpirationDate()).thenReturn(LocalDate.of(2023, 6, 30));
    when(lotReferenceDataService.findByIds(any())).thenReturn(asList(product1Lot1, product2Lot1));
  }

  private void mockReasons() {
    ValidReasonAssignmentDto reason1 = mock(ValidReasonAssignmentDto.class);
    when(reason1.getId()).thenReturn(reasonId);
    StockCardLineItemReason reason = mock(StockCardLineItemReason.class);
    when(reason.getName()).thenReturn("reject");
    when(reason1.getReason()).thenReturn(reason);
    ValidReasonAssignmentDto reason2 = mock(ValidReasonAssignmentDto.class);
    when(reason2.getId()).thenReturn(UUID.randomUUID());
    StockCardLineItemReason mockReason = mock(StockCardLineItemReason.class);
    when(mockReason.getName()).thenReturn("nothing");
    when(reason2.getReason()).thenReturn(mockReason);
    when(validReasonAssignmentService.getValidReasonsForAllProducts(any(), any(), any()))
        .thenReturn(asList(reason1, reason2));
  }

  private void mockAuth() {
    UserDto user = mock(UserDto.class);
    when(user.getHomeFacilityId()).thenReturn(homeFacilityId);
    when(authHelper.getCurrentUser()).thenReturn(user);
    org.siglus.common.dto.referencedata.FacilityDto homeFacility =
        new org.siglus.common.dto.referencedata.FacilityDto();
    homeFacility.setId(homeFacilityId);
    FacilityTypeDto facilityType = new FacilityTypeDto();
    facilityType.setId(UUID.randomUUID());
    homeFacility.setType(facilityType);
    when(facilityReferenceDataService.getFacilityById(homeFacilityId)).thenReturn(homeFacility);
  }

  private void mockPods() {
    VersionEntityReference orderable1 = mock(VersionEntityReference.class);
    when(orderable1.getId()).thenReturn(product1Id);
    VersionEntityReference orderable2 = mock(VersionEntityReference.class);
    when(orderable2.getId()).thenReturn(product2Id);

    ProofOfDelivery pod1 = mockPod1(orderable1);
    ProofOfDelivery pod2 = mockPod2(orderable2);
    ProofOfDelivery pod3 = mockPod3(orderable1);

    when(podRepo.findAllByFacilitySince(any(), any(), anyVararg())).thenReturn(asList(pod1, pod2, pod3));
  }

  private ProofOfDelivery mockPod1(VersionEntityReference orderable1) {
    ProofOfDelivery pod1 = mock(ProofOfDelivery.class);
    when(pod1.getReceivedDate()).thenReturn(LocalDate.of(2020, 10, 1));
    when(pod1.getDeliveredBy()).thenReturn("qla");
    when(pod1.getReceivedBy()).thenReturn("zjj");
    Shipment shipment1 = mock(Shipment.class);
    when(pod1.getShipment()).thenReturn(shipment1);
    when(shipment1.getShippedDate()).thenReturn(LocalDate.of(2020, 9, 2).atStartOfDay(ZoneId.systemDefault()));
    Order order1 = mock(Order.class);
    when(shipment1.getOrder()).thenReturn(order1);
    ShipmentLineItem shipment1Line1 = mock(ShipmentLineItem.class);
    when(shipment1Line1.getLotId()).thenReturn(product1Lot1Id);
    when(shipment1Line1.getQuantityShipped()).thenReturn(20L);
    when(shipment1Line1.getOrderable()).thenReturn(orderable1);
    when(shipment1.getLineItems()).thenReturn(singletonList(shipment1Line1));
    when(order1.getId()).thenReturn(order1Id);
    ProofOfDeliveryLineItem pod1Line1 = mock(ProofOfDeliveryLineItem.class);
    when(pod1Line1.getLotId()).thenReturn(product1Lot1Id);
    when(pod1Line1.getQuantityAccepted()).thenReturn(10);
    when(pod1Line1.getQuantityRejected()).thenReturn(10);
    when(pod1Line1.getRejectionReasonId()).thenReturn(reasonId);
    when(pod1Line1.getNotes()).thenReturn("123");
    when(pod1Line1.getOrderable()).thenReturn(orderable1);
    when(pod1.getLineItems()).thenReturn(singletonList(pod1Line1));
    return pod1;
  }

  private ProofOfDelivery mockPod2(VersionEntityReference orderable2) {
    ProofOfDelivery pod2 = mock(ProofOfDelivery.class);
    when(pod2.getReceivedDate()).thenReturn(LocalDate.of(2020, 11, 1));
    when(pod2.getDeliveredBy()).thenReturn("qla");
    when(pod2.getReceivedBy()).thenReturn("zjj");
    Shipment shipment2 = mock(Shipment.class);
    when(pod2.getShipment()).thenReturn(shipment2);
    Order order2 = mock(Order.class);
    when(shipment2.getOrder()).thenReturn(order2);
    when(shipment2.getShippedDate()).thenReturn(LocalDate.of(2020, 10, 2).atStartOfDay(ZoneId.systemDefault()));
    ShipmentLineItem shipment2Line1 = mock(ShipmentLineItem.class);
    when(shipment2Line1.getLotId()).thenReturn(product2Lot1Id);
    when(shipment2Line1.getQuantityShipped()).thenReturn(20L);
    when(shipment2Line1.getOrderable()).thenReturn(orderable2);
    when(shipment2.getLineItems()).thenReturn(singletonList(shipment2Line1));
    when(order2.getId()).thenReturn(order2Id);
    ProofOfDeliveryLineItem pod2Line1 = mock(ProofOfDeliveryLineItem.class);
    when(pod2Line1.getLotId()).thenReturn(product2Lot1Id);
    when(pod2Line1.getQuantityAccepted()).thenReturn(20);
    when(pod2Line1.getQuantityRejected()).thenReturn(0);
    when(pod2Line1.getNotes()).thenReturn("123");
    when(pod2Line1.getOrderable()).thenReturn(orderable2);
    when(pod2.getLineItems()).thenReturn(singletonList(pod2Line1));
    return pod2;
  }

  private ProofOfDelivery mockPod3(VersionEntityReference orderable1) {
    ProofOfDelivery pod3 = mock(ProofOfDelivery.class);
    Shipment shipment3 = mock(Shipment.class);
    when(pod3.getShipment()).thenReturn(shipment3);
    Order order3 = mock(Order.class);
    when(shipment3.getOrder()).thenReturn(order3);
    when(shipment3.getShippedDate()).thenReturn(LocalDate.of(2020, 11, 2).atStartOfDay(ZoneId.systemDefault()));
    when(order3.getId()).thenReturn(order3Id);
    ShipmentLineItem shipment3Line1 = mock(ShipmentLineItem.class);
    when(shipment3Line1.getLotId()).thenReturn(product1Lot1Id);
    when(shipment3Line1.getQuantityShipped()).thenReturn(20L);
    when(shipment3Line1.getOrderable()).thenReturn(orderable1);
    when(shipment3.getLineItems()).thenReturn(singletonList(shipment3Line1));
    ProofOfDeliveryLineItem pod3Line1 = mock(ProofOfDeliveryLineItem.class);
    when(pod3Line1.getLotId()).thenReturn(product1Lot1Id);
    when(pod3Line1.getQuantityAccepted()).thenReturn(null);
    when(pod3Line1.getQuantityRejected()).thenReturn(null);
    when(pod3Line1.getOrderable()).thenReturn(orderable1);
    when(pod3.getLineItems()).thenReturn(singletonList(pod3Line1));
    return pod3;
  }

  private void mockOrders() {
    FacilityDto supplyingFacility = mock(FacilityDto.class);
    when(supplyingFacility.getName()).thenReturn("Centro de Saude de ntopa");
    OrderableDto product1 = mock(OrderableDto.class);
    when(product1.getId()).thenReturn(product1Id);
    when(product1.getProductCode()).thenReturn("22A01");
    OrderableDto product2 = mock(OrderableDto.class);
    when(product2.getId()).thenReturn(product2Id);
    when(product2.getProductCode()).thenReturn("22B01");

    mockOrder1(supplyingFacility, product1);
    mockOrder2(supplyingFacility, product2);
    mockOrder3(supplyingFacility, product1);
  }

  private void mockOrder1(FacilityDto supplyingFacility, OrderableDto product1) {
    OrderDto order1 = mock(OrderDto.class);
    when(order1.getId()).thenReturn(order1Id);
    when(order1.getOrderCode()).thenReturn("ORDER-AS20JF");
    when(order1.getCreatedDate()).thenReturn(LocalDate.of(2020, 9, 2).atStartOfDay(ZoneId.systemDefault()));
    when(order1.getLastUpdatedDate())
        .thenReturn(LocalDate.of(2020, 9, 2).atTime(10, 15).atZone(ZoneId.systemDefault()));
    when(order1.getStatus()).thenReturn(OrderStatus.RECEIVED);
    when(order1.getRequisitionNumber()).thenReturn("RNR-NO01050119-0");
    when(order1.getEmergency()).thenReturn(false);
    when(order1.getSupplyingFacility()).thenReturn(supplyingFacility);
    ProcessingPeriodDto order1Period = new ProcessingPeriodDto();
    order1Period.setStartDate(LocalDate.of(2020, 7, 21));
    order1Period.setEndDate(LocalDate.of(2020, 8, 20));
    when(order1.getProcessingPeriod()).thenReturn(order1Period);
    ProgramDto order1Program = mock(ProgramDto.class);
    when(order1Program.getCode()).thenReturn("VC");
    when(order1.getProgram()).thenReturn(order1Program);
    OrderLineItemDto order1Line1 = mock(OrderLineItemDto.class);
    when(order1.orderLineItems()).thenReturn(singletonList(order1Line1));
    when(order1Line1.getOrderable()).thenReturn(product1);
    when(order1Line1.getOrderedQuantity()).thenReturn(20L);
    when(order1Line1.getPartialFulfilledQuantity()).thenReturn(0L);
    when(orderService.searchOrderByIdWithoutProducts(order1Id)).thenReturn(new SiglusOrderDto(order1, emptySet()));
  }

  private void mockOrder2(FacilityDto supplyingFacility, OrderableDto product2) {
    OrderDto order2 = mock(OrderDto.class);
    when(order2.getId()).thenReturn(order2Id);
    when(order2.getOrderCode()).thenReturn("ORDER-AS21JF");
    when(order2.getCreatedDate()).thenReturn(LocalDate.of(2020, 10, 2).atStartOfDay(ZoneId.systemDefault()));
    when(order2.getLastUpdatedDate())
        .thenReturn(LocalDate.of(2020, 10, 2).atTime(10, 15).atZone(ZoneId.systemDefault()));
    when(order2.getStatus()).thenReturn(OrderStatus.RECEIVED);
    when(order2.getRequisitionNumber()).thenReturn("RNR-NO01050120-0");
    when(order2.getEmergency()).thenReturn(false);
    when(order2.getSupplyingFacility()).thenReturn(supplyingFacility);
    ProcessingPeriodDto order2Period = new ProcessingPeriodDto();
    order2Period.setStartDate(LocalDate.of(2020, 8, 21));
    order2Period.setEndDate(LocalDate.of(2020, 9, 20));
    when(order2.getProcessingPeriod()).thenReturn(order2Period);
    ProgramDto order2Program = mock(ProgramDto.class);
    when(order2Program.getCode()).thenReturn("VC");
    when(order2.getProgram()).thenReturn(order2Program);
    OrderLineItemDto order2Line1 = mock(OrderLineItemDto.class);
    when(order2.orderLineItems()).thenReturn(singletonList(order2Line1));
    when(order2Line1.getOrderedQuantity()).thenReturn(20L);
    when(order2Line1.getPartialFulfilledQuantity()).thenReturn(0L);
    when(order2Line1.getOrderable()).thenReturn(product2);
    when(orderService.searchOrderByIdWithoutProducts(order2Id)).thenReturn(new SiglusOrderDto(order2, emptySet()));
  }

  private void mockOrder3(FacilityDto supplyingFacility, OrderableDto product1) {
    OrderDto order3 = mock(OrderDto.class);
    when(order3.getId()).thenReturn(order3Id);
    when(order3.getOrderCode()).thenReturn("ORDER-AS22JF");
    when(order3.getCreatedDate()).thenReturn(LocalDate.of(2020, 11, 2).atStartOfDay(ZoneId.systemDefault()));
    when(order3.getLastUpdatedDate())
        .thenReturn(LocalDate.of(2020, 11, 2).atTime(10, 15).atZone(ZoneId.systemDefault()));
    when(order3.getStatus()).thenReturn(OrderStatus.SHIPPED);
    when(order3.getRequisitionNumber()).thenReturn("RNR-NO01050121-0");
    when(order3.getEmergency()).thenReturn(false);
    when(order3.getSupplyingFacility()).thenReturn(supplyingFacility);
    ProcessingPeriodDto order3Period = new ProcessingPeriodDto();
    order3Period.setStartDate(LocalDate.of(2020, 9, 21));
    order3Period.setEndDate(LocalDate.of(2020, 10, 20));
    when(order3.getProcessingPeriod()).thenReturn(order3Period);
    ProgramDto order3Program = mock(ProgramDto.class);
    when(order3Program.getCode()).thenReturn("VC");
    when(order3.getProgram()).thenReturn(order3Program);
    OrderLineItemDto order3Line1 = mock(OrderLineItemDto.class);
    when(order3.orderLineItems()).thenReturn(singletonList(order3Line1));
    when(order3Line1.getOrderedQuantity()).thenReturn(20L);
    when(order3Line1.getPartialFulfilledQuantity()).thenReturn(0L);
    when(order3Line1.getOrderable()).thenReturn(product1);
    when(orderService.searchOrderByIdWithoutProducts(order3Id)).thenReturn(new SiglusOrderDto(order3, emptySet()));
  }

  // it's stupid to rewrite the logical what we have already impement in the source.
  // But using Mockito 1.x makes it kind of tricky to mock static method. So...
  private LocalDate getLastYear() {
    return YearMonth.now().minusMonths(13).atDay(1);
  }

}
