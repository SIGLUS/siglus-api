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

package org.siglus.siglusapi.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.siglus.siglusapi.domain.CalculatedStockOnHandByLocation;
import org.siglus.siglusapi.dto.LocationMovementLineItemDto;
import org.siglus.siglusapi.dto.LocationStatusDto;
import org.siglus.siglusapi.dto.LotLocationSohDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface CalculatedStockOnHandByLocationRepository extends JpaRepository<CalculatedStockOnHandByLocation, UUID>,
    JpaSpecificationExecutor<CalculatedStockOnHandByLocation> {

  @Query(value = "select * from siglusintegration.calculated_stocks_on_hand_by_location c "
      + "where c.stockcardid in :stockCardIds", nativeQuery = true)
  List<CalculatedStockOnHandByLocation> findAllByStockCardIds(
      @Param("stockCardIds") Collection<UUID> stockCardIds);

  @Query(name = "LotLocationSoh.findLocationSoh", nativeQuery = true)
  List<LotLocationSohDto> getLocationSoh(
      @Param("lotIds") Collection<UUID> lotIds, @Param("facilityId") UUID facilityId);

  @Query(name = "LotLocationSoh.findLocationSohByStockCard", nativeQuery = true)
  List<LotLocationSohDto> getLocationSohByStockCard(@Param("stockCardId") UUID stockCardId);


  @Query(value = "SELECT * FROM siglusintegration.calculated_stocks_on_hand_by_location c "
      + "JOIN ("
      + "    SELECT c2.stockcardid, c2.locationcode, max_occurred_date, MAX(c2.processeddate) AS max_processed_date "
      + "    FROM ("
      + "        SELECT stockcardid, locationcode, MAX(occurreddate) AS max_occurred_date "
      + "        FROM siglusintegration.calculated_stocks_on_hand_by_location "
      + "        WHERE stockcardid IN (:stockCardIds) "
      + "        GROUP BY stockcardid, locationcode"
      + "    ) AS max_occurred_dates "
      + "    JOIN siglusintegration.calculated_stocks_on_hand_by_location c2 "
      + "    ON c2.stockcardid = max_occurred_dates.stockcardid "
      + "    AND c2.locationcode = max_occurred_dates.locationcode "
      + "    AND c2.occurreddate = max_occurred_dates.max_occurred_date "
      + "    GROUP BY c2.stockcardid, c2.locationcode, max_occurred_date"
      + ") AS latest_records "
      + "ON c.stockcardid = latest_records.stockcardid "
      + "AND c.locationcode = latest_records.locationcode "
      + "AND c.occurreddate = latest_records.max_occurred_date "
      + "AND c.processeddate = latest_records.max_processed_date",
      nativeQuery = true)
  List<CalculatedStockOnHandByLocation> findByStockCardIdIn(
      @Param("stockCardIds") Collection<UUID> stockCardIds);

  @Query(value = " select DISTINCT ON (stockcardid,locationcode) first_value(stockonhand)\n"
      + "                                              OVER (PARTITION BY stockcardid ORDER BY occurreddate DESC )\n"
      + "from siglusintegration.calculated_stocks_on_hand_by_location\n"
      + "where stockcardid = ?1 \n"
      + "and locationcode = ?2 ", nativeQuery = true)
  Optional<Integer> findRecentlySohByStockCardIdAndLocationCode(UUID stockCardId, String locationCode);

  @Query(value = "select distinct on (stockcardid, locationcode) id,\n"
      + "                                               stockcardid,\n"
      + "                                               occurreddate,\n"
      + "                                               calculatedstockonhandid,\n"
      + "                                               locationcode,\n"
      + "                                               area,\n"
      + "                                               first_value(stockonhand)\n"
      + "                 over (partition by (stockcardid, locationcode)\n"
      + " order by occurreddate DESC, processeddate DESC ) as stockonhand\n"
      + "from siglusintegration.calculated_stocks_on_hand_by_location\n"
      + "where stockcardid in (:stockCardIds) ", nativeQuery = true)
  List<CalculatedStockOnHandByLocation> findRecentlyLocationSohByStockCardIds(
      @Param("stockCardIds") Collection<UUID> stockCardIds);


  // TODO performance
  @Query(value = "select * from siglusintegration.calculated_stocks_on_hand_by_location "
      + "where (stockcardid, locationcode, occurreddate) in ("
      + "select stockcardid, locationcode, max(occurreddate) "
      + "from siglusintegration.calculated_stocks_on_hand_by_location c "
      + "where c.stockcardid in :stockCardIds "
      + "and c.occurreddate < :occurredDate "
      + "group by c.stockcardid, c.locationcode)", nativeQuery = true)
  List<CalculatedStockOnHandByLocation> findPreviousLocationStockOnHands(
      @Param("stockCardIds") Collection<UUID> stockCardIds,
      @Param("occurredDate") LocalDate occurredDate);

  @Query(value = "select * from siglusintegration.calculated_stocks_on_hand_by_location "
      + "where (stockcardid, locationcode, occurreddate) in ("
      + "select stockcardid, locationcode, max(occurreddate) "
      + "from siglusintegration.calculated_stocks_on_hand_by_location c "
      + "where c.stockcardid in :stockCardIds "
      + "and c.occurreddate <= :occurredDate "
      + "group by c.stockcardid, c.locationcode)", nativeQuery = true)
  List<CalculatedStockOnHandByLocation> findPreviousLocationStockOnHandsTillNow(
      @Param("stockCardIds") Collection<UUID> stockCardIds,
      @Param("occurredDate") LocalDate occurredDate);

  @Query(value = "select * from siglusintegration.calculated_stocks_on_hand_by_location "
      + "where (stockcardid, locationcode, occurreddate) in ("
      + "select stockcardid, locationcode, max(occurreddate) "
      + "from siglusintegration.calculated_stocks_on_hand_by_location c "
      + "where c.stockcardid in :stockCardIds "
      + "and c.stockonhand > 0 "
      + "and c.occurreddate <= :occurredDate "
      + "group by c.stockcardid, c.locationcode)", nativeQuery = true)
  List<CalculatedStockOnHandByLocation> findPreviousLocationStockOnHandsGreaterThan0(
      @Param("stockCardIds") Collection<UUID> stockCardIds,
      @Param("occurredDate") LocalDate occurredDate);


  @Query(value = "select * from siglusintegration.calculated_stocks_on_hand_by_location\n"
      + "where (stockcardid, processeddate) in\n"
      + "      (select stockcardid, max(processeddate)\n"
      + "       from siglusintegration.calculated_stocks_on_hand_by_location c\n"
      + "       where c.stockcardid in :stockCardIds\n"
      + "       group by c.stockcardid);", nativeQuery = true)
  List<CalculatedStockOnHandByLocation> findLatestLocationSohByStockCardIds(
      @Param("stockCardIds") Collection<UUID> stockCardIds);


  @Modifying
  @Query(value = "delete from siglusintegration.calculated_stocks_on_hand_by_location csohbl "
      + "where csohbl.stockcardid = :stockCardId and csohbl.occurreddate >= :occurredDate "
      + "and csohbl.locationcode = :locationCode",
      nativeQuery = true)
  void deleteAllByStockCardIdAndOccurredDateAndLocationCodes(@Param("stockCardId") UUID stockCardId,
      @Param("occurredDate") Date occurredDate,
      @Param("locationCode") String locationCode);

  @Modifying
  @Query(value = "delete from siglusintegration.calculated_stocks_on_hand_by_location csohbl "
      + "where csohbl.stockcardid = :stockCardId and csohbl.occurreddate = :occurredDate "
      + "and csohbl.locationcode = :locationCode",
      nativeQuery = true)
  void deleteAllByStockCardIdAndEqualOccurredDateAndLocationCodes(@Param("stockCardId") UUID stockCardId,
      @Param("occurredDate") Date occurredDate,
      @Param("locationCode") String locationCode);

  @Modifying
  @Query(value = "delete from siglusintegration.calculated_stocks_on_hand_by_location "
      + "where concat(cast(stockcardid as varchar), concat(cast(occurreddate as varchar), locationcode)) in :pairs",
      nativeQuery = true)
  void deleteAllByStockCardIdAndEqualOccurredDateAndLocationCodePairs(@Param("pairs") Set<String> deletedPairs);

  @Query(name = "LocationMovement.getStockMovementWithLocation", nativeQuery = true)
  List<LocationMovementLineItemDto> getStockMovementWithLocation(@Param("stockCardId") UUID stockCardId,
      @Param("locationCode") String locationCode);

  @Query(name = "LocationMovement.getProductLocationMovement", nativeQuery = true)
  List<LocationMovementLineItemDto> getProductLocationMovement(@Param("stockCardId") UUID stockCardId,
      @Param("locationCode") String locationCode);

  @Query(name = "LocationStatus.findLocationStatusByFacilityId", nativeQuery = true)
  List<LocationStatusDto> findLocationStatusByFacilityId(@Param("facilityId") UUID facilityId);

  void deleteByStockCardIdIn(@Param("stockCardId") Collection<UUID> stockCardId);

  void deleteAllByIdIn(Collection<UUID> ids);

}
