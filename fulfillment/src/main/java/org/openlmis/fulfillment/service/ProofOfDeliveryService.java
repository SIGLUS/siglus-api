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

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.openlmis.fulfillment.service.PermissionService.PODS_MANAGE;
import static org.openlmis.fulfillment.service.PermissionService.PODS_VIEW;
import static org.openlmis.fulfillment.service.PermissionService.SHIPMENTS_EDIT;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.repository.ProofOfDeliveryRepository;
import org.openlmis.fulfillment.service.referencedata.PermissionStringDto;
import org.openlmis.fulfillment.service.referencedata.PermissionStrings;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.util.Pagination;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProofOfDeliveryService {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(ProofOfDeliveryService.class);

  @Autowired
  private AuthenticationHelper authenticationHelper;

  @Autowired
  private PermissionService permissionService;

  @Autowired
  private ProofOfDeliveryRepository proofOfDeliveryRepository;

  /**
   * Searches for PODs using shipment and order ids (both are optional).
   * If request was made by user then POD will be returned if user has any following permissions:
   * SHIPMENT_EDIT right with program and supplying facility from given POD,
   * POD_EDIT right with program and requesting facility from given POD,
   * POD_VIEW right with program and requesting facility from given POD,
   * If it was cross-service request then all PODs that met parameter criteria will be returned,
   * regardless of permissions.
   *
   * @param shipmentId UUID of shipment associated with POD, optional
   * @param orderId    UUID of order associated with POD, optional
   * @param pageable   pagination parameters
   * @return page of filtered PODs.
   */
  public Page<ProofOfDelivery> search(UUID shipmentId, UUID orderId, Pageable pageable) {
    Profiler profiler = new Profiler("SEARCH_PODS");
    profiler.setLogger(XLOGGER);

    UserDto user = authenticationHelper.getCurrentUser();
    Set<UUID> programIds = new HashSet<>();
    Set<UUID> receivingFacilitiesIds = new HashSet<>();
    Set<UUID> supplyingFacilitiesIds = new HashSet<>();

    if (null != user) {
      profiler.start("GET_PERMISSION_STRINGS");
      PermissionStrings.Handler handler = permissionService.getPermissionStrings(user.getId());
      Set<PermissionStringDto> permissionStrings = handler.get();

      profiler.start("RETRIEVE_LIST_OF_IDS_FROM_PERMISSION_STRINGS");
      populateIdsFromPermissionStrings(
          permissionStrings, programIds, receivingFacilitiesIds, supplyingFacilitiesIds);

      if (isEmpty(programIds)
          && isEmpty(receivingFacilitiesIds)
          && isEmpty(supplyingFacilitiesIds)) {
        return Pagination.getPage(emptyList(), pageable, 0);
      }
    }
    profiler.start("FIND_PODS");
    Page<ProofOfDelivery> result = proofOfDeliveryRepository.search(shipmentId, orderId,
        receivingFacilitiesIds, supplyingFacilitiesIds, programIds, pageable);

    profiler.stop().log();
    return result;
  }

  private void populateIdsFromPermissionStrings(Set<PermissionStringDto> permissionStrings,
      Set<UUID> programIds, Set<UUID> receivingFacilitiesIds, Set<UUID> supplyingFacilitiesIds) {
    boolean hasPermissionForAllPrograms = permissionStrings.stream()
        .filter(this::isPodSearchRight)
        .anyMatch(permissionString -> permissionString.getProgramId() == null);

    permissionStrings.stream()
        .filter(this::isPodSearchRight)
        .forEach(permissionString -> {
          if (!hasPermissionForAllPrograms) {
            programIds.add(permissionString.getProgramId());
          }
          if (PODS_MANAGE.equals(permissionString.getRightName())
              || PODS_VIEW.equals(permissionString.getRightName())) {
            receivingFacilitiesIds.add(permissionString.getFacilityId());
          }
          if (SHIPMENTS_EDIT.equals(permissionString.getRightName())) {
            supplyingFacilitiesIds.add(permissionString.getFacilityId());
          }
        });
  }

  private boolean isPodSearchRight(PermissionStringDto permissionString) {
    return PODS_MANAGE.equals(permissionString.getRightName())
        || PODS_VIEW.equals(permissionString.getRightName())
        || SHIPMENTS_EDIT.equals(permissionString.getRightName());
  }
}
