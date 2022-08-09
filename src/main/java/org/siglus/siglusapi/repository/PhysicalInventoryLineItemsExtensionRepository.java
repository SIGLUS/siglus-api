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

package org.siglus.siglusapi.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.siglus.siglusapi.domain.PhysicalInventoryLineItemsExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface PhysicalInventoryLineItemsExtensionRepository extends
    JpaRepository<PhysicalInventoryLineItemsExtension, UUID> {

  List<PhysicalInventoryLineItemsExtension> findByPhysicalInventoryIdIn(Collection<UUID> ids);

  List<PhysicalInventoryLineItemsExtension> findByPhysicalInventoryId(UUID id);

  Optional<PhysicalInventoryLineItemsExtension> findFirstBySubDraftId(UUID subDraftId);



  @Transactional
  void deleteByPhysicalInventoryIdIn(Collection<UUID> ids);

  List<PhysicalInventoryLineItemsExtension> findBySubDraftIdIn(List<UUID> subDraftUuids);
}
