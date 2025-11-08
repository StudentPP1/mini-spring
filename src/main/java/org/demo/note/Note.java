package org.demo.note;

import lombok.Getter;
import lombok.Setter;
import org.spring.hibernate.annotation.Entity;
import org.spring.hibernate.annotation.Id;
import org.spring.hibernate.annotation.Table;

import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name = "notes")
public class Note implements Serializable {
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
