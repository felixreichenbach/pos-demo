package com.example.posdemo.transaction.api;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.posdemo.transaction.Transaction;
import com.example.posdemo.transaction.TransactionFileLogger;
import com.example.posdemo.transaction.TransactionLogRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
  "spring.autoconfigure.exclude="
    + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
    + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
})
@AutoConfigureMockMvc
class TransactionApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionLogRepository transactionLogRepository;

    @MockBean
    private TransactionFileLogger transactionFileLogger;

    @Test
    void createTransaction_returnsCreatedAndLogsToBothTargets() throws Exception {
        String requestBody = """
                {
                  "cashierId": "cashier-01",
                  "items": [
                    {
                      "productName": "Coffee",
                      "quantity": 2,
                      "unitPrice": 3.50
                    }
                  ],
                  "paymentMethod": "CARD"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("LOGGED"))
                .andExpect(jsonPath("$.transactionId", not(nullValue())))
                .andExpect(jsonPath("$.timestamp", not(nullValue())));

        verify(transactionLogRepository, times(1)).save(any(Transaction.class));
        verify(transactionFileLogger, times(1)).appendTransactionExecuted(any(Transaction.class));

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionLogRepository).save(transactionCaptor.capture());
        assertEquals(new BigDecimal("7.00"), transactionCaptor.getValue().totalAmount());
    }

    @Test
    void createTransaction_withMultipleItems_calculatesTotalAutomatically() throws Exception {
        String requestBody = """
                {
                  "cashierId": "cashier-01",
                  "items": [
                    {
                      "productName": "Coffee",
                      "quantity": 2,
                      "unitPrice": 3.50
                    },
                    {
                      "productName": "Bagel",
                      "quantity": 1,
                      "unitPrice": 2.25
                    }
                  ],
                  "paymentMethod": "CARD"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionLogRepository).save(transactionCaptor.capture());
        assertEquals(new BigDecimal("9.25"), transactionCaptor.getValue().totalAmount());
        verify(transactionFileLogger, times(1)).appendTransactionExecuted(any(Transaction.class));
    }

    @Test
    void createTransaction_whenRepositoryFails_returnsServiceUnavailable() throws Exception {
        doThrow(new RuntimeException("database unavailable"))
                .when(transactionLogRepository)
                .save(any(Transaction.class));

        String requestBody = """
                {
                  "cashierId": "cashier-01",
                  "items": [
                    {
                      "productName": "Coffee",
                      "quantity": 2,
                      "unitPrice": 3.50
                    }
                  ],
                  "paymentMethod": "CARD"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Transaction logging failed"));

        verify(transactionLogRepository, times(1)).save(any(Transaction.class));
        verify(transactionFileLogger, never()).appendTransactionExecuted(any(Transaction.class));
        verify(transactionFileLogger, times(1))
          .appendTransactionFailed(any(Transaction.class), eq("database unavailable"));
    }

      @Test
      void createTransaction_withMissingRequiredFields_returnsBadRequestValidationError() throws Exception {
        String requestBody = """
            {
              "cashierId": "",
              "items": [],
              "paymentMethod": "CARD"
            }
            """;

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid transaction request"));

        verify(transactionLogRepository, never()).save(any(Transaction.class));
        verify(transactionFileLogger, never()).appendTransactionExecuted(any(Transaction.class));
      }
}
