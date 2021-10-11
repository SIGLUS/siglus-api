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
  CUSTOMER_RETURN("Devolução dos clientes (US e Depósitos Beneficiários)"),
  DAMAGED("Danificado no depósito"),
  DONATION("Doações ao Depósito"),
  EXPIRED_RETURN_FROM_CUSTOMER("Devolução de expirados (US e Depósitos Beneficiários)"),
  EXPIRED_RETURN_TO_SUPPLIER("Devolução de expirados quarentena (ou depósito fornecedor)"),
  INVENTORY_NEGATIVE("Correcção de inventário, no caso do stock em falta  (stock é inferior ao existente na ficha de stock) "),
  INVENTORY_POSITIVE("Correcção de inventário, no caso do stock em excesso (stock é superior ao existente na ficha de stock) "),
  LOANS_DEPOSIT("Empréstimos (para todos níveis) que dão saída do depósito"),
  LOANS_RECEIVED("Empréstimos (de todos os níveis) que dão entrada no depósito"),
  PROD_DEFECTIVE("Saída para quarentena, no caso de problemas relativos a qualidade "),
  RETURN_FROM_QUARANTINE("Retorno da quarentena, no caso de se confirmar a qualidade do produto"),
  RETURN_TO_DDM("Devolução para o DDM"),
  UNPACK_KIT("Unpack Kit");

  private final String name;

  public static Optional<AdjustmentReason> findByName(String name) {
    return Arrays.stream(values()).filter(e -> e.name.equals(name)).findFirst();
  }

}
