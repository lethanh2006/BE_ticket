package vn.datve.dat_ve.event;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String venue;

    @Column(name = "image_url")
    private String imageUrl;

    private String category;

    // Cột này do database tự điền (DEFAULT now()), nên Hibernate không được insert/update nó.
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Event() {
        // JPA yêu cầu constructor không tham số
    }

    public Event(String name, String description, String venue, String imageUrl, String category) {
        update(name, description, venue, imageUrl, category);
    }

    public void update(String name, String description, String venue, String imageUrl, String category) {
        this.name = name;
        this.description = description;
        this.venue = venue;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getVenue() { return venue; }
    public String getImageUrl() { return imageUrl; }
    public String getCategory() { return category; }
    public Instant getCreatedAt() { return createdAt; }
}
