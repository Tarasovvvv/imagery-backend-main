package com.imagery.app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@AllArgsConstructor
@Getter
@Setter
public class CardLocal {
    private ImageLocal image;
    private ArrayList<String> tags;
    private User user;
}
