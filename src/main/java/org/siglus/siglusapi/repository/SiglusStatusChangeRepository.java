package org.siglus.siglusapi.repository;

import java.util.UUID;

import org.openlmis.requisition.domain.requisition.StatusChange;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface SiglusStatusChangeRepository
    extends PagingAndSortingRepository<StatusChange, UUID> {

  @Query(value = "select * from requisition.status_changes "
      + "where requisitionid = :requisitionId "
      + "and status = :status", nativeQuery = true)
  StatusChange findByRequisitionIdAndStatus(@Param("requisitionId") UUID requisitionId,
                                            @Param("status") String status);

}
