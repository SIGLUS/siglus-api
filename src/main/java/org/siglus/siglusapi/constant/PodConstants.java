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

public class PodConstants {

  private PodConstants() {}

  public static final String NOT_EXIST_MESSAGE = "Sync failed. This order number does not exist."
      + " Please confirm whether this order number is correct.";

  public static final String NOT_EXIST_MESSAGE_PT = "A sincronização falhou. Este número de pedido não existe."
      + " Confirme se este número de pedido está correto.";

  public static final String ERROR_MESSAGE = "Sync failed. There is an error. Please contact the administrator.";

  public static final String ERROR_MESSAGE_PT = "A sincronização falhou. Há um erro. "
      + "Entre em contato com o administrador.";

}
