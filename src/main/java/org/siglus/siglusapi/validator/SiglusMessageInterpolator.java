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

package org.siglus.siglusapi.validator;

import java.util.Locale;
import javax.annotation.Nonnull;
import javax.validation.MessageInterpolator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SiglusMessageInterpolator implements MessageInterpolator {

  @Nonnull
  private final MessageInterpolator targetInterpolator;

  @Override
  public String interpolate(String message, Context context) {
    String messageInEnglish = this.targetInterpolator.interpolate(message, context, Locale.ENGLISH);
    return String.format("%s", messageInEnglish);
  }

  @Override
  public String interpolate(String message, Context context, Locale locale) {
    return this.interpolate(message, context);
  }


}
