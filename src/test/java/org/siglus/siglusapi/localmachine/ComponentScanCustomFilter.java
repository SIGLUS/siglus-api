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

import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

public class ComponentScanCustomFilter implements TypeFilter {

  @Override
  public boolean match(MetadataReader metadataReader,
      MetadataReaderFactory metadataReaderFactory) {
    ClassMetadata classMetadata = metadataReader.getClassMetadata();
    String fullyQualifiedName = classMetadata.getClassName();
    String className = fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf('.') + 1);

    return fullyQualifiedName.contains("org.siglus.siglusapi.localmachine.event.")
        || className.contains("Repository")
        || className.contains("Controller")
        || className.equals("RequisitionInternalApproveEmitter")
        || className.equals("RequisitionInternalApproveReplayer")
        || className.equals(Machine.class.getSimpleName());
  }
}
