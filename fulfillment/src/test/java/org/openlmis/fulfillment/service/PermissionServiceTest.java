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

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.openlmis.fulfillment.i18n.MessageKeys.ORDER_NOT_FOUND;
import static org.openlmis.fulfillment.i18n.MessageKeys.PERMISSIONS_MISSING;
import static org.openlmis.fulfillment.i18n.MessageKeys.PERMISSION_MISSING;
import static org.openlmis.fulfillment.service.PermissionService.ORDERS_EDIT;
import static org.openlmis.fulfillment.service.PermissionService.ORDERS_TRANSFER;
import static org.openlmis.fulfillment.service.PermissionService.ORDERS_VIEW;
import static org.openlmis.fulfillment.service.PermissionService.PODS_MANAGE;
import static org.openlmis.fulfillment.service.PermissionService.PODS_VIEW;
import static org.openlmis.fulfillment.service.PermissionService.SHIPMENTS_EDIT;
import static org.openlmis.fulfillment.service.PermissionService.SHIPMENTS_VIEW;
import static org.openlmis.fulfillment.service.PermissionService.SYSTEM_SETTINGS_MANAGE;
import static org.openlmis.fulfillment.testutils.OAuth2AuthenticationDataBuilder.API_KEY_PREFIX;
import static org.openlmis.fulfillment.testutils.OAuth2AuthenticationDataBuilder.SERVICE_CLIENT_ID;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.repository.ShipmentRepository;
import org.openlmis.fulfillment.service.referencedata.RightDto;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.service.referencedata.UserReferenceDataService;
import org.openlmis.fulfillment.testutils.DtoGenerator;
import org.openlmis.fulfillment.testutils.OAuth2AuthenticationDataBuilder;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.web.MissingPermissionException;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.shipment.ShipmentDtoDataBuilder;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class PermissionServiceTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Mock
  private UserReferenceDataService userReferenceDataService;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private ShipmentRepository shipmentRepository;

  @InjectMocks
  private PermissionService permissionService;

  @Mock
  private SecurityContext securityContext;

  private UserDto user = DtoGenerator.of(UserDto.class);
  private List<RightDto> rights = DtoGenerator.of(RightDto.class, 8);
  private Map<String, RightDto> rightsMap = ImmutableMap
      .<String, RightDto>builder()
      .put(ORDERS_TRANSFER, rights.get(0))
      .put(PODS_MANAGE, rights.get(1))
      .put(PODS_VIEW, rights.get(2))
      .put(ORDERS_VIEW, rights.get(3))
      .put(ORDERS_EDIT, rights.get(4))
      .put(SHIPMENTS_VIEW, rights.get(5))
      .put(SHIPMENTS_EDIT, rights.get(6))
      .put(SYSTEM_SETTINGS_MANAGE, rights.get(7))
      .build();

  private ProofOfDelivery pod = new ProofOfDeliveryDataBuilder().build();
  private Shipment shipment = pod.getShipment();
  private Order order = shipment.getOrder();

  private ShipmentDto shipmentDto = new ShipmentDtoDataBuilder()
      .withOrder(new OrderObjectReferenceDto(order.getId()))
      .build();

  private OAuth2Authentication trustedClient = new OAuth2AuthenticationDataBuilder()
      .buildServiceAuthentication();
  private OAuth2Authentication userClient = new OAuth2AuthenticationDataBuilder()
      .buildUserAuthentication();
  private OAuth2Authentication apiKeyClient = new OAuth2AuthenticationDataBuilder()
      .buildApiKeyAuthentication();

  @Before
  public void setUp() {
    SecurityContextHolder.setContext(securityContext);

    user.setId(order.getCreatedById());

    when(orderRepository.findOne(order.getId())).thenReturn(order);
    when(shipmentRepository.findOne(shipment.getId())).thenReturn(shipment);

    rightsMap.forEach((right, details) ->
        when(authenticationHelper.getRight(right)).thenReturn(details));

    when(authenticationHelper.getCurrentUser()).thenReturn(user);
    when(securityContext.getAuthentication()).thenReturn(userClient);

    ReflectionTestUtils.setField(permissionService, "serviceTokenClientId", SERVICE_CLIENT_ID);
    ReflectionTestUtils.setField(permissionService, "apiKeyPrefix", API_KEY_PREFIX);
  }

  @Test
  public void canTransferOrder() {
    mockHasRight(ORDERS_TRANSFER, null, null, order.getSupplyingFacilityId());

    permissionService.canTransferOrder(order);

    verifyRight(ORDERS_TRANSFER, null, null, order.getSupplyingFacilityId());
  }

  @Test
  public void cannotTransferOrder() {
    expectException(ORDERS_TRANSFER);

    permissionService.canTransferOrder(order);
  }

  @Test
  public void canManageSystemSettingsByServiceToken() {
    when(securityContext.getAuthentication()).thenReturn(trustedClient);

    permissionService.canManageSystemSettings();

    verifyZeroInteractions(authenticationHelper, userReferenceDataService);
  }

  @Test
  public void cannotManageSystemSettingsByApiKeyToken() {
    when(securityContext.getAuthentication()).thenReturn(apiKeyClient);
    expectException(SYSTEM_SETTINGS_MANAGE);

    //If endpoint does not allow for service level token authorization, method will throw Exception.
    permissionService.canManageSystemSettings();
  }

  @Test
  public void canManageSystemSettingsByUserToken() {
    mockHasRight(SYSTEM_SETTINGS_MANAGE, null, null, null);

    permissionService.canManageSystemSettings();

    verifyRight(SYSTEM_SETTINGS_MANAGE, null, null, null);
  }

  @Test
  public void cannotManageSystemSettingsByUserToken() {
    expectException(SYSTEM_SETTINGS_MANAGE);

    permissionService.canManageSystemSettings();
  }

  @Test
  public void canManagePod() {
    mockHasRight(PODS_MANAGE, pod.getReceivingFacilityId(), pod.getProgramId(), null);

    permissionService.canManagePod(pod);

    verifyRight(PODS_MANAGE, pod.getReceivingFacilityId(), pod.getProgramId(), null);
  }

  @Test
  public void cannotManagePod() {
    expectException(PODS_MANAGE);

    permissionService.canManagePod(pod);
  }

  @Test
  public void canViewPod() {
    mockHasRight(PODS_VIEW, pod.getReceivingFacilityId(), pod.getProgramId(), null);

    permissionService.canViewPod(pod);

    verifyRight(PODS_VIEW, pod.getReceivingFacilityId(), pod.getProgramId(), null);
  }

  @Test
  public void canViewPodWithPodManageRight() {
    mockHasRight(PODS_MANAGE, pod.getReceivingFacilityId(), pod.getProgramId(), null);

    permissionService.canViewPod(pod);

    verifyRight(PODS_MANAGE, pod.getReceivingFacilityId(), pod.getProgramId(), null);
  }

  @Test
  public void canViewPodWithShipmentEditRight() {
    mockHasRight(SHIPMENTS_EDIT, null, null, pod.getSupplyingFacilityId());

    permissionService.canViewPod(pod);

    verifyRight(SHIPMENTS_EDIT, null, null, pod.getSupplyingFacilityId());
  }

  @Test
  public void cannotViewPod() {
    expectException(PODS_MANAGE, PODS_VIEW, SHIPMENTS_EDIT);

    permissionService.canViewPod(pod);
  }

  @Test
  public void canViewOrder() {
    mockHasRight(ORDERS_VIEW, null, null, order.getSupplyingFacilityId());

    permissionService.canViewOrder(order);

    verifyRight(ORDERS_VIEW, null, null, order.getSupplyingFacilityId());
  }

  @Test
  public void canViewOrderWithOrderEditRight() {
    mockHasRight(ORDERS_EDIT, null, null, order.getSupplyingFacilityId());

    permissionService.canViewOrder(order);

    verifyRight(ORDERS_EDIT, null, null, order.getSupplyingFacilityId());
  }

  @Test
  public void canViewOrderWithShipmentEditRight() {
    mockHasRight(SHIPMENTS_EDIT, null, null, order.getSupplyingFacilityId());

    permissionService.canViewOrder(order);

    verifyRight(SHIPMENTS_EDIT, null, null, order.getSupplyingFacilityId());
  }

  @Test
  public void canViewOrderWithShipmentViewRight() {
    mockHasRight(SHIPMENTS_VIEW, null, null, order.getSupplyingFacilityId());

    permissionService.canViewOrder(order);

    verifyRight(SHIPMENTS_VIEW, null, null, order.getSupplyingFacilityId());
  }

  @Test
  public void canViewOrderWithPodViewRight() {
    mockHasRight(PODS_VIEW, order.getRequestingFacilityId(), order.getProgramId(), null);

    permissionService.canViewOrder(order);

    verifyRight(PODS_VIEW, order.getRequestingFacilityId(), order.getProgramId(), null);
  }

  @Test
  public void canViewOrderWithPodManageRight() {
    mockHasRight(PODS_MANAGE, order.getRequestingFacilityId(), order.getProgramId(), null);

    permissionService.canViewOrder(order);

    verifyRight(PODS_MANAGE, order.getRequestingFacilityId(), order.getProgramId(), null);
  }

  @Test
  public void cannotViewOrder() {
    expectException(
        ORDERS_VIEW, ORDERS_EDIT, SHIPMENTS_EDIT, SHIPMENTS_VIEW, PODS_VIEW, PODS_MANAGE
    );

    permissionService.canViewOrder(order);
  }

  @Test
  public void canEditOrder() {
    mockHasRight(ORDERS_EDIT, null, null, order.getSupplyingFacilityId());

    permissionService.canEditOrder(order);

    verifyRight(ORDERS_EDIT, null, null, order.getSupplyingFacilityId());
  }

  @Test
  public void cannotEditOrder() {
    expectException(ORDERS_EDIT);

    permissionService.canEditOrder(order);
  }

  @Test
  public void canManageShipment() {
    mockHasRight(SHIPMENTS_EDIT, null, null, order.getSupplyingFacilityId());

    permissionService.canEditShipment(shipmentDto);

    verifyRight(SHIPMENTS_EDIT, null, null, order.getSupplyingFacilityId());
  }

  @Test
  public void cannotManageShipmentWhenUserHasNoRights() {
    expectException(SHIPMENTS_EDIT);

    permissionService.canEditShipment(shipmentDto);
  }

  @Test
  public void cannotManageShipmentWhenOrderIsNotFound() {
    mockHasRight(ORDERS_EDIT, null, null, order.getSupplyingFacilityId());
    when(orderRepository.findOne(order.getId())).thenReturn(null);

    exception.expect(ValidationException.class);
    exception.expect(hasProperty("params", arrayContaining(order.getId().toString())));
    exception.expectMessage(ORDER_NOT_FOUND);

    permissionService.canEditShipment(shipmentDto);
  }

  @Test
  public void canViewShipment() {
    mockHasRight(SHIPMENTS_VIEW, null, null, order.getSupplyingFacilityId());

    permissionService.canViewShipment(shipment);

    verifyRight(SHIPMENTS_VIEW, null, null, order.getSupplyingFacilityId());
  }

  @Test
  public void canViewShipmentWithEditRight() {
    mockHasRight(SHIPMENTS_EDIT, null, null, order.getSupplyingFacilityId());

    permissionService.canViewShipment(shipment);

    verifyRight(SHIPMENTS_EDIT, null, null, order.getSupplyingFacilityId());
  }

  @Test
  public void cannotViewShipmentWhenUserHasNoRights() {
    expectException(SHIPMENTS_VIEW, SHIPMENTS_EDIT);

    permissionService.canViewShipment(shipment);
  }

  private void mockHasRight(String rightName, UUID facility, UUID program,
                            UUID warehouse) {
    when(userReferenceDataService
        .hasRight(user.getId(), rightsMap.get(rightName).getId(), program, facility, warehouse))
        .thenReturn(new ResultDto<>(true));
  }

  private void expectException(String rightName) {
    exception.expect(MissingPermissionException.class);
    exception.expect(hasProperty("params", arrayContaining(rightName)));
    exception.expectMessage(PERMISSION_MISSING);
  }

  private void expectException(String... rightNames) {
    exception.expect(MissingPermissionException.class);
    exception.expect(hasProperty("params", arrayContaining(String.join(", ", rightNames))));
    exception.expectMessage(PERMISSIONS_MISSING);
  }

  private void verifyRight(String rightName, UUID facility, UUID program,
                           UUID warehouse) {
    verify(authenticationHelper, atLeastOnce()).getCurrentUser();
    verify(authenticationHelper).getRight(rightName);
    verify(userReferenceDataService).hasRight(
        user.getId(), rightsMap.get(rightName).getId(), program, facility, warehouse
    );
  }

}

