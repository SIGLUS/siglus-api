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

package org.openlmis.requisition.domain.type;

import static org.openlmis.requisition.CurrencyConfig.currencyCode;

import java.math.BigDecimal;
import java.util.Properties;
import lombok.EqualsAndHashCode;
import org.hibernate.SessionFactory;
import org.hibernate.usertype.ParameterizedType;
import org.jadira.usertype.moneyandcurrency.joda.columnmapper.BigDecimalColumnMoneyMapper;
import org.jadira.usertype.moneyandcurrency.joda.util.CurrencyUnitConfigured;
import org.jadira.usertype.spi.shared.AbstractSingleColumnUserType;
import org.jadira.usertype.spi.shared.IntegratorConfiguredType;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

@EqualsAndHashCode(exclude = {"columnMapper", "sqlTypes"}, callSuper = false)
public final class CustomSingleColumnMoneyUserType
    extends AbstractSingleColumnUserType<Money, BigDecimal, BigDecimalColumnMoneyMapper>
    implements ParameterizedType, IntegratorConfiguredType {

  private static final long serialVersionUID = -40478801316537388L;
  private Properties parameterValues;

  @Override
  public void setParameterValues(Properties parameters) {
    this.parameterValues = parameters;
  }

  protected Properties getParameterValues() {
    return parameterValues;
  }

  @Override
  public void applyConfiguration(SessionFactory sessionFactory) {
    CurrencyUnitConfigured columnMapper = getColumnMapper();
    columnMapper.setCurrencyUnit(CurrencyUnit.of(currencyCode));
  }
}
