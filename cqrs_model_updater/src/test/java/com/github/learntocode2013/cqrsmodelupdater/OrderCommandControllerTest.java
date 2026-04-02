package com.github.learntocode2013.cqrsmodelupdater;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderCommandController.class)
public class OrderCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderRepository repository;

    @Test
    void testUpdateOrderCompletesOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setStatus("PENDING");

        when(repository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(repository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String requestBody = "{\"id\":\"" + orderId + "\", \"delivered\": true}";

        mockMvc.perform(put("/orders/" + orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(repository).saveAndFlush(existingOrder);
    }

    @Test
    void testUpdateOrderNoNPEOnMissingDelivered() throws Exception {
        UUID orderId = UUID.randomUUID();
        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setStatus("PENDING");

        when(repository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(repository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Missing "delivered" field
        String requestBody = "{\"id\":\"" + orderId + "\"}";

        mockMvc.perform(put("/orders/" + orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        verify(repository).saveAndFlush(existingOrder);
    }

    @Test
    void testUpdateOrderNotFound() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(repository.findById(orderId)).thenReturn(Optional.empty());

        String requestBody = "{\"id\":\"" + orderId + "\", \"delivered\": true}";

        mockMvc.perform(put("/orders/" + orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isNotFound());
    }
}
