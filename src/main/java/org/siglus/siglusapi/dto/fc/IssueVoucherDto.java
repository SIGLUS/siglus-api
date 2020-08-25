package org.siglus.siglusapi.dto.fc;

import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IssueVoucherDto {

  private String requisitionNumber;

  private String supplyOrderNumber;

  private String shippingDate;

  private String clientName;

  private String sourceOfSupplyOrder;

  private Date lastUpdatedAt;

  private String clientCode;

  private String issueVoucherNumber;

  private String warehouseName;

  private String warehouseCode;

  private List<ProductDto> products;

}
