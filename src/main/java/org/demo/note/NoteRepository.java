package org.demo.note;

import org.spring.annotation.Repository;
import org.spring.hibernate.repository.BaseRepository;
import org.spring.hibernate.session.Session;

@Repository
public class NoteRepository extends BaseRepository<Note, Long> {
    protected NoteRepository(Class<Note> entityClass, Session session) {
        super(entityClass, session);
    }
}
