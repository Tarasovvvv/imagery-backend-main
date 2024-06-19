package com.imagery.app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@AllArgsConstructor
@Setter
@Getter
public class Card {
    private Image image;
    private ArrayList<String> tags;
    private User user;
}
