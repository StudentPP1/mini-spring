package org.spring.demo;

public interface NoteService {
    Note createNote(NoteDto noteDto);

    void deleteNote(Long id);

    NoteDto updateNote(Note note);

    NoteDto getNote(Long id);
}
