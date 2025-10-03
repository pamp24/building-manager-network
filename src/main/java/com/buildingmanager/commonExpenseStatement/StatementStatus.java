package com.buildingmanager.commonExpenseStatement;

public enum StatementStatus {
    DRAFT,   // Πρόχειρο → δεν έχει εκδοθεί, απλά αποθηκευμένο
    ISSUED,  // Εκδόθηκε και στάλθηκε στους ενοίκους
    PAID, // Όλα τα allocations πληρώθηκαν
    CLOSED   // Ολοκληρώθηκε, δεν μπορεί να αλλάξει άλλο
}
