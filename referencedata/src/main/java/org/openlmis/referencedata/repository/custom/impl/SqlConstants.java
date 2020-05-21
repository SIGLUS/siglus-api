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

package org.openlmis.referencedata.repository.custom.impl;

import java.util.Collection;

class SqlConstants {

  static final String SELECT = "SELECT";
  static final String SELECT_DISTINCT = join(SELECT, "DISTINCT");
  static final String SELECT_DISTINCT_COUNT = join(SELECT_DISTINCT, "COUNT(*)");
  static final String FROM = "FROM";
  static final String AS = "AS";
  static final String WHERE = "WHERE";
  static final String LEFT_JOIN_FETCH = "LEFT JOIN FETCH";
  static final String ORDER_BY = "ORDER BY";
  static final String ID = "id";

  private SqlConstants() {}

  static String join(String... parameters) {
    return String.join(" ", parameters);
  }

  static String in(String parameterName) {
    return String.format("IN (:%s)", parameterName);
  }

  static String and(Collection<String> conditions) {
    return String.join(" AND ", conditions);
  }

  static String and(String... parameters) {
    return String.join(" AND ", parameters);
  }

  static String getField(String... fields) {
    return String.join(".", fields);
  }

  static String isEqual(String left, String right) {
    return join(left, "=", right);
  }

  static String asParameter(String parameterName) {
    return ":" + parameterName;
  }
}
