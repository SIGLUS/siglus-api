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

package org.siglus.siglusapi.dto;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import lombok.Data;

@NamedNativeQueries({
    @NamedNativeQuery(name = "LocationMovement.getStockMovementWithLocation",
        query = "select\n"
            + "  case\n "
            + "    when scli.sourceid is null\n "
            + "    and scli.destinationid is null\n "
            + "    and scli.reasonid is not null then concat((case\n"
            + "      when sclir.reasontype = 'DEBIT' then '[Ajustes Negativos] '\n"
            + "      when sclir.reasontype = 'CREDIT' then '[Ajustes Positivos] '\n"
            + "      end), sclir.name,"
            + "      (case when scli.reasonfreetext is not null then ': ' end), scli.reasonfreetext)\n"
            + "    when sclir2.name is not null\n"
            + "      then concat((case\n"
            + "      when sclir2.reasontype = 'DEBIT' then '[Ajustes Negativos] '\n"
            + "      when sclir2.reasonType = 'CREDIT' then '[Ajustes Positivos] '\n"
            + "      end), sclir2.name, ': ', scli.reasonfreetext)\n"
            + "    when scli.sourceid is null\n "
            + "    and scli.destinationid is null\n "
            + "    and scli.reasonid is null then 'Inventário físico'\n"
            + "  end as adjustment,\n"
            + "  case\n "
            + "    when scli.sourceid is not null\n"
            + "    and scli.sourcefreetext is null then srcf.name\n"
            + "    when scli.sourcefreetext is not null then 'Outros:' || scli.sourcefreetext\n"
            + "  end as source,\n"
            + "  case\n"
            + "    when scli.destinationid is not null\n"
            + "    and scli.destinationfreetext is null then destf.name\n"
            + "    when scli.destinationfreetext is not null then 'Outros:' || scli.destinationfreetext\n"
            + "  end as destination,\n"
            + "  case\n"
            + "    when scli.sourceid is null\n"
            + "    and scli.destinationid is null\n"
            + "    and scli.reasonid is null then 'INVENTORY'\n"
            + "    when scli.sourceid is not null then 'RECEIVE'\n"
            + "    when scli.destinationid is not null then 'ISSUE'\n"
            + "    else sclir.reasoncategory\n"
            + "  end as reasoncategory,\n"
            + "  sclir.reasontype ,\n"
            + "  sclibl.locationcode ,\n"
            + "  sclibl.area,\n"
            + "  scli.documentnumber ,\n"
            + "  case\n"
            + "    when scli.sourceid is null\n"
            + "    and scli.destinationid is null\n"
            + "    and scli.reasonid is null\n"
            + "    and sclir2.reasontype = 'CREDIT'\n"
            + "    then pilia.quantity\n"
            + "    when scli.sourceid is null\n"
            + "    and scli.destinationid is null\n"
            + "    and scli.reasonid is null\n"
            + "    and sclir2.reasontype = 'DEBIT'\n"
            + "    then -pilia.quantity\n"
            + "    when pilia.id is null and phi.id is not null then 0\n"
            + "    else scli.quantity\n"
            + "  end as quantity ,\n"
            + "  scli.quantity as lineItemQuantity,\n"
            + "  scli.occurreddate ,\n"
            + "  scli.processeddate ,\n"
            + "  scli.signature\n"
            + "from\n"
            + "  stockmanagement.stock_card_line_items scli\n"
            + "join siglusintegration.stock_card_line_items_by_location sclibl on\n"
            + "  scli.id = sclibl.stockcardlineitemid\n"
            + "left join stockmanagement.stock_card_line_item_reasons sclir on\n"
            + "  scli.reasonid = sclir.id\n"
            + "join stockmanagement.stock_cards sc on\n"
            + "  sc.id = scli.stockcardid\n"
            + "left join stockmanagement.nodes dest on\n"
            + "  dest.id = scli.destinationid\n"
            + "left join stockmanagement.nodes src on\n"
            + "  src.id = scli.sourceid\n"
            + "left join referencedata.facilities destf on\n"
            + "  dest.referenceid = destf.id\n"
            + "left join referencedata.facilities srcf on\n"
            + "  src.referenceid = srcf.id\n"
            + "left join stockmanagement.physical_inventories phi on\n"
            + "  phi.stockeventid = scli.origineventid\n"
            + "left join stockmanagement.physical_inventory_line_item_adjustments pilia on\n"
            + "  pilia.stockcardlineitemid = scli.id\n"
            + "left join stockmanagement.stock_card_line_item_reasons sclir2 on\n"
            + "  sclir2.id = pilia.reasonid\n"
            + "left join siglusintegration.physical_inventory_line_items_extension pilie on\n"
            + "    pilie.orderableid = sc.orderableid\n"
            + "  and (case\n"
            + "    when pilie.lotid is null then '28b3c24d-94b3-465e-b751-d24dcaa1cd82'\n"
            + "    else pilie.lotid\n"
            + "  end) = (case\n"
            + "    when sc.lotid is null then '28b3c24d-94b3-465e-b751-d24dcaa1cd82'\n"
            + "    else sc.lotid\n"
            + "  end)\n"
            + "  and pilie.physicalinventoryid = phi.id\n"
            + "  and pilie.locationcode = sclibl.locationcode\n"
            + "where\n"
            + "  scli.stockcardid = :stockCardId\n"
            + "  and sclibl.locationcode = :locationCode\n",
        resultSetMapping = "LocationMovement.LocationMovementDto"),
    @NamedNativeQuery(name = "LocationMovement.getProductLocationMovement",
        query = "select \n"
            + "case\n"
            + "  when sclmli.srclocationcode = :locationCode then 'mover para ' || sclmli.destlocationcode\n"
            + "  when sclmli.destlocationcode = :locationCode then 'mover a partir de ' || sclmli.srclocationcode\n"
            + "end as adjustment,\n"
            + "null as source,\n"
            + "null as destination,\n"
            + "'ADJUSTMENT' as reasoncategory,\n"
            + "case \n"
            + "  when sclmli.srclocationcode = :locationCode then 'DEBIT'\n"
            + "  when sclmli.destlocationcode = :locationCode then 'CREDIT'\n"
            + "end as reasonType,\n"
            + "case \n"
            + "  when sclmli.srclocationcode = :locationCode then sclmli.srclocationcode\n"
            + "  when sclmli.destlocationcode = :locationCode then sclmli.destlocationcode\n"
            + "end as locationcode,\n"
            + "case \n"
            + "  when sclmli.srclocationcode = :locationCode then sclmli.srcarea\n"
            + "  when sclmli.destlocationcode = :locationCode then sclmli.destarea\n"
            + "end as area,\n"
            + "null as documentnumber,\n"
            + "sclmli.quantity ,\n"
            + "sclmli.quantity as lineItemQuantity,\n"
            + "sclmli.occurreddate ,\n"
            + "sclmli.processeddate ,\n"
            + "sclmli.signature\n"
            + "from\n"
            + "siglusintegration.stock_card_location_movement_line_items sclmli\n"
            + "where sclmli.stockcardid = :stockCardId "
            + "and (sclmli.srclocationcode = :locationCode or sclmli.destlocationcode = :locationCode) \n",
        resultSetMapping = "LocationMovement.LocationMovementDto")
})
@MappedSuperclass
@SqlResultSetMappings({
    @SqlResultSetMapping(
        name = "LocationMovement.LocationMovementDto",
        classes = {
            @ConstructorResult(
                targetClass = LocationMovementLineItemDto.class,
                columns = {
                    @ColumnResult(name = "adjustment", type = String.class),
                    @ColumnResult(name = "source", type = String.class),
                    @ColumnResult(name = "destination", type = String.class),
                    @ColumnResult(name = "reasonCategory", type = String.class),
                    @ColumnResult(name = "reasonType", type = String.class),
                    @ColumnResult(name = "locationCode", type = String.class),
                    @ColumnResult(name = "area", type = String.class),
                    @ColumnResult(name = "documentNumber", type = String.class),
                    @ColumnResult(name = "quantity", type = Integer.class),
                    @ColumnResult(name = "lineItemQuantity", type = Integer.class),
                    @ColumnResult(name = "occurredDate", type = LocalDate.class),
                    @ColumnResult(name = "processedDate", type = ZonedDateTime.class),
                    @ColumnResult(name = "signature", type = String.class)
                }
            )
        }
    )
})
@Data
public class LocationMovementLineItemDto {

  private String adjustment;
  private String source;
  private String destination;
  private String reasonCategory;
  private String reasonType;
  private String locationCode;
  private String area;
  private String documentNumber;
  private Integer quantity;
  private Integer lineItemQuantity;
  private LocalDate occurredDate;
  private ZonedDateTime processedDate;
  private String signature;
  private Integer soh;

  public LocationMovementLineItemDto(String adjustment, String source, String destination, String reasonCategory,
      String reasonType, String locationCode, String area, String documentNumber, Integer quantity,
      Integer lineItemQuantity, LocalDate occurredDate, ZonedDateTime processedDate, String signature) {
    this.adjustment = adjustment;
    this.source = source;
    this.destination = destination;
    this.reasonCategory = reasonCategory;
    this.reasonType = reasonType;
    this.locationCode = locationCode;
    this.area = area;
    this.documentNumber = documentNumber;
    this.quantity = quantity;
    this.lineItemQuantity = lineItemQuantity;
    this.occurredDate = occurredDate;
    this.processedDate = processedDate;
    this.signature = signature;
  }

  public LocationMovementLineItemDto() {
  }
}
