package org.mars_group.gisimport.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Health {

    @GetMapping("/healthz")
    public ResponseEntity getHealth() {
        return new ResponseEntity(HttpStatus.OK);
    }

}
