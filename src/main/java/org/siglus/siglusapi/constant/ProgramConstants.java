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

package org.siglus.siglusapi.constant;

import java.util.UUID;

public class ProgramConstants {

  private ProgramConstants() {
  }

  public static final UUID ALL_PRODUCTS_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  public static final UUID ALL_PRODUCTS_PROGRAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  public static final String ALL_PRODUCTS_PROGRAM_CODE = "ALL";

  public static final String ALL_PRODUCTS_PROGRAM_NAME = "Todos os produtos";

  public static final String VIA_PROGRAM_NAME = "VC";

  public static final String MMIA_PROGRAM_NAME = "T";

  public static final String MALARIA_PROGRAM_NAME = "ML";

  public static final String RAPIDTEST_PROGRAM_NAME = "TR";
}
