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

package org.siglus.siglusapi.dto.android;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public class StocksOnHandTest {

  @Test
  public void shouldReturnLotDateWhenStockGivenNoStockBeforeEveryLot() {
    // given
    List<ProductLotStock> stocks = new ArrayList<>(generateLots());
    ProductLotStock noStock = ProductLotStock.builder()
        .code(ProductLotCode.of("test", null))
        .stockQuantity(0)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 7, 30), Instant.now()))
        .build();
    stocks.add(noStock);

    // when
    StocksOnHand stocksOnHand = new StocksOnHand(stocks);

    // then
    assertEquals(LocalDate.of(2021, 8, 31), stocksOnHand.getTheEarliestDate(LocalDate.MAX));
    assertEquals(asList("test", "26A01"), stocksOnHand.getAllProductCodes());
    assertEquals(27, (int) stocksOnHand.getStockQuantityByProduct("test"));
    Map<ProductLotCode, InventoryDetail> lotsOfTest = stocksOnHand.getLotInventoriesByProduct("test");
    assertEquals(3, lotsOfTest.size());
    Map<ProductLotCode, InventoryDetail> lotsOfKit = stocksOnHand.getLotInventoriesByProduct("26A01");
    assertEquals(1, lotsOfKit.size());
    Map<ProductLotCode, Integer> lots = stocksOnHand.getLotInventories();
    assertEquals(4, lots.size());
    Lot lot1 = stocksOnHand.getLot(ProductLotCode.of("test", "lot1"));
    assertEquals("lot1", lot1.getCode());
    // check latest date effected by no stock record
    InventoryDetail lot1Inventory = stocksOnHand.findInventory(ProductLotCode.of("test", "lot1"));
    assertEquals(10, (int) lot1Inventory.getStockQuantity());
    assertEquals(LocalDate.of(2021, 10, 1), lot1Inventory.getEventTime().getOccurredDate());
    InventoryDetail lot2Inventory = stocksOnHand.findInventory(ProductLotCode.of("test", "lot2"));
    assertEquals(9, (int) lot2Inventory.getStockQuantity());
    assertEquals(LocalDate.of(2021, 9, 15), lot2Inventory.getEventTime().getOccurredDate());
    InventoryDetail lot3Inventory = stocksOnHand.findInventory(ProductLotCode.of("test", "lot3"));
    assertEquals(8, (int) lot3Inventory.getStockQuantity());
    assertEquals(LocalDate.of(2021, 8, 31), lot3Inventory.getEventTime().getOccurredDate());
  }

  @Test
  public void shouldReturnLotDateWhenStockGivenNoStockBeforeSomeLot() {
    // given
    List<ProductLotStock> stocks = new ArrayList<>(generateLots());
    ProductLotStock noStock = ProductLotStock.builder()
        .code(ProductLotCode.of("test", null))
        .stockQuantity(0)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 9, 1), Instant.now()))
        .build();
    stocks.add(noStock);

    // when
    StocksOnHand stocksOnHand = new StocksOnHand(stocks);

    // then
    assertEquals(LocalDate.of(2021, 9, 1), stocksOnHand.getTheEarliestDate(LocalDate.MAX));
    assertEquals(asList("test", "26A01"), stocksOnHand.getAllProductCodes());
    assertEquals(19, (int) stocksOnHand.getStockQuantityByProduct("test"));
    Map<ProductLotCode, InventoryDetail> lotsOfTest = stocksOnHand.getLotInventoriesByProduct("test");
    assertEquals(3, lotsOfTest.size());
    Map<ProductLotCode, InventoryDetail> lotsOfKit = stocksOnHand.getLotInventoriesByProduct("26A01");
    assertEquals(1, lotsOfKit.size());
    Map<ProductLotCode, Integer> lots = stocksOnHand.getLotInventories();
    assertEquals(4, lots.size());
    Lot lot1 = stocksOnHand.getLot(ProductLotCode.of("test", "lot1"));
    assertEquals("lot1", lot1.getCode());
    // check latest date effected by no stock record
    InventoryDetail lot1Inventory = stocksOnHand.findInventory(ProductLotCode.of("test", "lot1"));
    assertEquals(10, (int) lot1Inventory.getStockQuantity());
    assertEquals(LocalDate.of(2021, 10, 1), lot1Inventory.getEventTime().getOccurredDate());
    InventoryDetail lot2Inventory = stocksOnHand.findInventory(ProductLotCode.of("test", "lot2"));
    assertEquals(9, (int) lot2Inventory.getStockQuantity());
    assertEquals(LocalDate.of(2021, 9, 15), lot2Inventory.getEventTime().getOccurredDate());
    InventoryDetail lot3Inventory = stocksOnHand.findInventory(ProductLotCode.of("test", "lot3"));
    assertEquals(0, (int) lot3Inventory.getStockQuantity());
    assertEquals(LocalDate.of(2021, 9, 1), lot3Inventory.getEventTime().getOccurredDate());
  }

  @Test
  public void shouldReturnLotDateWhenStockGivenNoStockAfterEveryLot() {
    // given
    List<ProductLotStock> stocks = new ArrayList<>(generateLots());
    ProductLotStock noStock = ProductLotStock.builder()
        .code(ProductLotCode.of("test", null))
        .stockQuantity(0)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 10, 15), Instant.now()))
        .build();
    stocks.add(noStock);

    // when
    StocksOnHand stocksOnHand = new StocksOnHand(stocks);

    // then
    assertEquals(LocalDate.of(2021, 10, 15), stocksOnHand.getTheEarliestDate(LocalDate.MAX));
    assertEquals(asList("test", "26A01"), stocksOnHand.getAllProductCodes());
    assertEquals(0, (int) stocksOnHand.getStockQuantityByProduct("test"));
    Map<ProductLotCode, InventoryDetail> lotsOfTest = stocksOnHand.getLotInventoriesByProduct("test");
    assertEquals(3, lotsOfTest.size());
    Map<ProductLotCode, InventoryDetail> lotsOfKit = stocksOnHand.getLotInventoriesByProduct("26A01");
    assertEquals(1, lotsOfKit.size());
    Map<ProductLotCode, Integer> lots = stocksOnHand.getLotInventories();
    assertEquals(4, lots.size());
    stocksOnHand.getLot(ProductLotCode.of("test", "lot1"));
    Lot lot1 = stocksOnHand.getLot(ProductLotCode.of("test", "lot1"));
    assertEquals("lot1", lot1.getCode());
    // check latest date effected by no stock record
    InventoryDetail lot1Inventory = stocksOnHand.findInventory(ProductLotCode.of("test", "lot1"));
    assertEquals(0, (int) lot1Inventory.getStockQuantity());
    assertEquals(LocalDate.of(2021, 10, 15), lot1Inventory.getEventTime().getOccurredDate());
    InventoryDetail lot2Inventory = stocksOnHand.findInventory(ProductLotCode.of("test", "lot2"));
    assertEquals(0, (int) lot2Inventory.getStockQuantity());
    assertEquals(LocalDate.of(2021, 10, 15), lot2Inventory.getEventTime().getOccurredDate());
    InventoryDetail lot3Inventory = stocksOnHand.findInventory(ProductLotCode.of("test", "lot3"));
    assertEquals(0, (int) lot3Inventory.getStockQuantity());
    assertEquals(LocalDate.of(2021, 10, 15), lot3Inventory.getEventTime().getOccurredDate());
  }

  private List<ProductLotStock> generateLots() {
    ProductLotStock lot1 = ProductLotStock.builder()
        .code(ProductLotCode.of("test", "lot1"))
        .stockQuantity(10)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 10, 1), Instant.now()))
        .expirationDate(new java.sql.Date(System.currentTimeMillis()))
        .build();
    ProductLotStock lot2 = ProductLotStock.builder()
        .code(ProductLotCode.of("test", "lot2"))
        .stockQuantity(9)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 9, 15), Instant.now()))
        .expirationDate(new java.sql.Date(System.currentTimeMillis()))
        .build();
    ProductLotStock lot3 = ProductLotStock.builder()
        .code(ProductLotCode.of("test", "lot3"))
        .stockQuantity(8)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 8, 31), Instant.now()))
        .expirationDate(new java.sql.Date(System.currentTimeMillis()))
        .build();
    ProductLotStock kitLot = ProductLotStock.builder()
        .code(ProductLotCode.of("26A01", null))
        .stockQuantity(10)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 10, 31), Instant.now()))
        .build();
    return asList(lot1, lot2, lot3, kitLot);
  }

}
