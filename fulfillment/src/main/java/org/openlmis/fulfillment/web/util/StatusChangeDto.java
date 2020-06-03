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

package org.openlmis.fulfillment.web.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.ZonedDateTime;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.fulfillment.domain.ExternalStatus;
import org.openlmis.fulfillment.domain.StatusChange;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.springframework.context.i18n.LocaleContextHolder;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusChangeDto implements StatusChange.Exporter, StatusChange.Importer {

  @Getter
  @Setter
  private ExternalStatus status;

  @Getter
  @Setter
  private UUID authorId;

  @Getter
  @Setter
  private ZonedDateTime createdDate;

  @Setter
  private UserDto author;

  @JsonIgnore
  public UserDto getAuthor() {
    return author;
  }

  /**
   * Print createdDate for display purposes.
   * @return created date
   */
  @JsonIgnore
  public String printDate() {
    Locale locale = LocaleContextHolder.getLocale();
    String datePattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            FormatStyle.MEDIUM, FormatStyle.MEDIUM, Chronology.ofLocale(locale), locale);
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(datePattern);

    return dateTimeFormatter.format(createdDate);
  }

  /**
   * Create new instance of StatusChangeDto based on given {@link StatusChange}.
   * @param statusChange instance of StatusChange
   * @return new instance of StatusChangeDto.
   */
  public static StatusChangeDto newInstance(StatusChange statusChange) {
    StatusChangeDto statusChangeDto = new StatusChangeDto();
    statusChange.export(statusChangeDto);
    return statusChangeDto;
  }
}
