package com.buildingmanager.commonExpenseAllocation;

public class CommonExpenseAllocationMapper {

    public static CommonExpenseAllocationDTO toDTO(CommonExpenseAllocation allocation) {
        return new CommonExpenseAllocationDTO(
                allocation.getId(),
                allocation.getItem().getDescriptionItem(),
                allocation.getAmount(),
                allocation.getIsPaid() != null ? allocation.getIsPaid() : false, // unboxing σε boolean
                allocation.getPaidDate(),
                allocation.getApartment() != null ? allocation.getApartment().getId() : null // apartmentId
        );
    }
}