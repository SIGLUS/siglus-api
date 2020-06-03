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

import static org.openlmis.fulfillment.i18n.MessageKeys.EVENT_MISSING_SOURCE_DESTINATION;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.service.ConfigurationSettingService;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.FacilityReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.OrderableReferenceDataService;
import org.openlmis.fulfillment.service.stockmanagement.ValidDestinationsStockManagementService;
import org.openlmis.fulfillment.service.stockmanagement.ValidSourceDestinationsStockManagementService;
import org.openlmis.fulfillment.service.stockmanagement.ValidSourcesStockManagementService;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.util.DateHelper;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.fulfillment.web.stockmanagement.StockEventDto;
import org.openlmis.fulfillment.web.stockmanagement.StockEventLineItemDto;
import org.openlmis.fulfillment.web.stockmanagement.ValidSourceDestinationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StockEventBuilder {
  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(StockEventBuilder.class);
  private static final Logger LOGGER = LoggerFactory.getLogger(StockEventBuilder.class);

  @Autowired
  private FacilityReferenceDataService facilityReferenceDataService;

  @Autowired
  private ValidDestinationsStockManagementService validDestinationsStockManagementService;

  @Autowired
  private ValidSourcesStockManagementService validSourcesStockManagementService;

  @Autowired
  private ConfigurationSettingService configurationSettingService;

  @Autowired
  private AuthenticationHelper authenticationHelper;

  @Autowired
  private OrderableReferenceDataService orderableReferenceDataService;

  @Autowired
  private DateHelper dateHelper;

  /**
   * Builds a stock event DTO from the given shipment.
   */
  public StockEventDto fromShipment(Shipment shipment) {
    XLOGGER.entry(shipment);
    Profiler profiler = new Profiler("BUILD_STOCK_EVENT_FROM_SHIPMENT");
    profiler.setLogger(XLOGGER);

    LOGGER.debug("Building stock events for shipment: {}", shipment.getId());

    profiler.start("BUILD_STOCK_EVENT");
    StockEventDto stockEventDto = new StockEventDto(
        shipment.getProgramId(), shipment.getSupplyingFacilityId(),
        getLineItems(shipment, profiler), shipment.getShippedById()
    );

    profiler.stop().log();
    XLOGGER.exit(stockEventDto);
    return stockEventDto;
  }

  /**
   * Builds a stock event DTO from the given proof of delivery.
   */
  public StockEventDto fromProofOfDelivery(ProofOfDelivery proofOfDelivery) {
    XLOGGER.entry(proofOfDelivery);
    Profiler profiler = new Profiler("BUILD_STOCK_EVENT_FROM_POD");
    profiler.setLogger(XLOGGER);

    LOGGER.debug("Building stock events for proof of delivery: {}", proofOfDelivery.getId());

    profiler.start("BUILD_STOCK_EVENT");
    StockEventDto stockEventDto = new StockEventDto(
        proofOfDelivery.getProgramId(), proofOfDelivery.getReceivingFacilityId(),
        getLineItems(proofOfDelivery, profiler), authenticationHelper.getCurrentUser().getId()
    );

    profiler.stop().log();
    XLOGGER.exit(stockEventDto);
    return stockEventDto;
  }

  private List<StockEventLineItemDto> getLineItems(Shipment shipment, Profiler profiler) {
    profiler.start("GET_ORDERABLE_IDENTITIES");
    Set<VersionEntityReference> orderableIdentities = shipment
        .getLineItems()
        .stream()
        .map(ShipmentLineItem::getOrderable)
        .collect(Collectors.toSet());

    profiler.start("GET_ORDERABLES_BY_IDENTITIES");
    Map<VersionIdentityDto, OrderableDto> orderables = getOrderables(orderableIdentities);

    profiler.start("GET_DESTINATION_ID");
    UUID destinationId = getDestinationId(
        shipment.getSupplyingFacilityId(),
        shipment.getReceivingFacilityId(),
        shipment.getProgramId()
    );

    profiler.start("CREATE_STOCK_EVENT_LINE_ITEMS");
    return shipment
        .getLineItems()
        .stream()
        .map(lineItem -> createLineItem(lineItem, orderables, destinationId))
        .collect(Collectors.toList());
  }

  private List<StockEventLineItemDto> getLineItems(ProofOfDelivery proofOfDelivery,
      Profiler profiler) {
    profiler.start("GET_ORDERABLE_IDENTITIES");
    Set<VersionEntityReference> orderableIdentities = proofOfDelivery
        .getLineItems()
        .stream()
        .map(ProofOfDeliveryLineItem::getOrderable)
        .collect(Collectors.toSet());

    profiler.start("GET_ORDERABLES_BY_IDENTITIES");
    Map<VersionIdentityDto, OrderableDto> orderables = getOrderables(orderableIdentities);

    profiler.start("GET_SOURCE_ID");
    UUID sourceId = getSourceId(
        proofOfDelivery.getReceivingFacilityId(),
        proofOfDelivery.getSupplyingFacilityId(),
        proofOfDelivery.getProgramId()
    );

    profiler.start("CREATE_STOCK_EVENT_LINE_ITEMS");
    return proofOfDelivery
        .getLineItems()
        .stream()
        .map(lineItem -> createLineItem(proofOfDelivery, lineItem, orderables, sourceId))
        .collect(Collectors.toList());
  }

  private StockEventLineItemDto createLineItem(ShipmentLineItem lineItem,
      Map<VersionIdentityDto, OrderableDto> orderables, UUID destinationId) {
    StockEventLineItemDto dto = new StockEventLineItemDto();
    dto.setOccurredDate(dateHelper.getCurrentDate());
    dto.setDestinationId(destinationId);

    final OrderableDto orderableDto = orderables.get(
        new VersionIdentityDto(lineItem.getOrderable()));

    dto.setOrderableId(orderableDto.getId());
    lineItem.export(dto, orderableDto);
    convertQuantityToDispensingUnits(dto, orderableDto, orderables);

    return dto;
  }

  private StockEventLineItemDto createLineItem(ProofOfDelivery proofOfDelivery,
      ProofOfDeliveryLineItem lineItem, Map<VersionIdentityDto, OrderableDto> orderables,
      UUID sourceId) {
    StockEventLineItemDto dto = new StockEventLineItemDto();
    dto.setOccurredDate(proofOfDelivery.getReceivedDate());
    dto.setSourceId(sourceId);
    dto.setReasonId(configurationSettingService.getTransferInReasonId());

    final OrderableDto orderableDto = orderables.get(
        new VersionIdentityDto(lineItem.getOrderable()));

    dto.setOrderableId(orderableDto.getId());
    lineItem.export(dto, orderableDto);
    convertQuantityToDispensingUnits(dto, orderableDto, orderables);

    return dto;
  }

  private Map<VersionIdentityDto, OrderableDto> getOrderables(
      Set<VersionEntityReference> orderableIdentities) {
    return orderableReferenceDataService
        .findByIdentities(orderableIdentities)
        .stream()
        .collect(Collectors.toMap(OrderableDto::getIdentity, orderable -> orderable));
  }

  private void convertQuantityToDispensingUnits(StockEventLineItemDto dto,
      OrderableDto orderableDto, Map<VersionIdentityDto, OrderableDto> orderables) {
    VersionIdentityDto dtoIdentifier = new VersionIdentityDto(
        dto.getOrderableId(), orderableDto.getVersionNumber());

    orderables.computeIfPresent(dtoIdentifier,
        (identity, orderable) -> {
        Long netContent = orderables.get(new VersionIdentityDto(
            dto.getOrderableId(), orderable.getVersionNumber())).getNetContent();
        dto.setQuantity((int) (dto.getQuantity() * netContent));
        return orderable;
      });

  }

  private UUID getDestinationId(UUID source, UUID destination, UUID programId) {
    return getNodeId(
        source, destination, programId, validDestinationsStockManagementService
    );
  }

  private UUID getSourceId(UUID destination, UUID source, UUID programId) {
    return getNodeId(
        destination, source, programId, validSourcesStockManagementService
    );
  }

  private UUID getNodeId(UUID fromFacilityId, UUID toFacilityId, UUID programId,
                         ValidSourceDestinationsStockManagementService service) {
    FacilityDto fromFacility = facilityReferenceDataService.findOne(fromFacilityId);
    FacilityDto toFacility = facilityReferenceDataService.findOne(toFacilityId);

    Optional<ValidSourceDestinationDto> response = service
        .search(programId, fromFacility.getId(), toFacility.getId());

    if (response.isPresent()) {
      return response.get().getNode().getId();
    }

    throw new ValidationException(EVENT_MISSING_SOURCE_DESTINATION, toFacility.getCode());
  }

}
