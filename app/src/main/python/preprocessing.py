import cv2
import numpy as np
from skimage import restoration
import sys
import os
from pathlib import Path
import base64
import io

EPSILON = sys.float_info.epsilon
blur_threshold = 100

class ImageDeblur:
    def __init__(self, psf_size=(5, 5)):
        self.psf = np.ones(psf_size) / np.prod(psf_size)

    def preprocess(self, image):
        image = image / 255.
        image[image >= 1] = 1 - EPSILON
        return image

    def deblur(self, image):
        B = image[:, :, 0]
        G = image[:, :, 1]
        R = image[:, :, 2]
        deconvolved_B = restoration.wiener(B, self.psf, 1, clip=False)
        deconvolved_G = restoration.wiener(G, self.psf, 1, clip=False)
        deconvolved_R = restoration.wiener(R, self.psf, 1, clip=False)
        rgbArray = np.zeros((image.shape[0], image.shape[1], 3))
        rgbArray[..., 0] = deconvolved_B
        rgbArray[..., 1] = deconvolved_G
        rgbArray[..., 2] = deconvolved_R
        return rgbArray

    def lightspace_deblur(self, image):
        left_half_width = image.shape[1] // 2
        left_side = image[:, :left_half_width, :]
        enhanced_left_side = self.f_inverse(left_side)
        enhanced_image = np.copy(image)
        enhanced_image[:, :left_half_width, :] = enhanced_left_side
        enhanced_image[enhanced_image >= 1] = 1 - EPSILON
        return enhanced_image

def process_video(input_video_path):
    cap = cv2.VideoCapture(input_video_path)
    if not cap.isOpened():
        return {"error": "Could not open video file."}

    image_deblur = ImageDeblur()
    processed_frames = []

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break

        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        _, thresholded = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        dark_pixels = np.sum(thresholded == 0)
        dark_percentage = dark_pixels / frame.size
        if dark_percentage > 0.5:  # Threshold for darkness
            preprocessed_frame = image_deblur.preprocess(frame)
            enhanced_frame = image_deblur.lightspace_deblur(preprocessed_frame)
            frame = (enhanced_frame * 255).astype(np.uint8)

        # Encode frame as a base64 string
        _, buffer = cv2.imencode('.jpg', frame)
        frame_str = base64.b64encode(buffer).decode()
        processed_frames.append(frame_str)

    cap.release()
    return {"frames": processed_frames}
