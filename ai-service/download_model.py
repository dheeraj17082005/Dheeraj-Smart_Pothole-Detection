#!/usr/bin/env python3
"""Script to download the official peterhdd/pothole-detection-yolov8 ONNX artifact."""

import os
import sys
import urllib.request

MODEL_URL = "https://huggingface.co/peterhdd/pothole-detection-yolov8/resolve/main/best.onnx"
DEST_DIR = os.path.join(os.path.dirname(__file__), "models")
DEST_FILE = os.path.join(DEST_DIR, "pothole_yolov8.onnx")


def download_progress(count, block_size, total_size):
    percent = int(count * block_size * 100 / total_size)
    sys.stdout.write(
        f"\rDownloading ONNX model: {percent}% ({count * block_size // (1024 * 1024)}MB / {total_size // (1024 * 1024)}MB)"
    )
    sys.stdout.flush()


def main():
    os.makedirs(DEST_DIR, exist_ok=True)
    if os.path.exists(DEST_FILE):
        file_size = os.path.getsize(DEST_FILE)
        if file_size > 10 * 1024 * 1024:  # > 10MB
            print(
                f"ONNX model already exists at {DEST_FILE} (size: {file_size / (1024*1024):.2f} MB)."
            )
            return

    print(f"Downloading ONNX model from {MODEL_URL}...")
    urllib.request.urlretrieve(MODEL_URL, DEST_FILE, reporthook=download_progress)
    print(f"\nModel saved successfully to {DEST_FILE}")


if __name__ == "__main__":
    main()
