package org.mars_group.gisimport.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Health {

    @ResponseBody
    @GetMapping("/healthz")
    public ResponseEntity<String> getHealth() {
        return new ResponseEntity<>("I am still alive, so please don't kill me! Pods have feelings too you know...", HttpStatus.OK);
    }

}
