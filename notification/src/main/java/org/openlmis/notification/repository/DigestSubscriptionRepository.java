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

package org.openlmis.notification.repository;

import java.util.List;
import java.util.UUID;
import org.openlmis.notification.domain.DigestConfiguration;
import org.openlmis.notification.domain.DigestSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface DigestSubscriptionRepository extends JpaRepository<DigestSubscription, UUID> {

  @Query("SELECT DISTINCT s"
      + " FROM DigestSubscription AS s"
      + " INNER JOIN FETCH s.userContactDetails AS u"
      + " INNER JOIN FETCH s.digestConfiguration AS c"
      + " WHERE u.referenceDataUserId = :userId")
  List<DigestSubscription> getUserSubscriptions(@Param("userId") UUID userId);

  @Query("SELECT CASE WHEN count(s) = 1 THEN TRUE ELSE FALSE END"
      + " FROM DigestSubscription AS s"
      + " WHERE s.userContactDetails.referenceDataUserId = :userId"
      + " AND s.digestConfiguration = :configuration")
  boolean existsBy(@Param("userId") UUID userId,
      @Param("configuration") DigestConfiguration configuration);

  @Query("SELECT s"
      + " FROM DigestSubscription AS s"
      + " WHERE s.userContactDetails.referenceDataUserId = :userId"
      + " AND s.digestConfiguration = :configuration")
  DigestSubscription findBy(@Param("userId") UUID userId,
      @Param("configuration") DigestConfiguration configuration);

  @Query("DELETE FROM DigestSubscription AS s"
      + " WHERE s.userContactDetails.referenceDataUserId = :userId")
  @Modifying(clearAutomatically = true)
  void deleteUserSubscriptions(@Param("userId") UUID userId);

}
