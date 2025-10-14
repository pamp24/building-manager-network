package com.buildingmanager.payment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
            p.user.id
        )
        FROM Payment p
        WHERE p.statement.building.id = :buildingId
        ORDER BY p.paymentDate DESC
    """)
    List<PaymentDTO> findRecentByBuilding(@Param("buildingId") Integer buildingId, Pageable pageable);

    @Query("""
SELECT new com.buildingmanager.payment.StatementUserPaymentDTO(
    u.id,
    CONCAT(u.firstName, ' ', u.lastName),
    a.id,
    a.number,
    a.floor,
    SUM(alloc.amount),
    SUM(COALESCE(alloc.paidAmount, 0)),
    MAX(alloc.paidDate),
    alloc.paymentMethod,
    alloc.status
)
FROM CommonExpenseAllocation alloc
LEFT JOIN alloc.apartment a
LEFT JOIN alloc.user u
WHERE alloc.statement.id = :statementId
GROUP BY u.id, a.id, a.number, a.floor, alloc.paymentMethod, alloc.status
ORDER BY a.number
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




}
