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

package org.openlmis.referencedata.dto;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.openlmis.referencedata.domain.Right;
import org.openlmis.referencedata.domain.RightType;

public class RightDto extends BaseDto implements Right.Exporter, Right.Importer {

  @Getter
  @Setter
  private String name;

  @Getter
  @Setter
  private RightType type;

  @Getter
  @Setter
  private String description;

  private Set<RightDto> attachments = new HashSet<>();

  @Override
  public void setAttachments(Set<Right> attachments) {
    for (Right attachment : attachments) {
      RightDto attachmentDto = new RightDto();
      attachment.export(attachmentDto);
      this.attachments.add(attachmentDto);
    }
  }
  
  @Override
  public Set<Right.Importer> getAttachments() {
    Set<Right.Importer> attachments = new HashSet<>();
    attachments.addAll(this.attachments);
    return attachments;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RightDto)) {
      return false;
    }
    RightDto rightDto = (RightDto) obj;
    return Objects.equals(name, rightDto.name);
  }
}
