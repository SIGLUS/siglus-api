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

import java.util.UUID;
import org.openlmis.notification.domain.UserContactDetails;
import org.openlmis.notification.repository.custom.UserContactDetailsRepositoryCustom;
import org.openlmis.notification.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserContactDetailsRepository
    extends JpaRepository<UserContactDetails, UUID>,
    UserContactDetailsRepositoryCustom {

  /**
   * Tries to find the first {@link UserContactDetails} with the given email address. If there is
   * no such row, the null value will be returned.
   */
  default UserContactDetails findOneByEmailAddress(String email) {
    Pageable pageable = new PageRequest(Pagination.DEFAULT_PAGE_NUMBER, Pagination.NO_PAGINATION);
    Page<UserContactDetails> page = search(email, null, pageable);

    return page.hasContent() ? page.getContent().get(0) : null;
  }

}
