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

package org.openlmis.referencedata.service;

import org.openlmis.referencedata.domain.GeographicLevel;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.dto.GeographicZoneDto;
import org.openlmis.referencedata.repository.GeographicLevelRepository;
import org.openlmis.referencedata.repository.GeographicZoneRepository;
import org.openlmis.referencedata.util.messagekeys.GeographicLevelMessageKeys;
import org.openlmis.referencedata.util.messagekeys.GeographicZoneMessageKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GeographicZoneBuilder
    implements DomainResourceBuilder<GeographicZoneDto, GeographicZone> {

  @Autowired
  private GeographicLevelRepository geographicLevelRepository;

  @Autowired
  private GeographicZoneRepository geographicZoneRepository;

  /**
   * Creates new {@link GeographicZone} based on data from importer.
   */
  public GeographicZone build(GeographicZoneDto importer) {
    final GeographicLevel level = findResource(
        geographicLevelRepository::findOne, importer.getLevel(),
        GeographicLevelMessageKeys.ERROR_NOT_FOUND);
    final GeographicZone parent = null == importer.getParent()
        ? null
        : findResource(
            geographicZoneRepository::findOne, importer.getParent(),
            GeographicZoneMessageKeys.ERROR_NOT_FOUND);

    GeographicZone zone;

    if (null == importer.getId()) {
      zone = new GeographicZone();
    } else {
      zone = geographicZoneRepository.findOne(importer.getId());

      if (null == zone) {
        zone = new GeographicZone();
        zone.setId(importer.getId());
      }
    }

    zone.updateFrom(importer);
    zone.setLevel(level);
    zone.setParent(parent);

    return zone;
  }
}
