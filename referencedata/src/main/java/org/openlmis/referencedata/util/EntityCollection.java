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

package org.openlmis.referencedata.util;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.openlmis.referencedata.domain.BaseEntity;

public class EntityCollection<T extends BaseEntity> {

  private Map<UUID, T> collection = Maps.newHashMap();

  /**
   * Builds instance of {@link EntityCollection} with collection passed in parameter.
   */
  public EntityCollection(Iterable<T> entities) {
    if (entities != null) {
      entities.forEach(e -> collection.put(e.getId(), e));
    }
  }

  /**
   * Gets an entity from collection based on id. Returns null if entity is not found.
   * @param id the id of getting entry.
   * @return the found entity.
   */
  public T getById(UUID id) {
    return collection.get(id);
  }

  /**
   * Gets all values.
   * @return all entities.
   */
  public Collection<T> values() {
    return collection.values();
  }
}
