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
  CUSTOMER_RETURN("Devoluções de clientes (US e Enfermarias Dependentes)"),
  DAMAGED("Danificado na Chegada"),
  DONATION("Doação para o Deposito"),
  EXPIRED_RETURN_FROM_CUSTOMER("Devoluções de Expirados (US e Enfermarias de Dependentes)"),
  EXPIRED_RETURN_TO_SUPPLIER("Devolvidos ao Fornecedor por terem Expirados em Quarentena"),
  INVENTORY_NEGATIVE("Correção Negativa"),
  INVENTORY_POSITIVE("Correção Positiva"),
  LOANS_DEPOSIT("Emprestimo Enviado pela US"),
  LOANS_RECEIVED("Emprestimo Recebido pela US"),
  PROD_DEFECTIVE("Produto com defeito, movido para quarentena"),
  RETURN_FROM_QUARANTINE("Devoluções da Quarentena"),
  RETURN_TO_DDM("Devolução para o DDM"),
  UNPACK_KIT("Unpack Kit");

  private final String name;

  public static Optional<AdjustmentReason> findByName(String name) {
    return Arrays.stream(values())
        .filter(e -> e.name.equals(name))
        .findFirst();
  }

}
