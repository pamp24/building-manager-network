package com.buildingmanager.payment;

import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Optional<Payment> findTopByApartment_IdAndStatement_IdOrderByPaymentDateDesc(Integer apartmentId, Integer statementId);


    Payment findTopByUser_IdAndStatement_IdOrderByPaymentDateDesc(Integer userId, Integer statementId);
    Payment findTopByStatement_IdAndStatement_Building_IdOrderByPaymentDateDesc(Integer statementId, Integer buildingId);

    @Query("""
    SELECT p
    FROM Payment p
    JOIN p.statement s
    WHERE s.building.id = :buildingId
      AND s.startDate BETWEEN :startOfMonth AND :endOfMonth
""")
    List<Payment> findByBuildingIdAndMonth(@Param("buildingId") Long buildingId,
                                           @Param("startOfMonth") LocalDateTime startOfMonth,
                                           @Param("endOfMonth") LocalDateTime endOfMonth);

    @Query("""
    SELECT new com.buildingmanager.payment.PaymentDTO(
        p.id,
        CONCAT(p.user.firstName, ' ', p.user.lastName),
        p.amount,
        p.paymentDate,
        p.paymentMethod,
        p.referenceNumber,
        p.user.id
    )
    FROM Payment p
    WHERE p.statement.id = :statementId
    ORDER BY p.paymentDate DESC
""")
    List<PaymentDTO> findPaymentsByStatementId(@Param("statementId") Integer statementId, Pageable pageable);

    @Query("""
SELECT new com.buildingmanager.payment.PaymentDTO(
    p.id,
    COALESCE(
        CASE WHEN u.id IS NOT NULL THEN CONCAT(u.firstName, ' ', u.lastName) END,
        CASE
            WHEN a.id IS NOT NULL AND a.isRented = TRUE THEN CONCAT(a.residentFirstName, ' ', a.residentLastName)
            WHEN a.id IS NOT NULL THEN CONCAT(a.ownerFirstName, ' ', a.ownerLastName)
            ELSE CONCAT('Διαμέρισμα ', COALESCE(a.number, '—'))
        END,
        '—'
    ),
    p.amount,
    p.paymentDate,
    p.paymentMethod,
    p.referenceNumber,
    u.id,
    s.id
)
FROM Payment p
LEFT JOIN p.user u
LEFT JOIN p.apartment a
LEFT JOIN p.statement s
LEFT JOIN a.building ab
LEFT JOIN s.building sb
WHERE (ab.id = :buildingId OR sb.id = :buildingId)
ORDER BY p.paymentDate DESC
""")
    List<PaymentDTO> findRecentByBuilding(@Param("buildingId") Integer buildingId, Pageable pageable);












    @Query("""
SELECT new com.buildingmanager.payment.StatementUserPaymentDTO(
    u.id,
    COALESCE(u.firstName, a.ownerFirstName, '—'),
    COALESCE(u.lastName, a.ownerLastName, 'Χωρίς χρήστη'),
    a.id,
    a.number,
    CAST(a.floor AS string),
    SUM(COALESCE(alloc.amount, 0)),
    SUM(COALESCE(alloc.paidAmount, 0)),
    MAX(alloc.paidDate),
    MAX(alloc.paymentMethod),
    CASE
        WHEN SUM(COALESCE(alloc.paidAmount, 0)) >= SUM(COALESCE(alloc.amount, 0)) THEN 'PAID'
        WHEN SUM(COALESCE(alloc.paidAmount, 0)) > 0 THEN 'PARTIALLY_PAID'
        ELSE 'UNPAID'
    END
)
FROM CommonExpenseAllocation alloc
LEFT JOIN alloc.apartment a
LEFT JOIN alloc.user u
WHERE alloc.statement.id = :statementId
GROUP BY u.id, u.firstName, u.lastName, a.ownerFirstName, a.ownerLastName, a.id, a.number, a.floor
ORDER BY a.floor ASC, a.number ASC
""")
    List<StatementUserPaymentDTO> findUserPaymentsByStatement(@Param("statementId") Integer statementId);



    @Query("""
    SELECT new com.buildingmanager.payment.CommonStatementSummaryDTO(
        SUM(alloc.amount),
        SUM(alloc.paidAmount),
        (SUM(alloc.amount) - SUM(alloc.paidAmount)),
        CASE 
            WHEN SUM(alloc.amount) > 0 THEN (SUM(alloc.paidAmount) / SUM(alloc.amount)) * 100 
            ELSE 0 
        END,
        MAX(s.dueDate)
    )
    FROM CommonExpenseAllocation alloc
    JOIN alloc.statement s
    WHERE s.building.id = :buildingId
""")
    CommonStatementSummaryDTO findBuildingSummary(@Param("buildingId") Integer buildingId);

    @Query("""
SELECT new com.buildingmanager.payment.StatementUserPaymentDTO(
    u.id,
    COALESCE(u.firstName, a.ownerFirstName, '—'),
    COALESCE(u.lastName, a.ownerLastName, 'Χωρίς χρήστη'),
    a.id,
    a.number,
    CAST(a.floor AS string),
    SUM(COALESCE(cea.amount, 0)),
    SUM(COALESCE(cea.paidAmount, 0)),
    MAX(cea.paidDate),
    MAX(cea.paymentMethod),
    CASE
        WHEN SUM(COALESCE(cea.paidAmount, 0)) >= SUM(COALESCE(cea.amount, 0)) THEN 'PAID'
        WHEN SUM(COALESCE(cea.paidAmount, 0)) > 0 THEN 'PARTIALLY_PAID'
        ELSE 'PENDING'
    END
)
FROM CommonExpenseStatement s
JOIN CommonExpenseAllocation cea ON cea.statement.id = s.id
JOIN cea.apartment a
LEFT JOIN cea.user u
WHERE s.id = (
    SELECT MAX(s2.id)
    FROM CommonExpenseStatement s2
    WHERE s2.building.id = :buildingId
)
GROUP BY u.id, u.firstName, u.lastName, a.ownerFirstName, a.ownerLastName, a.id, a.number, a.floor
ORDER BY a.floor, a.number
""")
    List<StatementUserPaymentDTO> findUserPaymentsByLastStatement(@Param("buildingId") Integer buildingId);




    @Query("""
SELECT new com.buildingmanager.payment.StatementUserPaymentDTO(
    u.id,
    COALESCE(u.firstName, a.ownerFirstName, '—'),
    COALESCE(u.lastName, a.ownerLastName, 'Χωρίς χρήστη'),
    a.id,
    a.number,
    CAST(a.floor AS string),
    SUM(COALESCE(cea.amount, 0)),
    SUM(COALESCE(cea.paidAmount, 0)),
    MAX(cea.paidDate),
    MAX(cea.paymentMethod),
    CASE
        WHEN SUM(COALESCE(cea.paidAmount, 0)) >= SUM(COALESCE(cea.amount, 0)) THEN 'PAID'
        WHEN SUM(COALESCE(cea.paidAmount, 0)) > 0 THEN 'PARTIALLY_PAID'
        ELSE 'PENDING'
    END
)
FROM CommonExpenseStatement s
LEFT JOIN CommonExpenseAllocation cea ON cea.statement.id = s.id
LEFT JOIN cea.apartment a
LEFT JOIN cea.user u
WHERE s.building.id = :buildingId
  AND (
    (s.startDate BETWEEN :startOfMonth AND :endOfMonth)
    OR (s.endDate BETWEEN :startOfMonth AND :endOfMonth)
    OR (:startOfMonth BETWEEN s.startDate AND s.endDate)
  )
GROUP BY u.id, u.firstName, u.lastName, a.ownerFirstName, a.ownerLastName, a.id, a.number, a.floor
ORDER BY a.floor, a.number
""")
    List<StatementUserPaymentDTO> findUserPaymentsByBuildingAndCurrentMonth(
            @Param("buildingId") Integer buildingId,
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth
    );






}
