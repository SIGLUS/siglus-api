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

package org.openlmis.fulfillment.web.shipment;

import static org.openlmis.fulfillment.i18n.MessageKeys.ORDER_NOT_FOUND;
import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_NOT_FOUND;
import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_ORDERLESS_NOT_SUPPORTED;
import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_ORDER_REQUIRED;
import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_ORDER_STATUS_INVALID;
import static org.openlmis.fulfillment.service.ResourceNames.BASE_PATH;
import static org.openlmis.fulfillment.web.shipment.ShipmentController.RESOURCE_PATH;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.openlmis.fulfillment.domain.CreationDetails;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.ShipmentDraft;
import org.openlmis.fulfillment.domain.UpdateDetails;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.repository.ShipmentDraftRepository;
import org.openlmis.fulfillment.repository.ShipmentRepository;
import org.openlmis.fulfillment.service.PermissionService;
import org.openlmis.fulfillment.service.ShipmentService;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.service.stockmanagement.StockEventStockManagementService;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.util.DateHelper;
import org.openlmis.fulfillment.util.Pagination;
import org.openlmis.fulfillment.web.BaseController;
import org.openlmis.fulfillment.web.NotFoundException;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.fulfillment.web.stockmanagement.StockEventDto;
import org.openlmis.fulfillment.web.util.ObjectReferenceDto;
import org.openlmis.fulfillment.web.util.StockEventBuilder;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@Transactional
@RequestMapping(RESOURCE_PATH)
public class ShipmentController extends BaseController {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(ShipmentController.class);

  static final String RESOURCE_PATH = BASE_PATH + "/shipments";

  @Autowired
  private ShipmentRepository shipmentRepository;

  @Autowired
  private ShipmentDtoBuilder shipmentDtoBuilder;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private ShipmentDraftRepository shipmentDraftRepository;

  @Autowired
  private AuthenticationHelper authenticationHelper;

  @Autowired
  private DateHelper dateHelper;

  @Autowired
  private PermissionService permissionService;

  @Autowired
  private StockEventStockManagementService stockEventService;

  @Autowired
  private StockEventBuilder stockEventBuilder;

  @Autowired
  private ShipmentService shipmentService;

  /**
   * Allows creating new shipment. If the id is specified, it will be ignored.
   *
   * @param shipmentDto A shipment item bound to the request body.
   * @return created shipment.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public ShipmentDto createShipment(@RequestBody ShipmentDto shipmentDto) {
    XLOGGER.entry(shipmentDto);
    Profiler profiler = new Profiler("CREATE_SHIPMENT");
    profiler.setLogger(XLOGGER);

    nullIds(shipmentDto);
    ObjectReferenceDto dtoOrder = shipmentDto.getOrder();
    if (dtoOrder == null || dtoOrder.getId() == null) {
      throw new ValidationException(SHIPMENT_ORDERLESS_NOT_SUPPORTED);
    }

    profiler.start("CHECK_RIGHTS");
    permissionService.canEditShipment(shipmentDto);

    profiler.start("CREATE_DOMAIN_INSTANCE");
    setShipDetailsToDto(shipmentDto);

    Order order = orderRepository.findOne(dtoOrder.getId());

    if (!order.canBeFulfilled()) {
      throw new ValidationException(SHIPMENT_ORDER_STATUS_INVALID, order.getStatus().toString());
    }

    Shipment shipment = Shipment.newInstance(shipmentDto, order);

    profiler.start("SAVE_SHIPMENT");
    shipment = shipmentService.save(shipment);

    profiler.start("UPDATE_ORDER");
    order.updateStatus(OrderStatus.SHIPPED, new UpdateDetails(
        authenticationHelper.getCurrentUser().getId(),
        dateHelper.getCurrentDateTimeWithSystemZone()));
    orderRepository.save(order);

    profiler.start("REMOVE_DRAFT_SHIPMENTS");
    findAndRemoveShipmentDraftsForOrder(order);

    profiler.start("BUILD_STOCK_EVENT_FROM_SHIPMENT");
    StockEventDto stockEventDto = stockEventBuilder.fromShipment(shipment);

    profiler.start("SUBMIT_STOCK_EVENT");
    stockEventService.submit(stockEventDto);

    profiler.start("BUILD_SHIPMENT_DTO");
    ShipmentDto dto = shipmentDtoBuilder.build(shipment);

    profiler.stop().log();
    XLOGGER.exit(dto);
    return dto;
  }

  /**
   * Get shipment with request param.
   *
   * @param orderId order UUID (required).
   * @return a page of shipments.
   */
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<ShipmentDto> getShipments(@RequestParam(required = false) UUID orderId,
                                                  Pageable pageable) {
    XLOGGER.entry(orderId);
    Profiler profiler = new Profiler("GET_SHIPMENTS");
    profiler.setLogger(XLOGGER);

    profiler.start("VALIDATE");
    if (orderId == null) {
      throw new ValidationException(SHIPMENT_ORDER_REQUIRED);
    }

    Order order = orderRepository.findOne(orderId);
    if (order == null) {
      throw new ValidationException(ORDER_NOT_FOUND, orderId.toString());
    }

    profiler.start("CHECK_RIGHTS");
    permissionService.canViewShipment(order);

    profiler.start("FIND_BY_ORDER_AND_BUILD_DTO");
    Page<Shipment> shipmentPage = shipmentRepository.findByOrder(order, pageable);
    List<ShipmentDto> shipmentDtos = shipmentDtoBuilder.build(shipmentPage.getContent());

    Page<ShipmentDto> page =
        Pagination.getPage(shipmentDtos, pageable, shipmentPage.getNumberOfElements());

    profiler.stop().log();
    XLOGGER.exit(page);
    return page;
  }

  /**
   * Get chosen shipment.
   *
   * @param id UUID of shipment item which we want to get
   * @return shipment.
   */
  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ShipmentDto getShipment(@PathVariable UUID id,
                                 @RequestParam(required = false) Set<String> expand) {
    XLOGGER.entry(id);
    Profiler profiler = new Profiler("GET_SHIPMENT_BY_ID");
    profiler.setLogger(XLOGGER);

    profiler.start("FIND_IN_DB");
    Shipment shipment = shipmentRepository.findOne(id);

    if (shipment == null) {
      throw new NotFoundException(SHIPMENT_NOT_FOUND, id.toString());
    }

    profiler.start("CHECK_RIGHTS");
    permissionService.canViewShipment(shipment);

    profiler.start("CREATE_DTO");
    ShipmentDto dto = shipmentDtoBuilder.build(shipment);
    expandDto(dto, expand);

    profiler.stop().log();
    XLOGGER.exit(dto);
    return dto;
  }

  private void nullIds(ShipmentDto shipmentDto) {
    shipmentDto.setId(null);
    if (shipmentDto.lineItems() != null) {
      shipmentDto.lineItems().forEach(l -> l.setId(null));
    }
  }

  private void setShipDetailsToDto(ShipmentDto shipmentDto) {
    UserDto currentUser = authenticationHelper.getCurrentUser();
    UUID userId = currentUser == null ? shipmentDto.getShippedBy().getId() : currentUser.getId();

    shipmentDto.setShipDetails(
        new CreationDetails(userId, dateHelper.getCurrentDateTimeWithSystemZone()));
  }

  private void findAndRemoveShipmentDraftsForOrder(Order order) {
    Collection<ShipmentDraft> drafts = shipmentDraftRepository.findByOrder(order);
    for (ShipmentDraft draft : drafts) {
      shipmentDraftRepository.delete(draft);
    }
  }
}
