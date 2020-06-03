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

package org.openlmis.fulfillment.web.shipmentdraft;

import static org.openlmis.fulfillment.i18n.MessageKeys.CANNOT_CREATE_SHIPMENT_DRAFT_FOR_ORDER_WITH_WRONG_STATUS;
import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_DRAFT_ID_MISMATCH;
import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_DRAFT_ORDER_NOT_FOUND;
import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_DRAFT_ORDER_REQUIRED;
import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_NOT_FOUND;
import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_ORDERLESS_NOT_SUPPORTED;
import static org.openlmis.fulfillment.service.ResourceNames.BASE_PATH;
import static org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftController.RESOURCE_PATH;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.ShipmentDraft;
import org.openlmis.fulfillment.domain.UpdateDetails;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.repository.ShipmentDraftRepository;
import org.openlmis.fulfillment.service.PermissionService;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.util.DateHelper;
import org.openlmis.fulfillment.util.Pagination;
import org.openlmis.fulfillment.web.BaseController;
import org.openlmis.fulfillment.web.NotFoundException;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.fulfillment.web.util.ObjectReferenceDto;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@Transactional
@RequestMapping(RESOURCE_PATH)
public class ShipmentDraftController extends BaseController {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(ShipmentDraftController.class);

  static final String RESOURCE_PATH = BASE_PATH + "/shipmentDrafts";
  private static final String CHECK_RIGHTS = "CHECK_RIGHTS";

  @Autowired
  private ShipmentDraftRepository repository;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private ShipmentDraftDtoBuilder draftDtoBuilder;

  @Autowired
  private PermissionService permissionService;

  @Autowired
  private AuthenticationHelper authenticationHelper;

  @Autowired
  private DateHelper dateHelper;

  /**
   * Allows creating new shipment. If the id is specified, it will be ignored.
   *
   * @param draftDto A shipment draft DTO bound to the request body.
   * @return created shipment draft.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public ShipmentDraftDto createShipmentDraft(@RequestBody ShipmentDraftDto draftDto) {
    XLOGGER.entry(draftDto);
    Profiler profiler = new Profiler("CREATE_SHIPMENT_DRAFT");
    profiler.setLogger(XLOGGER);

    nullIds(draftDto);
    profiler.start("VALIDATE");
    validateOrder(draftDto.getOrder());

    profiler.start(CHECK_RIGHTS);
    permissionService.canEditShipmentDraft(draftDto);

    profiler.start("CREATE_DOMAIN_INSTANCE");
    ShipmentDraft draft = ShipmentDraft.newInstance(draftDto);

    Order order = orderRepository.findOne(draftDto.getOrder().getId());
    if (!order.isOrdered()) {
      throw new ValidationException(CANNOT_CREATE_SHIPMENT_DRAFT_FOR_ORDER_WITH_WRONG_STATUS,
          order.getStatus().toString());
    }

    profiler.start("SAVE_SHIPMENT_DTO");
    draft = repository.save(draft);

    profiler.start("UPDATE_ORDER");
    updateOrderStatus(order, OrderStatus.FULFILLING);
    orderRepository.save(order);

    profiler.start("BUILD_SHIPMENT_DTO");
    ShipmentDraftDto dto = draftDtoBuilder.build(draft);

    profiler.stop().log();
    XLOGGER.exit(dto);
    return dto;
  }

  /**
   * Allows update shipment. If draft does not exist, new one will be created.
   *
   * @param draftDto A shipment draft DTO bound to the request body.
   * @return created or updated shipment draft.
   */
  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ShipmentDraftDto updateShipmentDraft(@PathVariable UUID id,
                                              @RequestBody ShipmentDraftDto draftDto) {
    XLOGGER.entry(draftDto);
    Profiler profiler = new Profiler("UPDATE_SHIPMENT_DRAFT");
    profiler.setLogger(XLOGGER);

    profiler.start("VALIDATE");
    if (draftDto.getId() != null && !draftDto.getId().equals(id)) {
      throw new ValidationException(SHIPMENT_DRAFT_ID_MISMATCH);
    }
    validateOrder(draftDto.getOrder());

    profiler.start(CHECK_RIGHTS);
    permissionService.canEditShipmentDraft(draftDto);

    profiler.start("CREATE_DOMAIN_INSTANCE");
    ShipmentDraft newDraft = ShipmentDraft.newInstance(draftDto);

    profiler.start("FIND_EXISTING_DRAFT");
    ShipmentDraft draft;
    ShipmentDraft existingDraft = repository.findOne(id);
    if (existingDraft == null) {
      profiler.start("SAVE_NEW_DRAFT");
      draft = repository.save(newDraft);
    } else {
      profiler.start("UPDATE_AND_SAVE_DRAFT");
      existingDraft.updateFrom(newDraft);
      draft = repository.save(existingDraft);
    }

    profiler.start("CREATE_DTO");
    ShipmentDraftDto dto = draftDtoBuilder.build(draft);

    profiler.stop().log();
    XLOGGER.exit(dto);
    return dto;
  }

  /**
   * Get shipment with request param.
   *
   * @param orderId order UUID (required).
   * @return a page of shipment drafts.
   */
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<ShipmentDraftDto> getShipmentDrafts(@RequestParam(required = false) UUID orderId,
                                                  Pageable pageable) {
    XLOGGER.entry(orderId);
    Profiler profiler = new Profiler("GET_SHIPMENT_DRAFTS");
    profiler.setLogger(XLOGGER);

    profiler.start("VALIDATE");
    if (orderId == null) {
      throw new ValidationException(SHIPMENT_DRAFT_ORDER_REQUIRED);
    }

    Order order = orderRepository.findOne(orderId);
    if (order == null) {
      throw new ValidationException(SHIPMENT_DRAFT_ORDER_NOT_FOUND);
    }

    profiler.start(CHECK_RIGHTS);
    permissionService.canViewShipmentDraft(order);

    profiler.start("FIND_BY_ORDER_AND_BUILD_DTO");
    Page<ShipmentDraft> draftPage = repository.findByOrder(order, pageable);
    List<ShipmentDraftDto> draftDtos = draftDtoBuilder.build(draftPage.getContent());

    int numberOfElements = draftPage.getNumberOfElements();
    Page<ShipmentDraftDto> page = Pagination.getPage(draftDtos, pageable, numberOfElements);

    profiler.stop().log();
    XLOGGER.exit(page);
    return page;
  }

  /**
   * Get chosen shipment draft.
   *
   * @param id UUID of shipment draft item which we want to get.
   * @return new instance of ShipmentDraftDto.
   */
  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ShipmentDraftDto getShipmentDraft(@PathVariable UUID id,
                                           @RequestParam(required = false) Set<String> expand) {
    XLOGGER.entry(id);
    Profiler profiler = new Profiler("GET_SHIPMENT_DRAFT_BY_ID");
    profiler.setLogger(XLOGGER);

    ShipmentDraft shipment = findShipmentDraft(id, profiler);

    profiler.start(CHECK_RIGHTS);
    permissionService.canViewShipmentDraft(shipment);

    profiler.start("CREATE_DTO");
    ShipmentDraftDto dto = draftDtoBuilder.build(shipment);
    expandDto(dto, expand);

    profiler.stop().log();
    XLOGGER.exit(dto);
    return dto;
  }

  /**
   * Delete chosen shipment draft.
   *
   * @param id UUID of shipment draft item which we want to delete.
   */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteShipmentDraft(@PathVariable UUID id) {
    XLOGGER.entry(id);
    Profiler profiler = new Profiler("DELETE_SHIPMENT_DRAFT");
    profiler.setLogger(XLOGGER);

    ShipmentDraft shipment = findShipmentDraft(id, profiler);

    profiler.start(CHECK_RIGHTS);
    permissionService.canEditShipmentDraft(shipment);

    profiler.start("DELETE");
    repository.delete(id);

    Order order = orderRepository.findOne(shipment.getOrder().getId());

    profiler.start("UPDATE_ORDER");
    updateOrderStatus(order, OrderStatus.ORDERED);
    orderRepository.save(order);

    profiler.stop().log();
    XLOGGER.exit();
  }

  private void nullIds(ShipmentDraftDto shipmentDto) {
    shipmentDto.setId(null);
    if (shipmentDto.lineItems() != null) {
      shipmentDto.lineItems().forEach(l -> l.setId(null));
    }
  }

  private ShipmentDraft findShipmentDraft(UUID id, Profiler profiler) {
    profiler.start("FIND_IN_DB");
    ShipmentDraft shipment = repository.findOne(id);
    if (shipment == null) {
      throw new NotFoundException(SHIPMENT_NOT_FOUND, id.toString());
    }
    return shipment;
  }

  private void validateOrder(ObjectReferenceDto dtoOrder) {
    if (dtoOrder == null || dtoOrder.getId() == null) {
      throw new ValidationException(SHIPMENT_ORDERLESS_NOT_SUPPORTED);
    }
  }

  private void updateOrderStatus(Order order, OrderStatus status) {
    UserDto currentUser = authenticationHelper.getCurrentUser();
    order.updateStatus(status, new UpdateDetails(currentUser.getId(),
        dateHelper.getCurrentDateTimeWithSystemZone()));
  }
}
