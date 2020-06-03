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

package org.openlmis.fulfillment.web.util;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.openlmis.fulfillment.i18n.MessageKeys.EVENT_MISSING_SOURCE_DESTINATION;
import static org.openlmis.fulfillment.web.stockmanagement.StockEventLineItemDto.QUANTITY_REJECTED;
import static org.openlmis.fulfillment.web.stockmanagement.StockEventLineItemDto.REJECTION_REASON_ID;
import static org.openlmis.fulfillment.web.stockmanagement.StockEventLineItemDto.VVM_STATUS;

import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.javers.common.collections.Sets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.ProofOfDeliveryDataBuilder;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.service.ConfigurationSettingService;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.FacilityReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.OrderableReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.service.stockmanagement.ValidDestinationsStockManagementService;
import org.openlmis.fulfillment.service.stockmanagement.ValidSourcesStockManagementService;
import org.openlmis.fulfillment.testutils.DtoGenerator;
import org.openlmis.fulfillment.testutils.OrderableDataBuilder;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.util.DateHelper;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.stockmanagement.NodeDto;
import org.openlmis.fulfillment.web.stockmanagement.StockEventDto;
import org.openlmis.fulfillment.web.stockmanagement.StockEventLineItemDto;
import org.openlmis.fulfillment.web.stockmanagement.ValidSourceDestinationDto;
import org.openlmis.fulfillment.web.stockmanagement.ValidSourceDestinationDtoDataBuilder;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnusedPrivateField"})
public class StockEventBuilderTest {
  private static final UUID TRANSFER_IN_REASON_ID = UUID.randomUUID();
  private static final LocalDate CURRENT_DATE = LocalDate.now();
  private static final Long NET_CONTENT = 76L;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private ValidDestinationsStockManagementService validDestinationsStockManagementService;

  @Mock
  private ValidSourcesStockManagementService validSourcesStockManagementService;

  @Mock
  private ConfigurationSettingService configurationSettingService;

  @Mock
  private OrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @Mock
  private DateHelper dateHelper;

  @InjectMocks
  private StockEventBuilder stockEventBuilder;

  private FacilityDto toFacilityDto = DtoGenerator.of(FacilityDto.class);
  private FacilityDto fromFacilityDto = DtoGenerator.of(FacilityDto.class);
  private ValidSourceDestinationDto destination = new ValidSourceDestinationDtoDataBuilder()
      .withNode(toFacilityDto.getId())
      .build();
  private NodeDto node = destination.getNode();
  private ProofOfDelivery proofOfDelivery = new ProofOfDeliveryDataBuilder().build();
  private Shipment shipment = proofOfDelivery.getShipment();
  private Order order = shipment.getOrder();
  private UserDto user = DtoGenerator.of(UserDto.class);

  @Before
  public void setUp() {
    when(facilityReferenceDataService.findOne(order.getReceivingFacilityId()))
        .thenReturn(toFacilityDto);
    when(facilityReferenceDataService.findOne(order.getSupplyingFacilityId()))
        .thenReturn(toFacilityDto);

    when(validDestinationsStockManagementService
        .search(order.getProgramId(), fromFacilityDto.getId(), toFacilityDto.getId()))
        .thenReturn(Optional.of(destination));

    when(validSourcesStockManagementService
        .search(order.getProgramId(), fromFacilityDto.getId(), toFacilityDto.getId()))
        .thenReturn(Optional.of(destination));

    when(configurationSettingService.getTransferInReasonId())
        .thenReturn(TRANSFER_IN_REASON_ID);

    when(dateHelper.getCurrentDate()).thenReturn(CURRENT_DATE);
    when(authenticationHelper.getCurrentUser()).thenReturn(user);
  }

  @Test
  public void shouldCreateEventFromShipment() {
    final OrderableDto orderable = new OrderableDataBuilder()
        .withId(shipment.getLineItems().get(0).getOrderable().getId())
        .withVersionNumber(shipment.getLineItems().get(0).getOrderable().getVersionNumber())
        .withNetContent(NET_CONTENT)
        .build();
    when(orderableReferenceDataService.findByIdentities(Sets.asSet(new VersionEntityReference(
        orderable.getId(), orderable.getVersionNumber()))))
        .thenReturn(Lists.newArrayList(orderable));

    StockEventDto event = stockEventBuilder.fromShipment(shipment);

    assertThat(event.getFacilityId(), is(order.getSupplyingFacilityId()));
    assertThat(event.getProgramId(), is(order.getProgramId()));
    assertThat(event.getUserId(), is(shipment.getShippedById()));

    assertThat(event.getLineItems(), hasSize(shipment.getLineItems().size()));
    assertEventLineItemOfShipment(event.getLineItems().get(0), orderable);
  }

  @Test
  public void shouldCreateEventFromProofOfDelivery() {
    final OrderableDto orderable = new OrderableDataBuilder()
        .withId(proofOfDelivery.getLineItems().get(0).getOrderable().getId())
        .withVersionNumber(proofOfDelivery.getLineItems().get(0).getOrderable().getVersionNumber())
        .withNetContent(NET_CONTENT)
        .build();
    when(orderableReferenceDataService.findByIdentities(Sets.asSet(new VersionEntityReference(
        orderable.getId(), orderable.getVersionNumber()))))
        .thenReturn(Lists.newArrayList(orderable));

    StockEventDto event = stockEventBuilder.fromProofOfDelivery(proofOfDelivery);

    assertThat(event.getFacilityId(), is(order.getReceivingFacilityId()));
    assertThat(event.getProgramId(), is(order.getProgramId()));
    assertThat(event.getUserId(), is(user.getId()));

    assertThat(event.getLineItems(), hasSize(proofOfDelivery.getLineItems().size()));
    assertEventLineItemOfProofOfDelivery(event.getLineItems().get(0), orderable);
  }

  @Test
  public void shouldThrowExceptionIfDestinationCannotBeFound() {
    exception.expect(ValidationException.class);
    exception.expectMessage(EVENT_MISSING_SOURCE_DESTINATION);

    when(validDestinationsStockManagementService.search(any(), any(), any()))
        .thenReturn(Optional.empty());

    stockEventBuilder.fromShipment(shipment);
  }

  @Test
  public void shouldThrowExceptionIfSourceCannotBeFound() {
    exception.expect(ValidationException.class);
    exception.expectMessage(EVENT_MISSING_SOURCE_DESTINATION);

    when(validSourcesStockManagementService.search(any(), any(), any()))
        .thenReturn(Optional.empty());

    stockEventBuilder.fromProofOfDelivery(proofOfDelivery);
  }

  private void assertEventLineItemOfShipment(StockEventLineItemDto eventLine,
      OrderableDto orderableDto) {
    ShipmentLineItemDto dto = new ShipmentLineItemDto();
    shipment.getLineItems().get(0).export(dto, orderableDto);

    assertThat(eventLine.getOrderableId(), is(dto.getOrderable().getId()));
    assertThat(eventLine.getLotId(), is(dto.getLotId()));
    assertThat(eventLine.getQuantity(),
        is(dto.getQuantityShipped().intValue() * NET_CONTENT.intValue()));
    assertThat(eventLine.getOccurredDate(), is(CURRENT_DATE));
    assertThat(eventLine.getDestinationId(), is(node.getId()));
  }

  private void assertEventLineItemOfProofOfDelivery(StockEventLineItemDto eventLine,
      OrderableDto orderableDto) {
    ProofOfDeliveryLineItemDto dto = new ProofOfDeliveryLineItemDto();
    proofOfDelivery.getLineItems().get(0).export(dto, orderableDto);

    assertThat(eventLine.getOrderableId(), is(dto.getOrderable().getId()));
    assertThat(eventLine.getLotId(), is(dto.getLotId()));
    assertThat(eventLine.getQuantity(),
        is(dto.getQuantityAccepted() * NET_CONTENT.intValue()));
    assertThat(eventLine.getOccurredDate(), is(proofOfDelivery.getReceivedDate()));
    assertThat(eventLine.getSourceId(), is(node.getId()));
    assertThat(eventLine.getReasonId(), is(TRANSFER_IN_REASON_ID));
    assertThat(
        eventLine.getExtraData(),
        allOf(
            hasEntry(VVM_STATUS, dto.getVvmStatus().toString()),
            hasEntry(QUANTITY_REJECTED, dto.getQuantityRejected().toString()),
            hasEntry(REJECTION_REASON_ID, dto.getRejectionReasonId().toString())
        )
    );
  }
}
