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

package org.siglus.siglusapi.exception;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.siglus.siglusapi.localmachine.utils.ErrorFormatter;
import org.springframework.data.util.Pair;

public class DeferredException extends RuntimeException {
  private final List<Pair<String, Exception>> exceptions = new LinkedList<>();

  public DeferredException add(String contextId, Exception item) {
    exceptions.add(Pair.of(contextId, item));
    return this;
  }

  public List<Exception> getExceptions() {
    return exceptions.stream().map(Pair::getSecond).collect(Collectors.toList());
  }

  public void emit() {
    if (exceptions.isEmpty()) {
      return;
    }
    throw this;
  }

  @Override
  public String getMessage() {
    return StringUtils.join(
        exceptions.stream().map(this::formatException).collect(Collectors.toList()), ",");
  }

  private String formatException(Pair<String, Exception> it) {
    return String.format(
        "%s:%s (%s)",
        it.getFirst(), it.getSecond(), ErrorFormatter.getRootStackTrace(it.getSecond()));
  }
}
