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

package org.openlmis.fulfillment.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.fulfillment.domain.FtpTransferProperties;
import org.openlmis.fulfillment.domain.LocalTransferProperties;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.StatusChange;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.service.referencedata.BaseReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.FacilityReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.OrderableReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.PeriodReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.ProgramReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.UserReferenceDataService;
import org.openlmis.fulfillment.web.util.StatusChangeDto;
import org.openlmis.fulfillment.web.util.VersionIdentityDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ExporterBuilder {

  @Autowired
  private FacilityReferenceDataService facilities;

  @Autowired
  private ProgramReferenceDataService programs;

  @Autowired
  private UserReferenceDataService users;

  @Autowired
  private PeriodReferenceDataService periods;

  @Autowired
  private OrderableReferenceDataService products;

  @Value("${service.url}")
  private String serviceUrl;

  /**
   * Copy data from the given order to the instance that implemenet {@link Order.Exporter}
   * interface.
   */
  public void export(Order order, Order.Exporter exporter) {
    exporter.setServiceUrl(serviceUrl);
    exporter.setId(order.getId());
    exporter.setExternalId(order.getExternalId());
    exporter.setEmergency(order.getEmergency());
    exporter.setFacility(getIfPresent(facilities, order.getFacilityId()));
    exporter.setProgram(getIfPresent(programs, order.getProgramId()));
    exporter.setProcessingPeriod(getIfPresent(periods, order.getProcessingPeriodId()));
    exporter.setRequestingFacility(getIfPresent(facilities, order.getRequestingFacilityId()));
    exporter.setReceivingFacility(getIfPresent(facilities, order.getReceivingFacilityId()));
    exporter.setSupplyingFacility(getIfPresent(facilities, order.getSupplyingFacilityId()));
    exporter.setOrderCode(order.getOrderCode());
    exporter.setStatus(order.getStatus());
    exporter.setQuotedCost(order.getQuotedCost());
    exporter.setCreatedBy(getIfPresent(users, order.getCreatedById()));
    exporter.setCreatedDate(order.getCreatedDate());
    exporter.setUpdateDetails(order.getUpdateDetails());
  }

  /**
   * Copy data from the given order line item to the instance that implement
   * {@link OrderLineItem.Exporter} interface.
   */
  public void export(OrderLineItem item, OrderLineItem.Exporter exporter,
                     List<OrderableDto> orderables) {
    exporter.setId(item.getId());
    Optional<OrderableDto> orderableOptional = orderables.stream().filter(
        orderable -> new VersionIdentityDto(orderable.getId(), orderable.getVersionNumber()).equals(
            new VersionIdentityDto(item.getOrderable()))).findAny();
    OrderableDto orderableDto = orderableOptional
        .orElse(getIfPresent(products, item.getOrderable().getId()));

    exporter.setOrderable(orderableDto);
    exporter.setOrderedQuantity(item.getOrderedQuantity());
    if (orderableDto != null) {
      exporter.setTotalDispensingUnits(item.getOrderedQuantity() * orderableDto.getNetContent());
    }
  }

  /**
   * Copy data from the given local transfer properties to the instance that implement
   * {@link LocalTransferProperties.Exporter} interface.
   */
  public void export(LocalTransferProperties properties,
                     LocalTransferProperties.Exporter exporter) {
    exporter.setId(properties.getId());
    exporter.setFacility(getIfPresent(facilities, properties.getFacilityId()));
    exporter.setTransferType(properties.getTransferType());
    exporter.setPath(properties.getPath());
  }

  /**
   * Copy data from the given ftp transfer properties to the instance that implement
   * {@link FtpTransferProperties.Exporter} interface.
   */
  public void export(FtpTransferProperties properties,
                     FtpTransferProperties.Exporter exporter) {
    exporter.setId(properties.getId());
    exporter.setFacility(getIfPresent(facilities, properties.getFacilityId()));
    exporter.setTransferType(properties.getTransferType());
    exporter.setProtocol(properties.getProtocol().name());
    exporter.setUsername(properties.getUsername());
    exporter.setServerHost(properties.getServerHost());
    exporter.setServerPort(properties.getServerPort());
    exporter.setRemoteDirectory(properties.getRemoteDirectory());
    exporter.setLocalDirectory(properties.getLocalDirectory());
    exporter.setPassiveMode(properties.getPassiveMode());
  }

  /**
   * Copy data from given statusChanges to newly initialized StatusChangeDto list.
   * @param statusChanges the status changes to convert
   * @return list of StatusChangeDto
   */
  public List<StatusChangeDto> convertToDtos(List<StatusChange> statusChanges) {
    List<StatusChangeDto> dtos = new ArrayList<>();
    for (StatusChange statusChange : statusChanges) {
      StatusChangeDto dto = StatusChangeDto.newInstance(statusChange);
      dto.setAuthor(getIfPresent(users, dto.getAuthorId()));
      dtos.add(dto);
    }
    return dtos;
  }

  /**
   * Fetch orderables for each line item of given order.
   * @param order related order
   * @return a list of orderable dtos
   */
  public List<OrderableDto> getLineItemOrderables(Order order) {
    Set<VersionEntityReference> identities = order.getOrderLineItems()
        .stream()
        .map(OrderLineItem::getOrderable).collect(Collectors.toSet());

    return products.findByIdentities(identities);
  }

  private <T> T getIfPresent(BaseReferenceDataService<T> service, UUID id) {
    return Optional.ofNullable(id).isPresent() ? service.findOne(id) : null;
  }

}
