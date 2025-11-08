package org.spring.demo;

import org.spring.annotation.Service;
import org.spring.hibernate.annotation.Transactional;

@Service
public class NoteServiceImpl implements NoteService {
    private final NoteRepository noteRepository;

    public NoteServiceImpl(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @Override
    public Note createNote(NoteDto noteDto) {
        Note note = new Note();
        note.setTitle(noteDto.getTitle());
        note.setContent(noteDto.getContent());
        return noteRepository.save(note);
    }

    @Override
    public void deleteNote(Long id) {
        noteRepository.delete(id);
    }

    @Override
    @Transactional
    public NoteDto updateNote(Note note) {
        noteRepository.update(note);
        Note updatedNote = noteRepository.findById(note.getId()).get();
        return new NoteDto(updatedNote.getTitle(), updatedNote.getContent());
    }

    @Override
    public NoteDto getNote(Long id) {
        Note note = noteRepository.findById(id).get();
        return new NoteDto(note.getTitle(), note.getContent());
    }
}
