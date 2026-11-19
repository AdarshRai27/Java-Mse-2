package com.college.library.service;

import com.college.library.model.Book;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Business logic and in-memory storage for books.
 *
 * <p>Why LinkedHashMap&lt;Integer, Book&gt;?</p>
 * <ul>
 *     <li>Looking up a book by id is O(1) on average (hash-based).</li>
 *     <li>LinkedHashMap also remembers insertion order, so GET /api/books
 *         returns books in the order they were added.</li>
 *     <li>An AtomicInteger generates ids safely without scanning the map.</li>
 * </ul>
 *
 * <p>The methods are synchronized so two simultaneous HTTP requests cannot
 * modify the map at the same time. In a real application a database would
 * replace this map.</p>
 */
@Service
public class BookService {

    private final Map<Integer, Book> books = new LinkedHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public BookService() {
        // Seed data so GET /api/books works immediately after startup.
        create(new Book(null, "Clean Code", "Robert C. Martin", "Programming", 4));
        create(new Book(null, "The Alchemist", "Paulo Coelho", "Fiction", 2));
        create(new Book(null, "Introduction to Algorithms", "Thomas H. Cormen", "Computer Science", 1));
    }

    /** Returns a copy of the values so callers cannot change the map's structure. */
    public synchronized List<Book> findAll() {
        return new ArrayList<>(books.values());
    }

    public synchronized Optional<Book> findById(Integer id) {
        return Optional.ofNullable(books.get(id));
    }

    /** Validates, assigns a new id, and stores the book. */
    public synchronized Book create(Book book) {
        validate(book);

        int id = nextId.getAndIncrement();
        Book savedBook = new Book(
                id,
                book.getTitle().trim(),
                book.getAuthor().trim(),
                book.getCategory(),
                book.getAvailableCopies());

        books.put(id, savedBook);
        return savedBook;
    }

    /**
     * Updates the four editable fields of an existing book.
     * The id is never changed.
     *
     * @return the updated book, or empty when the id does not exist
     */
    public synchronized Optional<Book> update(Integer id, Book book) {
        if (!books.containsKey(id)) {
            return Optional.empty();
        }

        validate(book);

        Book existingBook = books.get(id);
        existingBook.setTitle(book.getTitle().trim());
        existingBook.setAuthor(book.getAuthor().trim());
        existingBook.setCategory(book.getCategory());
        existingBook.setAvailableCopies(book.getAvailableCopies());

        return Optional.of(existingBook);
    }

    /**
     * Removes a book.
     *
     * @return true when it existed and was removed, false otherwise
     */
    public synchronized boolean delete(Integer id) {
        return books.remove(id) != null;
    }

    /**
     * Validation lives in the service because it is a business rule, not an
     * HTTP concern. The controller turns the exception into a 400 response.
     */
    private void validate(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("Request body must contain a book");
        }
        if (book.getTitle() == null || book.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        if (book.getAuthor() == null || book.getAuthor().isBlank()) {
            throw new IllegalArgumentException("Author must not be blank");
        }
        if (book.getAvailableCopies() == null) {
            throw new IllegalArgumentException("availableCopies is required");
        }
        if (book.getAvailableCopies() < 0) {
            throw new IllegalArgumentException("availableCopies must be 0 or greater");
        }
    }
}
