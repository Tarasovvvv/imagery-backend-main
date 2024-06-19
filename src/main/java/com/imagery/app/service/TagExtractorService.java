package com.imagery.app.service;

import com.imagery.app.model.Card;
import com.imagery.app.model.CardLocal;
import com.imagery.app.model.Image;
import com.imagery.app.model.ImageLocal;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Array;
import java.sql.SQLException;
import java.util.*;

@Service
public class TagExtractorService {
    public ArrayList<CardLocal> getImagesData(String text) throws SQLException, ClassNotFoundException, IOException {
        final String[] tags = Objects.requireNonNull(new RestTemplateBuilder().build()
                        .getForObject("http://127.0.0.1:8000?text=" + text.toLowerCase(), String.class))
                .replaceAll("\"", "").split(",");
        DataBaseService dataBaseService = new DataBaseService();
        ArrayList<ImageLocal> images = dataBaseService.selectImagesByTagNames(tags);
        ArrayList<CardLocal> response = new ArrayList<>();
        for (ImageLocal image : images) {
            ArrayList<String> sortedTags = dataBaseService.selectTagNamesByImageId(image.getId());
            for (String tag : tags) {
                sortedTags.remove(tag);
                sortedTags.add(0, tag);
            }
            response.add(new CardLocal(
                    image,
                    sortedTags,
                    dataBaseService.selectUserIdNameByImageId(image.getId())));
        }
        return response;
    }

    public ArrayList<ArrayList<String>> getTags(String[] tags) {
        String[] parsedTags = new String[tags.length];
        System.arraycopy(tags, 0, parsedTags, 0, tags.length);
        ArrayList<ArrayList<String>> response = new ArrayList<>();
        for (String imageTags : parsedTags) {
            ArrayList<String> a = new ArrayList<>();
            a.add(Arrays.toString(Objects.requireNonNull(new RestTemplateBuilder().build()
                            .getForObject("http://127.0.0.1:8000?text=" + imageTags.toLowerCase(), String.class))
                    .replaceAll("\"", "").split(",")));
            response.add(a);
        }
        return response;
    }
}
