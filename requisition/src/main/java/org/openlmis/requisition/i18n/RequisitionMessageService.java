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

package org.openlmis.requisition.i18n;

import java.util.Locale;
import org.openlmis.requisition.utils.Message;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class RequisitionMessageService {

  private final XLogger logger = XLoggerFactory.getXLogger(getClass());

  @Autowired
  @Qualifier("requisitionMessageSource")
  private ExposedMessageSource messageSource;

  /**
   * Translates message.
   *
   * @param message to be localized
   * @return localized message
   */
  public Message.LocalizedMessage localize(Message message) {
    return localize(message, LocaleContextHolder.getLocale());
  }

  /**
   * Translates message with parametrized locale.
   *
   * @param message to be localized
   * @param locale  language used for translating
   * @return localized message
   */
  public Message.LocalizedMessage localize(Message message, Locale locale) {
    logger.info("Requisition message service message key {}", message.getKey());
    logger.info("Requisition message service locale {}", LocaleContextHolder.getLocale());
    Message.LocalizedMessage localized = message.localMessage(messageSource, locale);
    logger.info("Requisition message service localized {}", localized.asMessage());
    return localized;
  }
}