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

import static java.util.Optional.ofNullable;

import java.util.Objects;
import lombok.Getter;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.zalando.problem.spring.web.advice.validation.Violation;

@Getter
public class FieldViolation {

  private final String propertyPath;

  @JsonIgnore
  private final String messageTemplate;

  private final String message;

  private final String messageInEnglish;

  private final String messageInPortuguese;

  public FieldViolation(Violation violation) {
    propertyPath = ofNullable(violation.getField()).map(Objects::toString).orElse(null);
    String[] messages = violation.getMessage().split("\\|");
    messageTemplate = messages[0];
    message = messages[1];
    messageInEnglish = messages[2];
    messageInPortuguese = messages[3];
  }

}
