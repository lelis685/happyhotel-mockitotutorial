package com.mockitotutorial.happyhotel.booking.service;

import com.mockitotutorial.happyhotel.booking.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

@MockitoSettings
class BookingServiceTest {

    @InjectMocks
    private BookingService bookingService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private RoomService roomService;

    @Spy
    private BookingDAO bookingDAO;

    @Mock
    private MailSender mailSender;

    @Captor
    private ArgumentCaptor<Double> doubleCaptor;


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
                .thenReturn(Collections.singletonList(new Room("Room 1", 2)));
        int expected = 2;

        int actual = bookingService.getAvailablePlaceCount();

        assertEquals(expected, actual);
    }


    @Test
    void should_CountAvailablePlaces_When_MultipleRoomsAvailable() {
        List<Room> rooms = Arrays.asList(new Room("Room 1", 2), new Room("Room 2", 3));
        when(this.roomService.getAvailableRooms())
                .thenReturn(rooms);

        int expected = 5;

        int actual = bookingService.getAvailablePlaceCount();

        assertEquals(expected, actual);
    }


    @Test
    void should_CountAvailablePlaces_When_CalledMultipleTimes() {
        when(this.roomService.getAvailableRooms())
                .thenReturn(Collections.nCopies(10, new Room("Room 1", 2)))
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

        when(this.paymentService.pay(any(), anyDouble()))
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

        when(this.paymentService.pay(any(), eq(400.0)))
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


    @Test
    void should_PayCorrect_When_InputOk() {
        // given
        BookingRequest bookingRequest = new BookingRequest("2",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 5), 2, true);

        //when
        this.bookingService.makeBooking(bookingRequest);

        //then
        verify(paymentService, times(1)).pay(eq(bookingRequest), doubleCaptor.capture());
        double capturedArgument = doubleCaptor.getValue();

        assertEquals(400.0, capturedArgument);
    }


    @Test
    void should_PayCorrect_When_MultipleCalls() {
        // given
        BookingRequest bookingRequest = new BookingRequest("2",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 5), 2, true);
        BookingRequest bookingRequest2 = new BookingRequest("2",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 2), 2, true);
        List<Double> expectedValues = Arrays.asList(400.0, 100.0);

        //when
        this.bookingService.makeBooking(bookingRequest);
        this.bookingService.makeBooking(bookingRequest2);

        //then
        verify(paymentService, times(2)).pay(any(), doubleCaptor.capture());
        List<Double> capturedArgument = doubleCaptor.getAllValues();

        assertEquals(expectedValues, capturedArgument);
    }

    @Test
    void should_BDD_CountAvailablePlaces_When_OneRoomAvailable() {
        given(this.roomService.getAvailableRooms())
                .willReturn(Collections.singletonList(new Room("Room 1", 2)));
        int expected = 2;

        int actual = bookingService.getAvailablePlaceCount();

        assertEquals(expected, actual);
    }

    @Test
    void should_BDD_InvokePayment_When_Prepaid() {
        // given
        BookingRequest bookingRequest = new BookingRequest("2",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 5), 2, true);

        //when
        bookingService.makeBooking(bookingRequest);

        // then
        then(paymentService).should(times(1)).pay(bookingRequest, 400.0);
        verifyNoMoreInteractions(paymentService);
    }


    @Test
    void should_CalculateCorrectPrice() {

        try(MockedStatic<CurrencyConverter> mockedConverter = mockStatic(CurrencyConverter.class)) {
            // given
            BookingRequest bookingRequest = new BookingRequest("2",
                    LocalDate.of(2023, 1, 1),
                    LocalDate.of(2023, 1, 5), 2, true);
            double expected = 400.0;
            mockedConverter.when(() -> CurrencyConverter.toEuro(anyDouble())).thenReturn(400.0);

            //when
            double actual = bookingService.calculatePriceEuro(bookingRequest);

            // then
            assertEquals(expected, actual);
        }
    }



    @Test
    void should_Return80PercentPrice() {

        try(MockedStatic<CurrencyConverter> mockedConverter = mockStatic(CurrencyConverter.class)) {
            // given
            BookingRequest bookingRequest = new BookingRequest("2",
                    LocalDate.of(2023, 1, 1),
                    LocalDate.of(2023, 1, 5), 2, true);
            double expected = 400.0 * 0.8;
            mockedConverter.when(() -> CurrencyConverter.toEuro(anyDouble()))
                    .thenAnswer(inv -> (double) inv.getArgument(0) * 0.8);

            //when
            double actual = bookingService.calculatePriceEuro(bookingRequest);

            // then
            assertEquals(expected, actual);
        }
    }



}