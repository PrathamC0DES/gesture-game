package com.mygdx.hillclimbracing;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Hand Controlled Hill Climb Racing");
        config.setWindowedMode(800, 600);
        config.setForegroundFPS(60);
        config.setIdleFPS(30);

        new Lwjgl3Application(new HillClimbGame(), config);
    }
}
