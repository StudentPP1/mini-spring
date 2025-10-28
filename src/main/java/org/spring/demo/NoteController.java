package org.spring.demo;

import org.spring.annotation.*;

@RestController(path = "/note")
public class NoteController {
    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping(path = "/create")
    public Note createNote(@RequestBody NoteDto note) {
        return noteService.createNote(note);
    }

    @GetMapping(path = "/{id}")
    public NoteDto getNote(@PathVariable("id") Long id) {
        return noteService.getNote(id);
    }

    @DeleteMapping(path = "/{id}")
    public void deleteNote(@PathVariable("id") Long id) {
        noteService.deleteNote(id);
    }

    @PutMapping(path = "/update")
    public NoteDto updateNote(@RequestBody Note note) {
        return noteService.updateNote(note);
    }
}
