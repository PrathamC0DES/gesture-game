# 2D Physics-Based Game with Gesture Control

## Description

A 2D interactive game that enables hands-free gameplay using real-time hand gesture recognition instead of traditional keyboard input.

## Features

* Physics-based movement and terrain interaction
* Real-time, contactless gesture control using camera input
* Gesture mapping:

  * Open palm → Accelerate
  * Closed fist → Brake / Reverse
* Dual input support (keyboard + gestures)
* Real-time communication between gesture module and game

## Tech Stack

* Java (LibGDX)
* Python (OpenCV, MediaPipe)

## How It Works

A Python-based gesture detection system captures hand movements using OpenCV and MediaPipe.
Recognized gestures are converted into control signals and sent to the Java-based game via TCP communication, enabling real-time, hands-free interaction.

## Future Improvements

* Improve gesture recognition accuracy
* Add scoring and levels
* Enhance UI/UX
