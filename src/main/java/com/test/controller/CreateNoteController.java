package com.test.controller;

import com.test.entity.Note;
import com.test.handler.Handler;
import com.test.http.HttpRequest;
import com.test.http.HttpResponse;
import com.test.http.HttpStatus;
import com.test.mapper.convertor.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CreateNoteController implements Handler {

    private static final Logger log = LogManager.getLogger(CreateNoteController.class);

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Exception {
        Note note = ObjectMapper.parse(request.getBody(), Note.class);
        log.info("{} get object {}", this.getClass().getName(), note);
        response.setHttpStatus(HttpStatus.CREATED);
        response.setHeader("Content-Type","application/json");
        response.setBody(ObjectMapper.write(note));
    }
}
