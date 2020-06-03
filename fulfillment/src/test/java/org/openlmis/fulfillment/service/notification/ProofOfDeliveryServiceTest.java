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

package org.openlmis.fulfillment.service.notification;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.javers.common.collections.Sets.asSet;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.openlmis.fulfillment.service.PermissionService.PODS_MANAGE;
import static org.openlmis.fulfillment.service.PermissionService.SHIPMENTS_EDIT;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.ProofOfDeliveryDataBuilder;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.repository.ProofOfDeliveryRepository;
import org.openlmis.fulfillment.service.PermissionService;
import org.openlmis.fulfillment.service.ProofOfDeliveryService;
import org.openlmis.fulfillment.service.referencedata.PermissionStringDto;
import org.openlmis.fulfillment.service.referencedata.PermissionStrings;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.testutils.UserDataBuilder;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RunWith(MockitoJUnitRunner.class)
public class ProofOfDeliveryServiceTest {

  @Mock
  private AuthenticationHelper authenticationHelper;

  @Mock
  private PermissionService permissionService;

  @Mock
  private ProofOfDeliveryRepository proofOfDeliveryRepository;

  @Mock
  private PermissionStrings.Handler permissionStringsHandler;

  @InjectMocks
  private ProofOfDeliveryService proofOfDeliveryService;

  private ProofOfDelivery proofOfDelivery = new ProofOfDeliveryDataBuilder().build();
  private Pageable pageable = new PageRequest(0, 10);
  private UUID shipmentId = UUID.randomUUID();
  private UUID orderId = UUID.randomUUID();
  private UserDto userDto = new UserDataBuilder().build();

  @Before
  public void setUp() {
    when(permissionService.getPermissionStrings(userDto.getId()))
        .thenReturn(permissionStringsHandler);
  }

  @Test
  public void shouldSearchAllProofsOfDeliveryForServiceRequest() {
    when(authenticationHelper.getCurrentUser())
        .thenReturn(null);
    when(proofOfDeliveryRepository.search(
        eq(null), eq(null), eq(emptySet()), eq(emptySet()), eq(emptySet()), eq(pageable)))
        .thenReturn(Pagination.getPage(singletonList(proofOfDelivery), pageable, 1));

    Page<ProofOfDelivery> result = proofOfDeliveryService.search(null, null, pageable);

    assertThat(result.getContent(), hasItems(proofOfDelivery));
  }

  @Test
  public void shouldSearchProofsOfDeliveryByShipmentIdForServiceRequest() {
    when(authenticationHelper.getCurrentUser())
        .thenReturn(null);
    when(proofOfDeliveryRepository.search(
        eq(shipmentId),  eq(null), eq(emptySet()), eq(emptySet()), eq(emptySet()), eq(pageable)))
        .thenReturn(Pagination.getPage(singletonList(proofOfDelivery), pageable, 1));

    Page<ProofOfDelivery> result = proofOfDeliveryService.search(shipmentId, null, pageable);

    assertThat(result.getContent(), hasItems(proofOfDelivery));
  }

  @Test
  public void shouldSearchProofsOfDeliveryByOrderIdForServiceRequest() {
    when(authenticationHelper.getCurrentUser())
        .thenReturn(null);
    when(proofOfDeliveryRepository.search(
        eq(null),  eq(orderId), eq(emptySet()), eq(emptySet()), eq(emptySet()), eq(pageable)))
        .thenReturn(Pagination.getPage(singletonList(proofOfDelivery), pageable, 1));

    Page<ProofOfDelivery> result = proofOfDeliveryService.search(null, orderId, pageable);

    assertThat(result.getContent(), hasItems(proofOfDelivery));
  }

  @Test
  public void shouldReturnEmptyListWhenUserHasNoPermissions() {
    when(authenticationHelper.getCurrentUser())
        .thenReturn(userDto);
    when(permissionStringsHandler.get())
        .thenReturn(emptySet());

    Page<ProofOfDelivery> result = proofOfDeliveryService.search(null, null, pageable);

    assertEquals(0, result.getContent().size());
    verifyNoMoreInteractions(proofOfDeliveryRepository);
  }

  @Test
  public void shouldSearchProofsOfDeliveryUsingUserPermissions() {
    when(authenticationHelper.getCurrentUser())
        .thenReturn(userDto);
    when(permissionStringsHandler.get())
        .thenReturn(asSet(
            PermissionStringDto.create(PODS_MANAGE, proofOfDelivery.getReceivingFacilityId(),
                proofOfDelivery.getProgramId()),
            PermissionStringDto.create(SHIPMENTS_EDIT, proofOfDelivery.getSupplyingFacilityId(),
                proofOfDelivery.getProgramId())));
    when(proofOfDeliveryRepository.search(
        eq(null),
        eq(null),
        eq(singleton(proofOfDelivery.getReceivingFacilityId())),
        eq(singleton(proofOfDelivery.getSupplyingFacilityId())),
        eq(singleton(proofOfDelivery.getProgramId())),
        eq(pageable)))
        .thenReturn(Pagination.getPage(singletonList(proofOfDelivery), pageable, 1));

    Page<ProofOfDelivery> result = proofOfDeliveryService.search(null, null, pageable);

    assertThat(result.getContent(), hasItems(proofOfDelivery));
  }

  @Test
  public void shouldSearchProofsOfDeliveryByAllParamsAndUserPermissions() {
    when(authenticationHelper.getCurrentUser())
        .thenReturn(userDto);
    when(permissionStringsHandler.get())
        .thenReturn(asSet(
            PermissionStringDto.create(PODS_MANAGE, proofOfDelivery.getReceivingFacilityId(),
                proofOfDelivery.getProgramId()),
            PermissionStringDto.create(SHIPMENTS_EDIT, proofOfDelivery.getSupplyingFacilityId(),
                proofOfDelivery.getProgramId())));
    when(proofOfDeliveryRepository.search(
        eq(orderId),
        eq(shipmentId),
        eq(singleton(proofOfDelivery.getReceivingFacilityId())),
        eq(singleton(proofOfDelivery.getSupplyingFacilityId())),
        eq(singleton(proofOfDelivery.getProgramId())),
        eq(pageable)))
        .thenReturn(Pagination.getPage(singletonList(proofOfDelivery), pageable, 1));

    Page<ProofOfDelivery> result = proofOfDeliveryService.search(orderId, shipmentId, pageable);

    assertThat(result.getContent(), hasItems(proofOfDelivery));
  }

  @Test
  public void shouldSearchWithEmptyListOfProgramIdsWhenAtLeastOnePermissionHasNoProgramId() {
    PermissionStringDto permissionWithoutProgram = PermissionStringDto
        .from(SHIPMENTS_EDIT + "|" + proofOfDelivery.getSupplyingFacilityId().toString());

    when(authenticationHelper.getCurrentUser())
        .thenReturn(userDto);
    when(permissionStringsHandler.get())
        .thenReturn(asSet(
            PermissionStringDto.create(PODS_MANAGE, proofOfDelivery.getReceivingFacilityId(),
                proofOfDelivery.getProgramId()),
            permissionWithoutProgram));
    when(proofOfDeliveryRepository.search(
        eq(orderId),
        eq(shipmentId),
        eq(singleton(proofOfDelivery.getReceivingFacilityId())),
        eq(singleton(proofOfDelivery.getSupplyingFacilityId())),
        eq(emptySet()),
        eq(pageable)))
        .thenReturn(Pagination.getPage(singletonList(proofOfDelivery), pageable, 1));

    Page<ProofOfDelivery> result = proofOfDeliveryService.search(orderId, shipmentId, pageable);

    assertThat(result.getContent(), hasItems(proofOfDelivery));
  }
}
