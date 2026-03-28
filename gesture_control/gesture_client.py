import cv2
import mediapipe as mp
import time
import socket
import threading

class HandDetector:
    def __init__(self, mode=False, maxHands=2, detectionCon=0.5, trackCon=0.5):
        self.mode = mode
        self.maxHands = maxHands
        self.detectionCon = detectionCon
        self.trackCon = trackCon

        self.mpHands = mp.solutions.hands
        self.hands = self.mpHands.Hands(
            static_image_mode=self.mode,
            max_num_hands=self.maxHands,
            min_detection_confidence=self.detectionCon,
            min_tracking_confidence=self.trackCon
        )
        self.mpDraw = mp.solutions.drawing_utils

    def findHands(self, img, draw=True):
        imgRGB = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        self.results = self.hands.process(imgRGB)
        self.handTypes = []

        if self.results.multi_hand_landmarks:
            if self.results.multi_handedness:
                for handedness in self.results.multi_handedness:
                    self.handTypes.append(handedness.classification[0].label)

            for handLms in self.results.multi_hand_landmarks:
                if draw:
                    self.mpDraw.draw_landmarks(img, handLms, self.mpHands.HAND_CONNECTIONS)

        return img

    def findAllHands(self, img, draw=True):
        hands_data = []
        if self.results.multi_hand_landmarks and self.handTypes:
            for idx, (handLms, handType) in enumerate(zip(self.results.multi_hand_landmarks, self.handTypes)):
                lmList = []
                for id, lm in enumerate(handLms.landmark):
                    h, w, c = img.shape
                    cx, cy = int(lm.x * w), int(lm.y * h)
                    lmList.append([id, cx, cy])
                    if draw and id in [4, 8, 12, 16, 20]:
                        cv2.circle(img, (cx, cy), 10, (0, 255, 0), cv2.FILLED)
                hands_data.append({"handType": handType, "landmarks": lmList})
        return hands_data

    def detectOpenPalm(self, landmarks):
        if len(landmarks) < 21:
            return False
        return sum([
            landmarks[8][2] < landmarks[6][2],
            landmarks[12][2] < landmarks[10][2],
            landmarks[16][2] < landmarks[14][2],
            landmarks[20][2] < landmarks[18][2]
        ]) >= 3

    def detectClosedFist(self, landmarks):
        if len(landmarks) < 21:
            return False
        folded = sum([
            landmarks[8][2] > landmarks[6][2],
            landmarks[12][2] > landmarks[10][2],
            landmarks[16][2] > landmarks[14][2],
            landmarks[20][2] > landmarks[18][2]
        ])
        thumb_folded = self.calculateDistance(landmarks[4], landmarks[0]) < 0.1
        return folded >= 3

    def calculateDistance(self, point1, point2):
        return ((point1[1] - point2[1])**2 + (point1[2] - point2[2])**2)**0.5

class GestureClient:
    def __init__(self, server_ip="localhost", server_port=12345):
        self.server_ip = server_ip
        self.server_port = server_port
        self.detector = HandDetector(detectionCon=0.7, maxHands=2)
        self.cap = None
        self.running = False
        self.client_socket = None
        self.connected = False
        self.last_command = "NONE"
        self.gesture_cooldown = 0.5
        self.last_gesture_time = 0

    def connect_to_game(self):
        try:
            self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.client_socket.connect((self.server_ip, self.server_port))
            print(f"Connected to game server at {self.server_ip}:{self.server_port}")
            self.connected = True
            return True
        except Exception as e:
            print(f"Failed to connect to game server: {e}")
            self.connected = False
            return False

    def start_camera(self, camera_id=0):
        self.cap = cv2.VideoCapture(camera_id)
        if not self.cap.isOpened():
            print("Error: Could not open camera.")
            return False
        return True

    def send_command(self, command):
        if not self.connected or not self.client_socket:
            return False
        try:
            message = f"GESTURE:{command}\n"
            self.client_socket.sendall(message.encode())
            return True
        except Exception as e:
            print(f"Error sending command: {e}")
            self.connected = False
            return False

    def map_gesture_to_command(self, gesture, hand_type):
        if hand_type.lower() == "right":
            if gesture == "open_palm":
                return "ACCELERATE"
            elif gesture == "closed_fist":
                return "BRAKE"
        return "NONE"

    def process_frame(self):
        success, img = self.cap.read()
        if not hasattr(self, 'no_hand_start_time'):
            self.no_hand_start_time = None

        if not success:
            print("Failed to get frame from camera")
            return None

        img = cv2.flip(img, 1)
        img = self.detector.findHands(img)
        hands_data = self.detector.findAllHands(img)

        current_time = time.time()
        command_sent = False
        gesture = "none"
        command = "NONE"
        hand_type = "none"

        if hands_data and (current_time - self.last_gesture_time) > self.gesture_cooldown:
            for hand in hands_data:
                landmarks = hand["landmarks"]
                hand_type = hand["handType"]

                gesture = "unknown"
                if self.detector.detectOpenPalm(landmarks):
                    gesture = "open_palm"
                elif self.detector.detectClosedFist(landmarks):
                    gesture = "closed_fist"

                command = self.map_gesture_to_command(gesture, hand_type)

                if command != "NONE" and command != self.last_command:
                    if self.send_command(command):
                        self.last_command = command
                        self.last_gesture_time = current_time
                        command_sent = True

        # If no hands are detected or no command was sent, explicitly send NONE
        if not hands_data:
            if self.no_hand_start_time is None:
                self.no_hand_start_time = current_time
            elif current_time - self.no_hand_start_time > 1.0:  # 1 second without hands
                if self.last_command != "NONE":
                    if self.send_command("NONE"):
                        self.last_command = "NONE"
        else:
            self.no_hand_start_time = None

        if hands_data:
            cv2.putText(img, f"Gesture: {gesture}", (10, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
            cv2.putText(img, f"Command: {command}", (10, 70),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
            cv2.putText(img, f"Hand: {hand_type}", (10, 110),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)

        status_color = (0, 255, 0) if self.connected else (0, 0, 255)
        status_text = "Connected to game" if self.connected else "Not connected"
        cv2.putText(img, status_text, (10, img.shape[0] - 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, status_color, 2)

        cv2.putText(img, "Controls:", (img.shape[1] - 250, 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
        cv2.putText(img, "Right Open Palm = Accelerate", (img.shape[1] - 250, 60),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 1)
        cv2.putText(img, "Right Closed Fist = Brake", (img.shape[1] - 250, 90),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 1)

        return img

    def run(self):
        if not self.start_camera():
            return

        self.connect_to_game()
        self.running = True
        reconnect_time = 0

        while self.running:
            current_time = time.time()
            if not self.connected and (current_time - reconnect_time > 5):
                self.connect_to_game()
                reconnect_time = current_time

            img = self.process_frame()
            if img is None:
                break

            cv2.imshow("Hill Climb Racing Hand Controller", img)
            if cv2.waitKey(1) & 0xFF == ord('q'):
                self.running = False

        if self.client_socket:
            self.client_socket.close()
        self.cap.release()
        cv2.destroyAllWindows()

def main():
    client = GestureClient()
    client.run()

if __name__ == "__main__":
    main()