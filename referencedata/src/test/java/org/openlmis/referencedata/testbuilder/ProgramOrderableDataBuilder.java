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

package org.openlmis.referencedata.testbuilder;

import java.util.UUID;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.OrderableDisplayCategory;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.ProgramOrderable;

public class ProgramOrderableDataBuilder {

  private UUID id;
  private Program program;
  private Integer dosesPerPatient;
  private boolean active;
  private OrderableDisplayCategory orderableDisplayCategory;
  private boolean fullSupply;
  private int displayOrder;
  private Orderable product;
  private CurrencyUnit currencyUnit;
  private Money pricePerPack;

  /**
   * Returns instance of {@link ProgramOrderableDataBuilder} with sample data.
   */
  public ProgramOrderableDataBuilder() {
    id = UUID.randomUUID();
    program = new ProgramDataBuilder().build();
    dosesPerPatient = 0;
    active = true;
    fullSupply = true;
    orderableDisplayCategory = null;
    displayOrder = 0;
    product = new OrderableDataBuilder().build();
    currencyUnit = CurrencyUnit.of("USD");
    pricePerPack = Money.of(currencyUnit, 0);
  }

  /**
   * Builds instance of {@link ProgramOrderable}.
   */
  public ProgramOrderable build() {
    ProgramOrderable programOrderable = buildAsNew();
    programOrderable.setId(id);

    return programOrderable;
  }

  /**
   * Builds instance of {@link ProgramOrderable} without id field.
   */
  public ProgramOrderable buildAsNew() {
    return ProgramOrderable.createNew(program, orderableDisplayCategory, product, dosesPerPatient,
        active, fullSupply, displayOrder, pricePerPack, currencyUnit);
  }

  public ProgramOrderableDataBuilder withProgram(Program program) {
    this.program = program;
    return this;
  }

  public ProgramOrderableDataBuilder withProduct(Orderable orderable) {
    this.product = orderable;
    return this;
  }

  public ProgramOrderableDataBuilder withoutProduct() {
    this.product = null;
    return this;
  }

  public ProgramOrderableDataBuilder asNonFullSupply() {
    this.fullSupply = false;
    return this;
  }

  public ProgramOrderableDataBuilder withOrderabeDisplayCategory(
      OrderableDisplayCategory orderabeDisplayCategory) {
    this.orderableDisplayCategory = orderabeDisplayCategory;
    return this;
  }

  public ProgramOrderableDataBuilder asInactive() {
    this.active = false;
    return this;
  }

}
