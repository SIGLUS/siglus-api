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

import static org.apache.commons.lang.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.openlmis.fulfillment.i18n.MessageKeys.ORDER_NOT_FOUND;

import java.util.UUID;
import javax.validation.constraints.NotNull;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.ShipmentDraft;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.referencedata.PermissionStrings;
import org.openlmis.fulfillment.service.referencedata.RightDto;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.service.referencedata.UserReferenceDataService;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.web.MissingPermissionException;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.openlmis.fulfillment.web.util.ObjectReferenceDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("PMD.TooManyMethods")
public class PermissionService {
  static final String ORDERS_TRANSFER = "ORDERS_TRANSFER";
  public static final String PODS_MANAGE = "PODS_MANAGE";
  public static final String PODS_VIEW = "PODS_VIEW";
  public static final String ORDERS_VIEW = "ORDERS_VIEW";
  public static final String ORDERS_EDIT = "ORDERS_EDIT";
  public static final String SHIPMENTS_VIEW = "SHIPMENTS_VIEW";
  public static final String SHIPMENTS_EDIT = "SHIPMENTS_EDIT";
  static final String SYSTEM_SETTINGS_MANAGE = "SYSTEM_SETTINGS_MANAGE";

  @Autowired
  private UserReferenceDataService userReferenceDataService;

  @Autowired
  private AuthenticationHelper authenticationHelper;

  @Autowired
  private OrderRepository orderRepository;

  @Value("${auth.server.clientId}")
  private String serviceTokenClientId;

  @Value("${auth.server.clientId.apiKey.prefix}")
  private String apiKeyPrefix;

  @Autowired
  private PermissionStrings permissionStrings;

  public void canTransferOrder(Order order) {
    checkPermission(ORDERS_TRANSFER, order.getSupplyingFacilityId());
  }

  /**
   * Checks if user has permission to manage PoD.
   */
  public void canManagePod(ProofOfDelivery proofOfDelivery) {
    checkPermission(
        PODS_MANAGE, proofOfDelivery.getReceivingFacilityId(), proofOfDelivery.getProgramId()
    );
  }

  /**
   * Checks if user has permission to view PoD.
   */
  public void canViewPod(ProofOfDelivery proofOfDelivery) {
    UUID receivingFacilityId = proofOfDelivery.getReceivingFacilityId();
    UUID supplyingFacilityId = proofOfDelivery.getSupplyingFacilityId();
    UUID programId = proofOfDelivery.getProgramId();

    if (hasPermission(PODS_MANAGE, receivingFacilityId, programId)
        || hasPermission(PODS_VIEW, receivingFacilityId, programId)
        || hasPermission(SHIPMENTS_EDIT, supplyingFacilityId)) {
      return;
    }

    throw new MissingPermissionException(PODS_MANAGE, PODS_VIEW, SHIPMENTS_EDIT);
  }

  public void canManageSystemSettings() {
    checkPermission(SYSTEM_SETTINGS_MANAGE, null);
  }

  /**
   * Checks if user has permission to view order.
   */
  public void canViewOrder(Order order) {
    UUID requestingFacilityId = order.getRequestingFacilityId();
    UUID supplyingFacilityId = order.getSupplyingFacilityId();
    UUID programId = order.getProgramId();

    if (hasPermission(ORDERS_VIEW, supplyingFacilityId)
        || hasPermission(ORDERS_EDIT, supplyingFacilityId)) {
      return;
    }

    if (hasPermission(SHIPMENTS_VIEW, supplyingFacilityId)
        || hasPermission(SHIPMENTS_EDIT, supplyingFacilityId)) {
      return;
    }

    if (hasPermission(PODS_VIEW, requestingFacilityId, programId)
        || hasPermission(PODS_MANAGE, requestingFacilityId, programId)) {
      return;
    }

    throw new MissingPermissionException(
        ORDERS_VIEW, ORDERS_EDIT, SHIPMENTS_EDIT, SHIPMENTS_VIEW, PODS_VIEW, PODS_MANAGE
    );
  }

  public void canEditOrder(Order order) {
    checkPermission(ORDERS_EDIT, order.getSupplyingFacilityId());
  }

  public void canEditOrder(OrderDto order) {
    checkPermission(ORDERS_EDIT, order.getSupplyingFacility().getId());
  }

  /**
   * Checks if user has permission to view Shipments.
   *
   * @param shipment a shipment
   */
  public void canViewShipment(@NotNull Shipment shipment) {
    canViewShipment(shipment.getOrder());
  }

  /**
   * Checks if user has permission to view Shipments.
   *
   * @param order an order associated with shipment
   */
  public void canViewShipment(@NotNull Order order) {
    if (hasPermission(SHIPMENTS_VIEW, order.getSupplyingFacilityId())
        || hasPermission(SHIPMENTS_EDIT, order.getSupplyingFacilityId())) {
      return;
    }

    throw new MissingPermissionException(SHIPMENTS_VIEW, SHIPMENTS_EDIT);
  }

  /**
   * Checks if user has permission to view Shipments.
   *
   * @param shipmentDraft a shipment draft
   */
  public void canViewShipmentDraft(@NotNull ShipmentDraft shipmentDraft) {
    canViewShipmentDraft(shipmentDraft.getOrder());
  }

  /**
   * Checks if user has permission to view Shipments.
   *
   * @param order an order associated with draft
   */
  public void canViewShipmentDraft(@NotNull Order order) {
    if (hasPermission(SHIPMENTS_VIEW, order.getSupplyingFacilityId())
        || hasPermission(SHIPMENTS_EDIT, order.getSupplyingFacilityId())) {
      return;
    }

    throw new MissingPermissionException(SHIPMENTS_VIEW, SHIPMENTS_EDIT);
  }

  /**
   * Checks if user has permission to edit Shipments.
   *
   * @param shipmentDto a shipment dto
   */
  public void canEditShipment(@NotNull ShipmentDto shipmentDto) {
    checkShipmentEditWithOrder(shipmentDto.getOrder());
  }

  /**
   * Checks if user has permission to edit Shipments.
   *
   * @param shipmentDto a shipment dto
   */
  public void canEditShipmentDraft(@NotNull ShipmentDraftDto shipmentDto) {
    checkShipmentEditWithOrder(shipmentDto.getOrder());
  }

  /**
   * Checks if user has permission to edit Shipments.
   *
   * @param shipmentDraft a shipment dto
   */
  public void canEditShipmentDraft(@NotNull ShipmentDraft shipmentDraft) {
    checkPermission(SHIPMENTS_EDIT, shipmentDraft.getOrder().getSupplyingFacilityId());
  }

  public PermissionStrings.Handler getPermissionStrings(UUID userId) {
    return permissionStrings.forUser(userId);
  }

  private void checkShipmentEditWithOrder(ObjectReferenceDto orderDto) {
    Order order = orderRepository.findOne(orderDto.getId());

    if (null == order) {
      throw new ValidationException(ORDER_NOT_FOUND, orderDto.getId().toString());
    }

    checkPermission(SHIPMENTS_EDIT, order.getSupplyingFacilityId());
  }

  private void checkPermission(String rightName, UUID facility, UUID program) {
    checkPermission(rightName, facility, program, null, true, false);
  }

  private void checkPermission(String rightName, UUID warehouse) {
    checkPermission(rightName, null, null, warehouse, true, false);
  }

  private void checkPermission(String rightName, UUID facility, UUID program, UUID warehouse,
                               boolean allowUserTokens, boolean allowApiKey) {
    if (hasPermission(rightName, facility, program, warehouse, allowUserTokens, allowApiKey)) {
      return;
    }

    throw new MissingPermissionException(rightName);
  }

  private boolean hasPermission(String rightName, UUID facility, UUID program) {
    return hasPermission(rightName, facility, program, null, true, false);
  }

  private boolean hasPermission(String rightName, UUID warehouse) {
    return hasPermission(rightName, null, null, warehouse, true, false);
  }

  private boolean hasPermission(String rightName, UUID facility, UUID program, UUID warehouse,
                                boolean allowUserTokens, boolean allowApiKey) {
    OAuth2Authentication authentication = (OAuth2Authentication) SecurityContextHolder
        .getContext()
        .getAuthentication();

    return authentication.isClientOnly()
        ? checkServiceToken(allowApiKey, authentication)
        : checkUserToken(rightName, facility, program, warehouse, allowUserTokens);
  }

  private boolean checkUserToken(String rightName, UUID facility, UUID program, UUID warehouse,
                                 boolean allowUserTokens) {
    if (!allowUserTokens) {
      return false;
    }

    UserDto user = authenticationHelper.getCurrentUser();
    RightDto right = authenticationHelper.getRight(rightName);
    ResultDto<Boolean> result =  userReferenceDataService.hasRight(
        user.getId(), right.getId(), program, facility, warehouse
    );

    return null != result && isTrue(result.getResult());
  }

  private boolean checkServiceToken(boolean allowApiKey,
                                    OAuth2Authentication authentication) {
    String clientId = authentication.getOAuth2Request().getClientId();

    if (serviceTokenClientId.equals(clientId)) {
      return true;
    }

    if (startsWith(clientId, apiKeyPrefix)) {
      return allowApiKey;
    }

    return false;
  }

}
