package com.buildingmanager.apartment;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApartmentOrderTest {
    @Test
    void sortsPhysicalFloorsAndNaturalApartmentNumbers() {
        List<Apartment> apartments = new ArrayList<>(List.of(
                apartment(1, "Δώμα", "1"), apartment(2, "Β", "Β10"),
                apartment(3, "Ισόγειο", "2"), apartment(4, "Υπόγειο", "1"),
                apartment(5, "Α", "Α2"), apartment(6, "Ημιυπόγειο", "1"),
                apartment(7, "Ημιόροφος", "1"), apartment(8, "Β", "Β2"),
                apartment(9, "10", "1"), apartment(10, null, null),
                apartment(11, "ΣΤ", "1"), apartment(12, "Α1", "Α1")));
        apartments.sort(ApartmentOrder.COMPARATOR);
        assertEquals(List.of(4, 6, 3, 7, 12, 5, 8, 2, 11, 9, 10, 1),
                apartments.stream().map(Apartment::getId).toList());
    }

    @Test
    void handlesAliasesNumericLabelsAndMissingValues() {
        assertEquals(0, ApartmentOrder.compare("ά", "Α2", "1", "2"));
        assertTrue(ApartmentOrder.compare("1", "2", "1", "10") < 0);
        assertTrue(ApartmentOrder.compare("Υ1", "Υ1", "Ισόγειο", "1") < 0);
        assertTrue(ApartmentOrder.compare("Α", "1", "Α", null) < 0);
        assertTrue(ApartmentOrder.compare("unknown", "1", null, "1") < 0);
        assertTrue(ApartmentOrder.compare("0", "1", "Ημιόροφος", "1") < 0);
        assertEquals(0, ApartmentOrder.compare("Ι", "Ι1", "Ισόγειο", "1"));
        assertTrue(ApartmentOrder.compare("Ι", "Ι1", "Α", "Α1") < 0);
        assertEquals(0, ApartmentOrder.compare("Ι΄", "1", "10", "1"));
        assertTrue(ApartmentOrder.compare("Ημιόροφος", "1", "1", "1") < 0);
        assertTrue(ApartmentOrder.compare("Υ1", "1", "Ημιόροφος", "1") < 0);
        assertEquals(0, ApartmentOrder.compare("Α όροφος", "1", "1", "1"));
        assertEquals(0, ApartmentOrder.compare("Α΄", "1", "1", "1"));
        assertTrue(ApartmentOrder.compare("Α΄", "1", "Β΄", "1") < 0);
        assertEquals(0, ApartmentOrder.compare("Η", "1", "8", "1"));
        assertTrue(ApartmentOrder.compare(null, "1", "Δώμα", "1") < 0);
    }

    private Apartment apartment(int id, String floor, String number) {
        return Apartment.builder().id(id).floor(floor).number(number).build();
    }
}
