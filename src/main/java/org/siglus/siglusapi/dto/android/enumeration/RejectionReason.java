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
public enum RejectionReason {
  DAMAGED("Danificado/quebrado/derramado"),
  INSUFFICIENT("Quantidade recebida a menos (no lote)"),
  EXPIRED("Fora do prazo de validade"),
  UNEATABLE("Impróprio para o consumo"),
  EXCESS("Quantidade recebida a mais (no lote)"),
  LOT_NOT_SPECIFIED("Lote não especificado na Guia de Remessa"),
  INCORRECT_BATCH("Lote Incorreto");

  private final String name;

  public static Optional<RejectionReason> findByName(String name) {
    return Arrays.stream(values())
        .filter(e -> e.name.equals(name))
        .findFirst();
  }

}
