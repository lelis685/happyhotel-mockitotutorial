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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
        this.bookingDAO = spy(BookingDAO.class);
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

    @Test
    void should_InvokePayment_When_Prepaid() {
        // given
        BookingRequest bookingRequest = new BookingRequest("2",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 5), 2, true);

        //when
        bookingService.makeBooking(bookingRequest);

        // then
        verify(paymentService, times(1)).pay(bookingRequest, 400.0);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void should_NotInvokePayment_When_NotPrepaid() {
        // given
        BookingRequest bookingRequest = new BookingRequest("2",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 5), 2, false);

        //when
        bookingService.makeBooking(bookingRequest);

        // then
        verify(paymentService, never()).pay(any(), anyDouble());
    }


    @Test
    void should_MakeBooking_When_InputOk() {
        // given
        BookingRequest bookingRequest = new BookingRequest("2",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 5), 2, true);

        //when
        String bookingId = bookingService.makeBooking(bookingRequest);

        // then
        verify(bookingDAO).save(bookingRequest);
        System.out.println("bookingId " + bookingId);
    }

    @Test
    void should_CancelBooking_When_InputOk() {
        // given
        BookingRequest bookingRequest = new BookingRequest("2",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 5), 2, true);
        bookingRequest.setRoomId("1.3");
        String bookingId = "1";

        doReturn(bookingRequest).when(bookingDAO).get(bookingId);

        //when
        bookingService.cancelBooking(bookingId);
    }


    @Test
    void should_ThrowException_WhenMailNotReady() {
        // given
        BookingRequest bookingRequest = new BookingRequest("2",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 5), 2, true);

        doThrow(new BusinessException()).when(mailSender).sendBookingConfirmation(any());

        //when
        Executable executable = () -> this.bookingService.makeBooking(bookingRequest);

        // then
        assertThrows(BusinessException.class, executable);
    }


    @Test
    void should_NotThrowException_WhenMailReady() {
        // given
        BookingRequest bookingRequest = new BookingRequest("2",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 5), 2, true);

        doNothing().when(mailSender).sendBookingConfirmation(any());

        //when
        this.bookingService.makeBooking(bookingRequest);
    }



}