package com.college.library.controller;

import com.college.library.model.Book;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Integration-style tests for every endpoint, including failure cases.
 * MockMvc calls the controller without starting a real network server.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllBooksReturnsSeededBooks() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Clean Code"));
    }

    @Test
    void createBookReturns201WithLocationHeader() throws Exception {
        String requestBody = """
                {
                  "title": "Effective Java",
                  "author": "Joshua Bloch",
                  "category": "Programming",
                  "availableCopies": 5
                }
                """;

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/books/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Effective Java"))
                .andExpect(jsonPath("$.author").value("Joshua Bloch"))
                .andExpect(jsonPath("$.availableCopies").value(5));
    }

    @Test
    void createBookRejectsBlankTitle() throws Exception {
        String requestBody = """
                {
                  "title": "   ",
                  "author": "Someone",
                  "category": "Fiction",
                  "availableCopies": 1
                }
                """;

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Title must not be blank"));
    }

    @Test
    void createBookRejectsNegativeAvailableCopies() throws Exception {
        String requestBody = """
                {
                  "title": "A Book",
                  "author": "An Author",
                  "category": "Fiction",
                  "availableCopies": -1
                }
                """;

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("availableCopies must be 0 or greater"));
    }

    @Test
    void getBookByIdReturnsBookWhenItExists() throws Exception {
        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Clean Code"));
    }

    @Test
    void getBookByIdReturns404WhenItDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/books/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Book with id 9999 not found"));
    }

    @Test
    void updateBookReturnsUpdatedBook() throws Exception {
        String requestBody = """
                {
                  "title": "The Alchemist (Updated)",
                  "author": "Paulo Coelho",
                  "category": "Fiction",
                  "availableCopies": 10
                }
                """;

        mockMvc.perform(put("/api/books/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("The Alchemist (Updated)"))
                .andExpect(jsonPath("$.availableCopies").value(10));
    }

    @Test
    void updateBookReturns404WhenItDoesNotExist() throws Exception {
        String requestBody = """
                {
                  "title": "Missing Book",
                  "author": "No One",
                  "category": "Fiction",
                  "availableCopies": 1
                }
                """;

        mockMvc.perform(put("/api/books/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Book with id 9999 not found"));
    }

    @Test
    void updateBookRejectsInvalidData() throws Exception {
        String requestBody = """
                {
                  "title": "Still Valid Title",
                  "author": "   ",
                  "category": "Fiction",
                  "availableCopies": 1
                }
                """;

        mockMvc.perform(put("/api/books/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Author must not be blank"));
    }

    @Test
    void deleteBookReturns204AndRemovesBook() throws Exception {
        int id = createBook("Temporary Book", "Temporary Author", 1);

        mockMvc.perform(delete("/api/books/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBookReturns404WhenItDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/books/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Book with id 9999 not found"));
    }

    /** Helper that creates a book and returns the generated id. */
    private int createBook(String title, String author, int availableCopies) throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new Book(null, title, author, "Test", availableCopies));

        String responseBody = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        return json.get("id").asInt();
    }
}
