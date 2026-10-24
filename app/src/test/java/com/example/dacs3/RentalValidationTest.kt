package com.example.dacs3

import org.junit.Assert.*
import org.junit.Test
class RentalValidationTest {
    @Test fun moneyAcceptsVndGroupingAndRejectsMalformedOrOverflowingValues() {
        for(value in listOf("1500000","1.500.000","1,500,000","1 500 000","1.500.000 VNĐ/tháng")) assertEquals(value,1500000L,RentalValidation.money(value))
        for(value in listOf("-1","1.50","1,2,3","1e8","abc","","999999999999999999999","1000000000001")) assertNull(value,RentalValidation.money(value))
        assertEquals(0L,RentalValidation.money("0"))
        assertNull(RentalValidation.total(Long.MAX_VALUE,1));assertNull(RentalValidation.total(-1,100))
        assertEquals(2650000L,RentalValidation.total(2500000,100000,50000))
    }
    @Test fun monthAndBillIdsNormalizeTheSamePeriodWithoutDuplicates() {
        assertEquals("06/2026",RentalValidation.month("6/2026"))
        assertEquals(RentalValidation.billId("room","tenant","06/2026"),RentalValidation.billId("room","tenant","6/2026"))
        for(value in listOf("00/2026","13/2026","06/26","2026-06","06/2026abc")) assertNull(RentalValidation.month(value))
        assertNotEquals(RentalValidation.billId("room","a","6/2026"),RentalValidation.billId("room","b","6/2026"))
    }
    @Test fun coordinatesIncludeEquatorAndPrimeMeridianAndRejectInvalidNumbers() {
        assertTrue(RentalValidation.coordinates(0.0,0.0));assertTrue(RentalValidation.coordinates(-90.0,180.0))
        assertFalse(RentalValidation.coordinates(null,108.0));assertFalse(RentalValidation.coordinates(91.0,0.0))
        assertFalse(RentalValidation.coordinates(Double.NaN,0.0));assertFalse(RentalValidation.coordinates(0.0,Double.POSITIVE_INFINITY))
    }
    @Test fun searchMatchesVietnameseWithoutAccentsAndCombinesBudgetAreaVacancyAndOwnership() {
        val rooms=listOf(
            Property(id="a",hostId="host",title="Phòng cửa sổ",location="Đà Nẵng",monthlyPrice=2000000,area="25",status="Trống"),
            Property(id="b",hostId="other",title="Phòng mới",location="Đà Nẵng",monthlyPrice=3000000,area="30",status="Đã thuê",tenantUid="tenant"),
            Property(id="c",hostId="host",title="Studio",rental="Hà Nội",monthlyPrice=1500000,area="18")
        )
        assertEquals(listOf("a"),RoomFilters(query="da nang cua so",maxPrice=2500000,minArea=20.0,vacantOnly=true).apply(rooms).map { it.id })
        assertEquals(listOf("c","a"),RoomFilters(ownedOnly=true,sort=1).apply(rooms,"host").map { it.id })
        assertTrue(RoomFilters(ownedOnly=true).apply(rooms,null).isEmpty())
        assertEquals(listOf("b","a","c"),RoomFilters(sort=2).apply(rooms).map { it.id })
    }
    @Test fun conversationsAreStableForBothParticipantsAndDifferentBetweenRooms() {
        assertEquals(RentalValidation.conversation("room","host","tenant"),RentalValidation.conversation("room","tenant","host"))
        assertNotEquals(RentalValidation.conversation("one","host","tenant"),RentalValidation.conversation("two","host","tenant"))
        assertThrows(IllegalArgumentException::class.java) { RentalValidation.conversation("room","host","host") }
    }
}
