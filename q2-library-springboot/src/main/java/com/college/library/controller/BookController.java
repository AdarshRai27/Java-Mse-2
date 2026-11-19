package com.college.library.controller;

import com.college.library.model.Book;
import com.college.library.service.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST endpoints for books.
 *
 * <p>The controller is intentionally thin: it translates HTTP requests into
 * service calls and chooses the HTTP status codes. Business rules live in
 * BookService.</p>
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    // Constructor injection: Spring provides the service when the app starts.
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /** POST /api/books -> 201 Created with the saved book. */
    @PostMapping
    public ResponseEntity<Book> createBook(@RequestBody Book book) {
        Book createdBook = bookService.create(book);
        URI location = URI.create("/api/books/" + createdBook.getId());
        return ResponseEntity.created(location).body(createdBook);
    }

    /** GET /api/books -> 200 OK with all books (an empty list is valid). */
    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks() {
        return ResponseEntity.ok(bookService.findAll());
    }

    /** GET /api/books/{id} -> 200 OK, or 404 Not Found. */
    @GetMapping("/{id}")
    public ResponseEntity<Object> getBookById(@PathVariable Integer id) {
        Optional<Book> book = bookService.findById(id);
        if (book.isEmpty()) {
            return notFound(id);
        }
        return ResponseEntity.ok(book.get());
    }

    /** PUT /api/books/{id} -> 200 OK, 400 Bad Request, or 404 Not Found. */
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateBook(@PathVariable Integer id,
                                             @RequestBody Book book) {
        Optional<Book> updatedBook = bookService.update(id, book);
        if (updatedBook.isEmpty()) {
            return notFound(id);
        }
        return ResponseEntity.ok(updatedBook.get());
    }

    /** DELETE /api/books/{id} -> 204 No Content, or 404 Not Found. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteBook(@PathVariable Integer id) {
        if (!bookService.delete(id)) {
            return notFound(id);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Converts validation failures from the service into a 400 response
     * with a small JSON error body.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidBook(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    private static ResponseEntity<Object> notFound(Integer id) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Book with id " + id + " not found"));
    }
}
