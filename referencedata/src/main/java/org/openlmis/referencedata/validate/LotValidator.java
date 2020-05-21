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

package org.openlmis.referencedata.validate;

import java.util.UUID;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.referencedata.domain.TradeItem;
import org.openlmis.referencedata.dto.LotDto;
import org.openlmis.referencedata.repository.LotRepository;
import org.openlmis.referencedata.repository.TradeItemRepository;
import org.openlmis.referencedata.util.messagekeys.LotMessageKeys;
import org.openlmis.referencedata.util.messagekeys.TradeItemMessageKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * A validator for {@link LotDto} object.
 */
@Component
public class LotValidator implements BaseValidator {

  // Lot fields
  static final String LOT_CODE = "lotCode";
  static final String TRADE_ITEM_ID = "tradeItemId";

  @Autowired
  private TradeItemRepository tradeItemRepository;

  @Autowired
  private LotRepository lotRepository;

  /**
   * Checks if the given class definition is supported.
   *
   * @param clazz the {@link Class} that this {@link Validator} is being asked if it can {@link
   *              #validate(Object, Errors) validate}
   * @return true if {@code clazz} is equal to {@link LotDto} class definition. Otherwise false.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return LotDto.class.equals(clazz);
  }

  /**
   * Validates the {@code target} object, which must be an instance of {@link Lot} class.
   *
   * @param target the object that is to be validated (never {@code null})
   * @param errors contextual state about the validation process (never {@code null})
   * @see ValidationUtils
   */
  @Override
  public void validate(Object target, Errors errors) {
    verifyArguments(target, errors, LotMessageKeys.ERROR_NULL);
    rejectIfEmptyOrWhitespace(errors, LOT_CODE, LotMessageKeys.ERROR_LOT_CODE_REQUIRED);

    if (!errors.hasErrors()) {
      LotDto lotDto = (LotDto) target;
      verifyTradeItem(lotDto, errors);
    }
  }

  private void verifyCode(LotDto lot, Errors errors) {
    boolean existsByCodeAndTradeItem = lotRepository.existsByLotCodeIgnoreCaseAndTradeItemId(
        lot.getLotCode(), lot.getTradeItemId());
    if (existsByCodeAndTradeItem) {
      rejectValue(errors, LOT_CODE, LotMessageKeys.ERROR_LOT_CODE_MUST_BE_UNIQUE, lot.getLotCode(),
          String.valueOf(lot.getTradeItemId()));
    }
  }

  private void verifyTradeItem(LotDto lot, Errors errors) {
    UUID tradeItemId = lot.getTradeItemId();
    if (tradeItemId == null) {
      rejectValue(errors, TRADE_ITEM_ID, LotMessageKeys.ERROR_TRADE_ITEM_REQUIRED);
    } else {
      TradeItem tradeItem = tradeItemRepository.findOne(tradeItemId);
      if (tradeItem == null) {
        rejectValue(errors, TRADE_ITEM_ID, TradeItemMessageKeys.ERROR_NOT_FOUND_WITH_ID,
            String.valueOf(tradeItemId));
      } else {
        verifyCode(lot, errors);
      }
    }
  }
}
