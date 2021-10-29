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

import java.util.UUID;
import org.siglus.siglusapi.domain.Notification;
import org.siglus.siglusapi.domain.NotificationStatus;
import org.siglus.siglusapi.domain.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationRepository extends JpaRepository<Notification, UUID>,
    JpaSpecificationExecutor<Notification> {

  default Page<Notification> findViewable(Pageable pageable, NotificationType type,
      Specification<Notification> rightFilter) {
    return findAll(
        (root, query, cb) -> cb.and(
            cb.equal(root.get("type"), type),
            cb.equal(root.get("viewed"), false),
            cb.equal(root.get("processed"), false),
            rightFilter.toPredicate(root, query, cb)
        ),
        pageable);
  }

  default void updateLastNotificationProcessed(UUID refId, NotificationStatus... statuses) {
    Notification notification = findOne(
        (root, query, cb) -> cb.and(
            cb.equal(root.get("refId"), refId),
            root.<NotificationStatus>get("status").in((Object[]) statuses),
            cb.equal(root.get("processed"), false),
            cb.equal(root.get("type"), NotificationType.TODO)
        )
    );
    if (notification == null) {
      return;
    }
    notification.setProcessed(true);
    save(notification);
  }

}
