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

package org.openlmis.referencedata.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.persistence.Embeddable;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents a unique designation.
 */
@Embeddable
public class Code {
  private final String code;

  protected Code() {
    this.code = "";
  }

  private Code(String code) {
    this.code = code.replaceAll("\\s", "");
  }

  /**
   * is this blank.
   * @return true if blank, false otherwise.
   */
  public boolean isBlank() {
    return StringUtils.isBlank(this.code);
  }

  /**
   * Code equality ignores whitespace and case.
   * @param object the Code to test against.
   * @return true if both represent the same code, false otherwise.
   */
  @Override
  public final boolean equals(Object object) {
    if (null == object) {
      return false;
    }

    if (!(object instanceof Code)) {
      return false;
    }

    return this.code.equalsIgnoreCase(((Code) object).code);
  }

  @Override
  public final int hashCode() {
    return code.toUpperCase().hashCode();
  }

  @Override
  @JsonValue
  public String toString() {
    return code;
  }

  /**
   * Creates a new Code value.
   * @param code the code
   * @return a new Code with the given code.  Uses a blank code if given null.
   */
  @JsonCreator
  public static final Code code(String code) {
    String workingCode = (null == code) ? "" : code;
    return new Code(workingCode);
  }
}
