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

package org.siglus.siglusapi.localmachine;

import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.MappedSuperclass;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import org.apache.commons.lang3.NotImplementedException;
import org.siglus.siglusapi.localmachine.EventQueue.MetaInfo;
import org.springframework.stereotype.Repository;

@Repository
public class ShadowFacilityRepository {

  public ShadowFacility getCurrentShadowFacility() {
    // fixme
    throw new NotImplementedException("todo");
  }

  public void save(ShadowFacility facility) {
    throw new NotImplementedException("todo");
  }

  @MappedSuperclass
  @SqlResultSetMappings({
    @SqlResultSetMapping(
        name = "",
        classes =
            @ConstructorResult(
                targetClass = MetaInfo.class,
                columns = @ColumnResult(name = "owner_id")))
  })
  interface MetaInfoRepository {}
}
