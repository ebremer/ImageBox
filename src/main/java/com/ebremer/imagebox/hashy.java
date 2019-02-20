/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebremer.imagebox;

import java.util.HashMap;

/**
 *
 * @author erich
 */
public class    hashy {
    
    public static void main(String[] args) {
        HashMap<String, Integer> numbers = new HashMap<>();
        numbers.put("one", 1);
        numbers.put("two", 2);
        numbers.put("three", 3);
        numbers.put("two", 1701);
        System.out.println(numbers.toString());

    }
    
}
