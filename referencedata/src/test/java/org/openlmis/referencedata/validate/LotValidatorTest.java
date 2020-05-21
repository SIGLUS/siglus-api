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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.openlmis.referencedata.validate.LotValidator.LOT_CODE;
import static org.openlmis.referencedata.validate.LotValidator.TRADE_ITEM_ID;
import static org.openlmis.referencedata.validate.ValidationTestUtils.assertErrorMessage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.referencedata.domain.TradeItem;
import org.openlmis.referencedata.dto.LotDto;
import org.openlmis.referencedata.repository.LotRepository;
import org.openlmis.referencedata.repository.TradeItemRepository;
import org.openlmis.referencedata.testbuilder.LotDataBuilder;
import org.openlmis.referencedata.testbuilder.TradeItemDataBuilder;
import org.openlmis.referencedata.util.messagekeys.LotMessageKeys;
import org.openlmis.referencedata.util.messagekeys.TradeItemMessageKeys;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class LotValidatorTest {

  private static final String CODE = "code";

  @Mock
  private TradeItemRepository tradeItemRepository;

  @Mock
  private LotRepository lotRepository;

  @InjectMocks
  private Validator validator = new LotValidator();

  private LotDto lotDto;
  private TradeItem tradeItem;
  private Errors errors;

  @Before
  public void setUp() throws Exception {
    lotDto = new LotDto();
    lotDto.setLotCode(CODE);
    lotDto.setExpirationDate(LocalDate.now());
    lotDto.setManufactureDate(LocalDate.now());
    lotDto.setActive(true);
    lotDto.setId(UUID.randomUUID());

    tradeItem =
        new TradeItem("manufacturer", null);
    tradeItem.setId(UUID.randomUUID());
    lotDto.setTradeItemId(tradeItem.getId());

    when(tradeItemRepository.findOne(tradeItem.getId())).thenReturn(tradeItem);

    errors = new BeanPropertyBindingResult(lotDto, "lotDto");
  }

  @Test
  public void shouldNotFindErrorsWhenLotIsValid() throws Exception {
    when(lotRepository.existsByLotCodeIgnoreCaseAndTradeItemId(lotDto.getLotCode(),
        lotDto.getTradeItemId())).thenReturn(false);

    validator.validate(lotDto, errors);

    assertEquals(0, errors.getErrorCount());
  }

  @Test
  public void shouldRejectWhenLotCodeIsNull() {
    lotDto.setLotCode(null);

    validator.validate(lotDto, errors);

    assertErrorMessage(errors, LOT_CODE, LotMessageKeys.ERROR_LOT_CODE_REQUIRED);
  }

  @Test
  public void shouldRejectWhenLotCodeIsEmpty() {
    lotDto.setLotCode("");

    validator.validate(lotDto, errors);

    assertErrorMessage(errors, LOT_CODE, LotMessageKeys.ERROR_LOT_CODE_REQUIRED);
  }

  @Test
  public void shouldRejectWhenLotCodeIsWhitespace() {
    lotDto.setLotCode(" ");

    validator.validate(lotDto, errors);

    assertErrorMessage(errors, LOT_CODE, LotMessageKeys.ERROR_LOT_CODE_REQUIRED);
  }

  @Test
  public void shouldRejectWhenLotCodeAlreadyExist() {
    Lot lot = new Lot();
    lot.setLotCode(CODE);
    lot.setTradeItem(tradeItem);
    lot.setId(UUID.randomUUID());
    List<Lot> lots = new ArrayList<>();
    lots.add(lot);

    when(lotRepository.existsByLotCodeIgnoreCaseAndTradeItemId(lot.getLotCode(),
        lot.getTradeItem().getId())).thenReturn(true);

    validator.validate(lotDto, errors);

    assertErrorMessage(errors, LOT_CODE, LotMessageKeys.ERROR_LOT_CODE_MUST_BE_UNIQUE);
  }

  @Test
  public void shouldNotFindErrorsWhenLotCodeAlreadyExistButIsTheSameLot() throws Exception {
    Lot lot = new Lot();
    lot.setLotCode(CODE);
    lot.setId(lotDto.getId());
    List<Lot> lots = new ArrayList<>();
    lots.add(lot);

    when(lotRepository.existsByLotCodeIgnoreCaseAndTradeItemId(lotDto.getLotCode(),
        lotDto.getTradeItemId())).thenReturn(false);

    validator.validate(lotDto, errors);

    assertEquals(0, errors.getErrorCount());
  }

  @Test
  public void shouldNotFindErrorsWhenLotCodeExistsButForDifferentTradeItem() throws Exception {
    TradeItem tradeItem = new TradeItemDataBuilder().build();
    Lot lot = new LotDataBuilder().build();
    lot.setLotCode(CODE);
    lot.setTradeItem(tradeItem);
    List<Lot> lots = new ArrayList<>();
    lots.add(lot);

    when(lotRepository.existsByLotCodeIgnoreCaseAndTradeItemId(lot.getLotCode(),
        lot.getTradeItem().getId())).thenReturn(false);

    validator.validate(lotDto, errors);

    assertEquals(0, errors.getErrorCount());
  }

  @Test
  public void shouldRejectWhenTradeItemIsNull() {
    when(lotRepository.existsByLotCodeIgnoreCaseAndTradeItemId(lotDto.getLotCode(),
        lotDto.getTradeItemId())).thenReturn(false);

    lotDto.setTradeItemId(null);

    validator.validate(lotDto, errors);

    assertErrorMessage(errors, TRADE_ITEM_ID, LotMessageKeys.ERROR_TRADE_ITEM_REQUIRED);
  }

  @Test
  public void shouldRejectWhenTradeItemDoesNotExist() {
    when(lotRepository.existsByLotCodeIgnoreCaseAndTradeItemId(lotDto.getLotCode(),
        lotDto.getTradeItemId())).thenReturn(false);

    when(tradeItemRepository.findOne(lotDto.getTradeItemId())).thenReturn(null);

    validator.validate(lotDto, errors);

    assertErrorMessage(errors, TRADE_ITEM_ID, TradeItemMessageKeys.ERROR_NOT_FOUND_WITH_ID);
  }
}
