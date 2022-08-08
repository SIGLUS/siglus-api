package org.siglus.siglusapi.domain;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.siglus.common.domain.BaseEntity;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "proofs_of_delivery_extension", schema = "siglusintegration")
public class ProofsOfDeliveryExtension extends BaseEntity {

  @Column(name = "proofofdeliveryid")
  private UUID podId;

  private String preparedBy;

  private String conferredBy;
}
