package com.test.demo;

import com.test.http.HttpRequest;
import com.test.http.HttpResponse;
import com.test.http.HttpStatus;
import com.test.mapper.convertor.ObjectMapper;
import com.test.servlet.HttpServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NoteServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger(NoteServlet.class);

    @Override
    protected void doPost(HttpRequest request, HttpResponse response) throws Exception {
        Note note = ObjectMapper.parse(request.getBody(), Note.class);
        log.info("{} get object: {}", this.getClass().getName(), note);
        response.setHttpStatus(HttpStatus.CREATED);
        response.setHeader("Content-Type", "application/json");
        response.setBody(ObjectMapper.write(note));
    }
}
