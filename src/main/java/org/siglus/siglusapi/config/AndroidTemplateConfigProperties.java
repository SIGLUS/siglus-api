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

package org.siglus.siglusapi.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AndroidTemplateConfigProperties {

  private UUID androidViaTemplateId;

  private UUID androidMmiaTemplateId;

  private UUID androidMalariaTemplateId;

  private UUID androidRapidtestTemplateId;

  private Map<String, UUID> programIdByCode = new HashMap<>();

  private Set<UUID> androidTemplateIds = new HashSet<>();

  public AndroidTemplateConfigProperties(UUID androidViaTemplateId, UUID androidMmiaTemplateId,
      UUID androidMalariaTemplateId, UUID androidRapidtestTemplateId) {
    this.androidViaTemplateId = androidViaTemplateId;
    this.androidMmiaTemplateId = androidMmiaTemplateId;
    this.androidMalariaTemplateId = androidMalariaTemplateId;
    this.androidRapidtestTemplateId = androidRapidtestTemplateId;
    programIdByCode.put("VC", androidViaTemplateId);
    programIdByCode.put("T", androidMmiaTemplateId);
    programIdByCode.put("ML", androidMalariaTemplateId);
    programIdByCode.put("TR", androidRapidtestTemplateId);
    androidTemplateIds.add(androidViaTemplateId);
    androidTemplateIds.add(androidMmiaTemplateId);
    androidTemplateIds.add(androidMalariaTemplateId);
    androidTemplateIds.add(androidRapidtestTemplateId);
  }

  public UUID findAndroidTemplateId(String programCode) {
    return programIdByCode.get(programCode);
  }
}
