package me.w1992wishes.spi;

import java.util.ServiceLoader;

public class mainApp {
    public static void main(String[] args) {
        ServiceLoader<Bird> birds = ServiceLoader.load(Bird.class);
        for (Bird bird : birds) {
            System.out.println(bird.say());
        }
    }
}