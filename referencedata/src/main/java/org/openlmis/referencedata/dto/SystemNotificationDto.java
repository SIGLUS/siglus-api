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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.referencedata.domain.SystemNotification;
import org.openlmis.referencedata.domain.User;
import org.openlmis.referencedata.web.BaseController;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class SystemNotificationDto extends BaseDto implements SystemNotification.Exporter,
    SystemNotification.Importer {

  @Setter
  @JsonIgnore
  private String serviceUrl;

  @Getter
  @Setter
  private String title;

  @Getter
  @Setter
  private String message;

  @Getter
  @Setter
  private ZonedDateTime startDate;

  @Getter
  @Setter
  private ZonedDateTime expiryDate;

  @Getter
  @Setter
  private ZonedDateTime createdDate;

  @Getter
  @Setter
  private boolean active;

  @Getter
  private UserObjectReferenceDto author;

  @Getter
  @Setter
  private Boolean isDisplayed;

  @Override
  @JsonIgnore
  public UUID getAuthorId() {
    return Optional
        .ofNullable(author)
        .map(BaseDto::getId)
        .orElse(null);
  }

  @JsonSetter("author")
  public void setAuthor(UserObjectReferenceDto author) {
    this.author = author;
  }

  @Override
  @JsonIgnore
  public void setAuthor(User author) {
    this.author = new UserObjectReferenceDto(serviceUrl,
        BaseController.API_PATH + "/users", author.getId());
  }

  /**
   * Creates a new instance based on data from a domain object.
   */
  public static SystemNotificationDto newInstance(SystemNotification systemNotification,
      String serviceUrl, Boolean isDisplayed) {
    SystemNotificationDto dto = new SystemNotificationDto();
    dto.setServiceUrl(serviceUrl);
    if (isDisplayed != null) {
      dto.setIsDisplayed(isDisplayed);
    }
    systemNotification.export(dto);

    return dto;
  }
}
