package com.imagery.app.controller;

import com.imagery.app.model.Card;
import com.imagery.app.model.CardLocal;
import com.imagery.app.service.TagExtractorService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class TagExtractorController {
    @GetMapping("/extract-tags")
    public ArrayList<CardLocal> searchImages(
            @RequestParam(name = "text") String text)
            throws SQLException, ClassNotFoundException, IOException {
        return new TagExtractorService().getImagesData(text);
    }
}

