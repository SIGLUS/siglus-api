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

package org.openlmis.requisition.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionDataBuilder;
import org.openlmis.requisition.errorhandling.ValidationResult;

public class RequisitionSecurityServiceTest {

  @Mock
  private PermissionService permissionService;

  @InjectMocks
  private RequisitionSecurityService requisitionSecurityService = new RequisitionSecurityService();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void shouldUseCachedRightIfOneExists() {
    final UUID facility = UUID.randomUUID();
    final UUID program = UUID.randomUUID();
    final Requisition requisition = buildRequisition(facility, program);

    when(permissionService.canViewRequisition(any(Requisition.class)))
        .thenReturn(ValidationResult.success());

    List<Requisition> allRequisitions = Lists.newArrayList(requisition, requisition, requisition);

    requisitionSecurityService.filterInaccessibleRequisitions(allRequisitions);

    // The permission service should be called only one time, despite having 3 requisitions, due
    // to caching
    verify(permissionService, times(1)).canViewRequisition(any(Requisition.class));
  }

  @Test
  public void shouldNotUseCachedRightIfAllCallsAreDifferent() {
    final Requisition requisition = buildRequisition(UUID.randomUUID(), UUID.randomUUID());
    final Requisition requisition2 = buildRequisition(UUID.randomUUID(), UUID.randomUUID());
    final Requisition requisition3 = buildRequisition(UUID.randomUUID(), UUID.randomUUID());

    List<Requisition> allRequisitions = Lists.newArrayList(requisition, requisition2, requisition3);

    when(permissionService.canViewRequisition(any(Requisition.class)))
        .thenReturn(ValidationResult.success());

    requisitionSecurityService.filterInaccessibleRequisitions(allRequisitions);

    // The permission service should be called one time for each requisition (no cache hits)
    verify(permissionService, times(3)).canViewRequisition(any(Requisition.class));
  }

  @Test
  public void shouldProperlyFilterAccessibleRequisitions() {
    final Requisition requisition = buildRequisition(UUID.randomUUID(), UUID.randomUUID());
    final Requisition requisition2 = buildRequisition(UUID.randomUUID(), UUID.randomUUID());
    final Requisition requisition3 = buildRequisition(UUID.randomUUID(), UUID.randomUUID());
    final Requisition requisition4 = buildRequisition(UUID.randomUUID(), UUID.randomUUID());

    when(permissionService.canViewRequisition(any(Requisition.class)))
        .thenReturn(ValidationResult.success(),
            ValidationResult.noPermission("accessDenied"),
            ValidationResult.noPermission("accessDenied"),
            ValidationResult.success());

    List<Requisition> allRequisitions = Lists.newArrayList(requisition, requisition2,
        requisition3, requisition4);
    List<Requisition> result = requisitionSecurityService
        .filterInaccessibleRequisitions(allRequisitions);

    assertEquals(2, result.size());
    assertEquals(requisition, result.get(0));
    assertEquals(requisition4, result.get(1));
  }

  private Requisition buildRequisition(UUID programId, UUID facilityId) {
    return new RequisitionDataBuilder()
        .withProgramId(programId)
        .withFacilityId(facilityId)
        .build();
  }
}
