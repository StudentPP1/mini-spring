package com.test.demo;

import com.test.annotation.Service;

@Service
public class NoteService {

    public Note createNote(NoteDto noteDto) {
        Note note = new Note();
        note.setId(1L);
        note.setTitle(noteDto.getTitle());
        note.setContent(noteDto.getContent());
        return note;
    }

    public void deleteNote(Long id) {

    }

    public NoteDto updateNote(Note note) {
        return new NoteDto(note.getTitle(), note.getContent());
    }

    public NoteDto getNote(Long id) {
        return new NoteDto("title", "content");
    }
}
