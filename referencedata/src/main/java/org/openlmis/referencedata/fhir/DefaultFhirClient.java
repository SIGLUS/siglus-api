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

package org.openlmis.referencedata.fhir;

import static org.apache.commons.lang3.StringUtils.startsWith;

import lombok.Setter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.GeographicZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

@Setter
class DefaultFhirClient implements FhirClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFhirClient.class);

  private static final String SKIPPING_SYNC_PROCESS_MSG =
      "Request came from FHIR service. Skipping synchronization process.";

  private LocationFactory locationFactory;
  private LocationConverter locationConvert;
  private LocationSynchronizer locationSynchronizer;

  private String apiKeyPrefix;

  @Override
  public void synchronizeFacility(Facility facility) {
    LOGGER.info("Synchronizing facility with id: {}", facility.getId());
    if (shouldIgnore()) {
      LOGGER.info(SKIPPING_SYNC_PROCESS_MSG);
      return;

    }
    synchronize(locationFactory.createFor(facility));
    LOGGER.info("Synchronized facility with id: {}", facility.getId());
  }

  @Override
  public void synchronizeGeographicZone(GeographicZone geographicZone) {
    LOGGER.info("Synchronizing geographic zone with id: {}", geographicZone.getId());
    if (shouldIgnore()) {
      LOGGER.info(SKIPPING_SYNC_PROCESS_MSG);
      return;
    }

    synchronize(locationFactory.createFor(geographicZone));
    LOGGER.info("Synchronized geographic zone with id: {}", geographicZone.getId());
  }

  private boolean shouldIgnore() {
    Authentication authentication = SecurityContextHolder
        .getContext()
        .getAuthentication();

    if (authentication instanceof OAuth2Authentication) {
      OAuth2Authentication auth2Authentication = (OAuth2Authentication) authentication;
      String clientId = auth2Authentication.getOAuth2Request().getClientId();

      return auth2Authentication.isClientOnly() && !startsWith(clientId, apiKeyPrefix);
    }

    return true;
  }

  private void synchronize(FhirLocation location) {
    IBaseResource resource = locationConvert.convert(location);
    locationSynchronizer.synchronize(location, resource);
  }

}
