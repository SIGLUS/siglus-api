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

package org.siglus.siglusapi.errorhandling;

import org.siglus.siglusapi.exception.BaseMessageException;
import org.siglus.siglusapi.i18n.MessageService;
import org.siglus.siglusapi.util.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Base classes for controller advices dealing with error handling.
 */
@ControllerAdvice
public abstract class AbstractErrorHandling {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private MessageService messageService;

  /**
   * Translate a Message into a LocalizedMessage.
   *
   * @param message a Message to translate
   * @return a LocalizedMessage translated by the MessageService bean
   */
  final Message.LocalizedMessage getLocalizedMessage(Message message) {
    return messageService.localize(message);
  }

  /**
   * Translate the Message in a BaseMessageException into a LocalizedMessage.
   *
   * @param exception is any BaseMessageException containing a Message
   * @return a LocalizedMessage translated by the MessageService bean
   */
  final Message.LocalizedMessage getLocalizedMessage(BaseMessageException exception) {
    Message.LocalizedMessage message = messageService.localize(exception.asMessage());
    logger.error("{}", message);
    return message;
  }

}
