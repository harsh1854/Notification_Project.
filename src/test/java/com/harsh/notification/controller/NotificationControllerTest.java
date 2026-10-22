
package com.harsh.notification.controller;

import com.harsh.notification.dto.NotificationRequest;
import com.harsh.notification.dto.NotificationResponse;
import com.harsh.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.mockito.ArgumentCaptor;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;




    @Test
    void shouldReturnBadRequestWhenRecipientIsBlank() throws Exception {

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "recipient": " ",
                                "message": "Hello",
                                "channel": "EMAIL"
                            }
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(notificationService);
    }


    @Test
    void shouldReturnBadRequestWhenMessageIsBlank() throws Exception {

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "recipient": "test@example.com",
                                "message": " ",
                                "channel": "EMAIL"
                            }
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldReturnBadRequestWhenChannelIsMissing() throws Exception {

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "recipient": "test@example.com",
                                "message": "Hello"
                            }
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(notificationService);
    }


    @Test
    void shouldReturnBadRequestWhenRecipientEmailIsInvalid() throws Exception {

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "recipient": "abc",
                                "message": "Hello",
                                "channel": "EMAIL"
                            }
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldReturnSuccessResponse() throws Exception {
        when(notificationService.sendNotification(any(NotificationRequest.class)))
                .thenReturn(new NotificationResponse(
                        true,
                        "Notification accepted for processing",
                        "test@example.com",
                        "EMAIL"
                ));

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "recipient": "test@example.com",
                                "message": "Hello",
                                "channel": "EMAIL"
                            }
                            """))
                .andExpect(status().isOk());

        ArgumentCaptor<NotificationRequest> captor =
                ArgumentCaptor.forClass(NotificationRequest.class);

        verify(notificationService).sendNotification(captor.capture());

        NotificationRequest capturedRequest = captor.getValue();

        org.junit.jupiter.api.Assertions.assertEquals(
                "test@example.com", capturedRequest.getRecipient());

        org.junit.jupiter.api.Assertions.assertEquals(
                "Hello", capturedRequest.getMessage());

        org.junit.jupiter.api.Assertions.assertEquals(
                com.harsh.notification.model.NotificationChannel.EMAIL,
                capturedRequest.getChannel());
    }


}
