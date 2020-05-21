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

package org.openlmis.referencedata.repository;

import java.util.List;
import java.util.UUID;
import org.openlmis.referencedata.domain.FacilityTypeApprovedProduct;
import org.openlmis.referencedata.domain.VersionIdentity;
import org.openlmis.referencedata.repository.custom.FacilityTypeApprovedProductRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacilityTypeApprovedProductRepository extends
    FacilityTypeApprovedProductRepositoryCustom,
    BaseAuditableRepository<FacilityTypeApprovedProduct, VersionIdentity> {

  List<FacilityTypeApprovedProduct> findByIdentityId(UUID id);

  FacilityTypeApprovedProduct findFirstByIdentityIdOrderByIdentityVersionNumberDesc(UUID id);

  FacilityTypeApprovedProduct findByIdentityIdAndIdentityVersionNumber(UUID id, Long versionNumber);

  @Query("UPDATE FacilityTypeApprovedProduct SET active = false WHERE identity.id = :id")
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  void deactivatePreviousVersions(@Param("id") UUID id);

  @Query(value = "SELECT"
      + "   ftap.*"
      + " FROM"
      + "   referencedata.facility_type_approved_products ftap"
      + " WHERE"
      + "   id NOT IN ("
      + "     SELECT"
      + "       id"
      + "     FROM"
      + "       referencedata.facility_type_approved_products ftap"
      + "       INNER JOIN referencedata.jv_global_id g"
      + "         ON (CAST(ftap.id AS varchar) = SUBSTRING("
      + "           CAST(CAST(g.local_id AS json)->'id' AS varchar), 2, 36))"
      + "         AND (CAST(ftap.versionNumber AS varchar) ="
      + "           CAST(CAST(g.local_id AS json)->'versionNumber' AS varchar))"
      + "       INNER JOIN referencedata.jv_snapshot s ON g.global_id_pk = s.global_id_fk)"
      + " ORDER BY ?#{#pageable}",
      nativeQuery = true)
  Page<FacilityTypeApprovedProduct> findAllWithoutSnapshots(Pageable pageable);
}
