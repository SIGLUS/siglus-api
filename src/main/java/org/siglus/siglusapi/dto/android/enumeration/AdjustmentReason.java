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

package org.siglus.siglusapi.dto.android.enumeration;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AdjustmentReason {
  CUSTOMER_RETURN("Devolução Dentro do prazo de validade dos clientes (US e Depósitos Beneficiários)"),
  DAMAGED("Danificado no depósito"),
  DONATION("Doações ao Depósito"),
  EXPIRED_RETURN_FROM_CUSTOMER("Devolução de expirados (US e Depósitos Beneficiários)"),
  EXPIRED_RETURN_TO_SUPPLIER("Devolução de expirados para Depósito fornecedor"),
  INVENTORY_NEGATIVE(
      "Correcção de inventário, no caso do stock inferior (stock é inferior ao existente na ficha de stock)"),
  INVENTORY_POSITIVE(
      "Correcção de inventário, no caso do stock Superior (stock é Superior ao existente na ficha de stock)"),
  LOANS_DEPOSIT("Empréstimos (para todos níveis) que dão saída do depósito"),
  LOANS_RECEIVED("Empréstimos (de todos os níveis) que dão entrada no depósito"),
  PROD_DEFECTIVE("Saída para quarentena, no caso de problemas relativos a qualidade"),
  RETURN_FROM_QUARANTINE("Da quarentena para Depósito, no caso de se confirmar a qualidade do produto"),
  RETURN_TO_SUPPLIER("Devolução Dentro do prazo de validade ao Depósito fornecedor"),
  RECEIVE("Receive"),
  ISSUE("Issue"),
  EXPIRED_DISCARD("Fora do prazo de validade"),
  EXPIRED_RETURN_TO_SUPPLIER_AND_DISCARD("Transferência de produtos expirados");

  private final String name;

  public static Optional<AdjustmentReason> findByName(String name) {
    return Arrays.stream(values()).filter(e -> e.name.equals(name)).findFirst();
  }

}
