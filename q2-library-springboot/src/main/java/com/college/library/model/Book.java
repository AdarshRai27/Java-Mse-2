package com.college.library.model;

/**
 * Represents one book in the library.
 *
 * <p>The no-argument constructor is needed by Jackson to create an object
 * from incoming JSON. The all-arguments constructor is a convenient way to
 * create complete Book objects in code.</p>
 */
public class Book {

    private Integer id;
    private String title;
    private String author;
    private String category;
    private Integer availableCopies;

    public Book() {
        // Required by Jackson for JSON deserialization.
    }

    public Book(Integer id, String title, String author, String category, Integer availableCopies) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.availableCopies = availableCopies;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(Integer availableCopies) {
        this.availableCopies = availableCopies;
    }
}
