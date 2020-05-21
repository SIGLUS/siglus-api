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

import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface LocationConverterStrategy<T extends IBaseResource> {

  T initiateResource();

  void setName(T resource, FhirLocation input);

  void setPhysicalType(T resource, FhirLocation input);

  void setPartOf(T resource, FhirLocation input);

  void setIdentifier(T resource, FhirLocation input);

  void addSystemIdentifier(T resource, String system, UUID value);

  void setAlias(T resource, FhirLocation input);

  void setPosition(T resource, FhirLocation input);

  void setDescription(T resource, FhirLocation input);

  void setStatus(T resource, FhirLocation input);
}
