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

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.Setter;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class LocationSynchronizer<T extends IBaseResource, B extends IBaseBundle>
    implements Synchronizer<FhirLocation, T> {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Setter
  private IGenericClient client;

  @Setter
  private CacheControlDirective cacheControlDirective;

  @Setter
  private CriterionBuilder criterionBuilder;

  private final Class<T> resourceClass;
  private final Class<B> bundleClass;
  private final boolean removeVersion;

  LocationSynchronizer(Class<T> resourceClass, Class<B> bundleClass, boolean removeVersion) {
    this.resourceClass = resourceClass;
    this.bundleClass = bundleClass;
    this.removeVersion = removeVersion;
  }

  @Override
  public void synchronize(FhirLocation olmisLocation, T fhirLocation) {
    logger.debug("Try to find resources by criterion");
    B bundle = client
        .search()
        .forResource(resourceClass)
        .cacheControl(cacheControlDirective)
        .where(criterionBuilder.buildIdentifierCriterion(olmisLocation.getId()))
        .returnBundle(bundleClass)
        .execute();
    T existing = getEntry(bundle);

    if (null == existing) {
      createLocation(client, fhirLocation);
    } else {
      updateLocation(client, existing, fhirLocation);
    }
  }

  abstract T getEntry(B bundle);

  private void createLocation(IGenericClient client, T fhirLocation) {
    fhirLocation.setId((IIdType) null);
    client
        .create()
        .resource(fhirLocation)
        .prettyPrint()
        .encodedJson()
        .execute();
  }

  private void updateLocation(IGenericClient client, T existing, T fhirLocation) {
    IIdType idType = existing.getIdElement();

    if (removeVersion) {
      idType = idType.withVersion(null);
    }

    fhirLocation.setId(idType);
    client
        .update()
        .resource(fhirLocation)
        .encodedJson()
        .execute();
  }

}
