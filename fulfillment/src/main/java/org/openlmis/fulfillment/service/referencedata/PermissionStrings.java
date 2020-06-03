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

package org.openlmis.fulfillment.service.referencedata;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.openlmis.fulfillment.service.ServiceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PermissionStrings {
  private final Map<UUID, Handler> handlers = Maps.newConcurrentMap();

  @Autowired
  private UserReferenceDataService userReferenceDataService;

  public Handler forUser(UUID user) {
    return handlers.computeIfAbsent(user, Handler::new);
  }

  public class Handler implements Supplier<Set<PermissionStringDto>> {
    private final Object lock = new Object();

    private UUID userId;

    private Set<PermissionStringDto> permissionStrings;
    private String etag;

    private Map<String, Set<UUID>> facilityIds;

    Handler(UUID userId) {
      this.userId = userId;
    }

    @Override
    public Set<PermissionStringDto> get() {
      updatePermissionStrings();
      return permissionStrings;
    }

    /**
     * Get facility IDs for the given rights from permission strings.
     *
     * @param rightNames a list of right names.
     * @return Set with all related facility IDs. If there is no permission strings for the given
     *         rights, an empty set will be returned.
     */
    public Set<UUID> getFacilityIds(String... rightNames) {
      updatePermissionStrings();
      return Arrays
          .stream(rightNames)
          .map(facilityIds::get)
          .filter(Objects::nonNull)
          .flatMap(Collection::stream)
          .collect(toSet());
    }

    private void updatePermissionStrings() {
      synchronized (lock) {
        ServiceResponse<List<String>> response = userReferenceDataService
            .getPermissionStrings(userId, etag);

        if (response.isModified()) {
          permissionStrings = PermissionStringDto.from(response.getBody());
          etag = response.getETag();
          facilityIds = permissionStrings
              .stream()
              .filter(Objects::nonNull)
              .filter(elem -> Objects.nonNull(elem.getFacilityId()))
              .collect(groupingBy(
                  PermissionStringDto::getRightName,
                  mapping(PermissionStringDto::getFacilityId, toSet())
              ));
        }
      }
    }
  }
}
