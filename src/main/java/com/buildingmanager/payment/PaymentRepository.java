package com.buildingmanager.payment;

import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

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
            CONCAT(p.user.firstName, ' ', p.user.lastName),
            p.amount,
            p.paymentDate,
            p.paymentMethod,
            p.referenceNumber,
            p.user.id,
            p.statement.id
        )
        
        FROM Payment p
        WHERE p.statement.building.id = :buildingId
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
    a.floor,
    SUM(COALESCE(alloc.amount, 0)),
    SUM(COALESCE(alloc.paidAmount, 0)),
    MAX(alloc.paidDate),
    COALESCE(alloc.paymentMethod, com.buildingmanager.payment.PaymentMethod.CASH),
    COALESCE(alloc.status, 'PENDING')
)
FROM CommonExpenseAllocation alloc
LEFT JOIN alloc.apartment a
LEFT JOIN alloc.user u
WHERE alloc.statement.id = :statementId
GROUP BY u.id, u.firstName, u.lastName, a.ownerFirstName, a.ownerLastName, a.id, a.number, a.floor, alloc.paymentMethod, alloc.status
ORDER BY a.floor ASC, a.number ASC
""")
    List<StatementUserPaymentDTO> findUserPaymentsByStatement(@Param    ("statementId") Integer statementId);



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
        CONCAT(u.firstName, ' ', u.lastName),
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
    LEFT JOIN Payment p ON p.statement.id = s.id AND p.user.id = u.id
    WHERE s.id = (
        SELECT MAX(s2.id) FROM CommonExpenseStatement s2 WHERE s2.building.id = :buildingId
    )
    GROUP BY u.id, u.firstName, u.lastName, a.id, a.number, a.floor
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
