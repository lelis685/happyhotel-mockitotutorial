package com.mockitotutorial.happyhotel.booking.service;

import com.mockitotutorial.happyhotel.booking.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.*;

class BookingServiceTest {

    private BookingService bookingService;
    private PaymentService paymentService;
    private RoomService roomService;
    private BookingDAO bookingDAO;
    private MailSender mailSender;

    @BeforeEach
    void setup() {
        this.paymentService = mock(PaymentService.class);
        this.roomService = mock(RoomService.class);
        this.bookingDAO = mock(BookingDAO.class);
        this.mailSender = mock(MailSender.class);

        this.bookingService = new BookingService(paymentService, roomService, bookingDAO, mailSender);
    }


    @Test
    void should_CalculateCorrectPrice_When_CorrectInput() {
        // given
        BookingRequest bookingRequest = new BookingRequest("1",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 5), 2, false);
        double expected = 400;

        // when
        double actual = bookingService.calculatePrice(bookingRequest);

        // then
        assertEquals(expected, actual);
    }


    @Test
    void should_CountAvailablePlaces() {
        int expected = 0;

        int actual = bookingService.getAvailablePlaceCount();

        assertEquals(expected, actual);
    }


    @Test
    void should_CountAvailablePlaces_When_OneRoomAvailable() {
        when(this.roomService.getAvailableRooms())
                .thenReturn(Collections.singletonList(new Room("Room 1",2)));
        int expected = 2;

        int actual = bookingService.getAvailablePlaceCount();

        assertEquals(expected, actual);
    }


    @Test
    void should_CountAvailablePlaces_When_MultipleRoomsAvailable() {
        List<Room> rooms = Arrays.asList(new Room("Room 1",2), new Room("Room 2",3));
        when(this.roomService.getAvailableRooms())
                .thenReturn(rooms);

        int expected = 5;

        int actual = bookingService.getAvailablePlaceCount();

        assertEquals(expected, actual);
    }


    @Test
    void should_CountAvailablePlaces_When_CalledMultipleTimes() {
        when(this.roomService.getAvailableRooms())
                .thenReturn(Collections.nCopies(10, new Room("Room 1",2)))
                .thenReturn(Collections.emptyList());

        int expectedFirstCall = 20;
        int expectedSecondCall = 0;

        int actualFirst = bookingService.getAvailablePlaceCount();
        int actualSecond = bookingService.getAvailablePlaceCount();

        assertAll(
                () -> assertEquals(expectedFirstCall, actualFirst),
                () -> assertEquals(expectedSecondCall, actualSecond)
        );
    }


    @Test
    void should_ThrowException_When_NoRoomAvailable() {
        // given
        BookingRequest bookingRequest = new BookingRequest("1",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 5), 2, false);
        when(this.roomService.findAvailableRoomId(bookingRequest))
                .thenThrow(BusinessException.class);

        //when
        Executable executable = () -> this.bookingService.makeBooking(bookingRequest);

        // then
        assertThrows(BusinessException.class, executable);
    }



    @Test
    void should_NotCompleteBooking_WhenPriceTooHigh() {
        // given
        BookingRequest bookingRequest = new BookingRequest("2",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 5), 2, true);

        when(this.paymentService.pay(any(),anyDouble()))
                .thenThrow(BusinessException.class);

        //when
        Executable executable = () -> this.bookingService.makeBooking(bookingRequest);

        // then
        assertThrows(BusinessException.class, executable);
    }

    @Test
    void should_NotCompleteBooking_WhenPrice400() {
        // given
        BookingRequest bookingRequest = new BookingRequest("2",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 5), 2, true);

        when(this.paymentService.pay(any(),eq(400.0)))
                .thenThrow(BusinessException.class);

        //when
        Executable executable = () -> this.bookingService.makeBooking(bookingRequest);

        // then
        assertThrows(BusinessException.class, executable);
    }

}