package org.spring.demo;

import lombok.Getter;
import lombok.Setter;
import org.spring.hibernate.annotation.Entity;
import org.spring.hibernate.annotation.Id;

@Setter
@Getter
@Entity
public class Note {
    @Id
    private Long id;
    private String title;
    private String content;

    public Note(String title, String text) {
        this.title = title;
        this.content = text;
    }

    public Note() {

    }

    @Override
    public String toString() {
        return "Note{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
