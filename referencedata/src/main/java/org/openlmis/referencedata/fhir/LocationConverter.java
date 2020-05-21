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

import lombok.Setter;
import org.hl7.fhir.instance.model.api.IBaseResource;

@Setter
class LocationConverter<T extends IBaseResource> implements Converter<FhirLocation, T> {

  private LocationConverterStrategy<T> strategy;
  private String serviceUrl;

  @Override
  public T convert(FhirLocation input) {
    T resource = strategy.initiateResource();
    strategy.setName(resource, input);
    strategy.setPhysicalType(resource, input);
    strategy.setPartOf(resource, input);
    strategy.setIdentifier(resource, input);
    strategy.addSystemIdentifier(resource, serviceUrl, input.getId());
    strategy.setAlias(resource, input);
    strategy.setPosition(resource, input);
    strategy.setDescription(resource, input);
    strategy.setStatus(resource, input);

    return resource;
  }

}
