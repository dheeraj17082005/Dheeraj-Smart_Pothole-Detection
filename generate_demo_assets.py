#!/usr/bin/env python3
import os
import sys
import time
import subprocess
import json
import numpy as np
import cv2
from PIL import Image, ImageDraw, ImageFont
from playwright.sync_api import sync_playwright

OUTPUT_DIR = "/Users/dheerajkumar/Dheeraj-Smart_Pothole-Detection/demo-assets"
TEMP_DIR = "/tmp/potholex_frames"

os.makedirs(OUTPUT_DIR, exist_ok=True)
os.makedirs(TEMP_DIR, exist_ok=True)

CHROME_PATH = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
APP_URL = "http://localhost"

print("--- Step 1: Capturing Live UI Screenshots via Playwright ---")

screenshots = {}

with sync_playwright() as p:
    browser = p.chromium.launch(executable_path=CHROME_PATH, headless=True)
    context = browser.new_context(viewport={"width": 1280, "height": 720})
    page = context.new_page()

    # 1. Login Page
    page.goto(f"{APP_URL}/login")
    page.wait_for_timeout(1000)
    screenshots["01_login"] = os.path.join(TEMP_DIR, "01_login.png")
    page.screenshot(path=screenshots["01_login"])

    # Login as Citizen
    page.fill('input[type="email"]', 'citizen_smoke@test.com')
    page.fill('input[type="password"]', 'Password123!')
    page.click('button[type="submit"]')
    page.wait_for_timeout(1500)

    # 2. Citizen Dashboard
    screenshots["02_citizen_dashboard"] = os.path.join(TEMP_DIR, "02_citizen_dashboard.png")
    page.screenshot(path=screenshots["02_citizen_dashboard"])

    # 3. Report Upload Page
    page.goto(f"{APP_URL}/upload")
    page.wait_for_timeout(1000)
    screenshots["03_upload"] = os.path.join(TEMP_DIR, "03_upload.png")
    page.screenshot(path=screenshots["03_upload"])

    # Upload image and submit
    file_input = page.locator('input[type="file"]')
    img_path = "/Users/dheerajkumar/Dheeraj-Smart_Pothole-Detection/test-data/images/istockphoto-502561495-612x612.jpg"
    file_input.set_input_files(img_path)
    page.fill('input[data-testid="latitude-input"]', '28.6139')
    page.fill('input[data-testid="longitude-input"]', '77.2090')
    page.wait_for_timeout(500)
    page.click('button[data-testid="submit-button"]')
    page.wait_for_timeout(3500)

    # 4. AI Detection Result
    screenshots["04_ai_result"] = os.path.join(TEMP_DIR, "04_ai_result.png")
    page.screenshot(path=screenshots["04_ai_result"])

    # 5. Interactive Map Page
    page.goto(f"{APP_URL}/map")
    page.wait_for_timeout(2000)
    screenshots["05_map"] = os.path.join(TEMP_DIR, "05_map.png")
    page.screenshot(path=screenshots["05_map"])

    # 6. Potholes List Page
    page.goto(f"{APP_URL}/potholes")
    page.wait_for_timeout(1500)
    screenshots["06_list"] = os.path.join(TEMP_DIR, "06_list.png")
    page.screenshot(path=screenshots["06_list"])

    # 7. Logout citizen and Login as Officer
    page.click('button:has-text("Sign Out")')
    page.wait_for_timeout(1000)
    page.goto(f"{APP_URL}/login")
    page.fill('input[type="email"]', 'officer_smoke@test.com')
    page.fill('input[type="password"]', 'Password123!')
    page.click('button[type="submit"]')
    page.wait_for_timeout(2000)

    # 8. Officer Dashboard
    screenshots["07_officer_dashboard"] = os.path.join(TEMP_DIR, "07_officer_dashboard.png")
    page.screenshot(path=screenshots["07_officer_dashboard"])

    # 9. Logout officer and re-login citizen to capture notifications
    page.click('button:has-text("Sign Out")')
    page.wait_for_timeout(1000)
    page.goto(f"{APP_URL}/login")
    page.fill('input[type="email"]', 'citizen_smoke@test.com')
    page.fill('input[type="password"]', 'Password123!')
    page.click('button[type="submit"]')
    page.wait_for_timeout(1500)

    # Open notification popover
    page.click('button:has-text("🔔")')
    page.wait_for_timeout(1000)
    screenshots["08_notifications"] = os.path.join(TEMP_DIR, "08_notifications.png")
    page.screenshot(path=screenshots["08_notifications"])

    browser.close()

print("Captured 8 real UI screenshots successfully!")

# Helper functions for overlay styling
def get_font(size=24, bold=False):
    try:
        font_path = "/System/Library/Fonts/Helvetica.ttc" if bold else "/System/Library/Fonts/Helvetica.ttc"
        return ImageFont.truetype(font_path, size)
    except:
        return ImageFont.load_default()

def draw_header_banner(img, title, subtitle):
    pil_img = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
    draw = ImageDraw.Draw(pil_img)
    w, h = pil_img.size

    # Draw dark translucent overlay banner at top
    overlay = Image.new("RGBA", (w, 60), (11, 13, 15, 230))
    pil_img.paste(overlay, (0, 0), overlay)

    draw = ImageDraw.Draw(pil_img)
    font_title = get_font(20, bold=True)
    font_sub = get_font(13, bold=False)

    draw.text((20, 10), title, fill=(214, 245, 61), font=font_title)
    draw.text((20, 36), subtitle, fill=(160, 174, 192), font=font_sub)

    return cv2.cvtColor(np.array(pil_img), cv2.COLOR_RGB2BGR)

def draw_subtitle_footer(img, subtitle_text):
    pil_img = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
    draw = ImageDraw.Draw(pil_img)
    w, h = pil_img.size

    # Subtitle bar at bottom
    overlay = Image.new("RGBA", (w, 45), (15, 23, 42, 230))
    pil_img.paste(overlay, (0, h - 45), overlay)

    draw = ImageDraw.Draw(pil_img)
    font_sub = get_font(15, bold=True)
    
    bbox = font_sub.getbbox(subtitle_text) if hasattr(font_sub, 'getbbox') else (0, 0, 400, 20)
    tw = bbox[2] - bbox[0]
    draw.text(((w - tw) // 2, h - 33), subtitle_text, fill=(255, 255, 255), font=font_sub)

    return cv2.cvtColor(np.array(pil_img), cv2.COLOR_RGB2BGR)

# Create Architecture Final Frame
def create_architecture_frame(width=1280, height=720):
    img = Image.new("RGB", (width, height), (11, 13, 15))
    draw = ImageDraw.Draw(img)

    title_font = get_font(38, bold=True)
    sub_font = get_font(20, bold=False)
    card_title_font = get_font(18, bold=True)
    card_sub_font = get_font(14, bold=False)

    # Title Banner
    draw.text((width // 2 - 120, 40), "PotholeX", fill=(255, 255, 255), font=title_font)
    draw.text((width // 2 + 55, 40), "AI", fill=(214, 245, 61), font=title_font)
    draw.text((width // 2 - 280, 90), "Production-Ready Road Telematics & Defect Remediation Platform", fill=(148, 163, 184), font=sub_font)

    # 4 Pipeline Stage Cards
    stages = [
        ("1. Citizen Evidence", "Image & Video Ingestion\nGPS Spatial Coordinates", (59, 130, 246)),
        ("2. AI Inference Engine", "YOLOv8 ONNX Model\nBounding Box & Severity", (214, 245, 61)),
        ("3. PostGIS Jurisdiction", "Spatial Boundary Resolution\nProximity Deduplication", (16, 185, 129)),
        ("4. Officer Operations", "Verified Review Queue\nCanonical State Machine", (168, 85, 247))
    ]

    card_w, card_h = 260, 160
    start_x = (width - (4 * card_w + 3 * 30)) // 2
    y_pos = 200

    for i, (stitle, sdesc, color) in enumerate(stages):
        x = start_x + i * (card_w + 30)
        draw.rectangle([x, y_pos, x + card_w, y_pos + card_h], fill=(23, 29, 38), outline=color, width=2)
        draw.text((x + 15, y_pos + 20), stitle, fill=color, font=card_title_font)
        
        lines = sdesc.split('\n')
        draw.text((x + 15, y_pos + 60), lines[0], fill=(226, 232, 240), font=card_sub_font)
        draw.text((x + 15, y_pos + 85), lines[1], fill=(148, 163, 184), font=card_sub_font)

    # Tech Stack Footer Card
    draw.rectangle([start_x, 420, width - start_x, 620], fill=(23, 29, 38), outline=(51, 65, 85), width=2)
    draw.text((start_x + 30, 440), "FULL-STACK PRODUCTION ARCHITECTURE", fill=(214, 245, 61), font=card_title_font)

    stack_items = [
        ("Frontend", "React 18 + TypeScript + Vite + Leaflet GIS"),
        ("Backend", "Spring Boot 3.2 + Spring Security JWT + Hibernate"),
        ("AI Microservice", "FastAPI + ONNX Runtime + OpenCV + PyResearch YOLOv8"),
        ("Spatial Database", "PostgreSQL 16 + PostGIS Spatial Index (ST_DWithin, ST_Contains)"),
        ("Object Storage", "MinIO S3 (pothole-raw & pothole-annotated buckets)"),
        ("Orchestration", "Docker Compose Multi-Container Production Environment")
    ]

    for idx, (label, val) in enumerate(stack_items):
        col = idx % 2
        row = idx // 2
        px = start_x + 30 + col * 580
        py = 480 + row * 45
        draw.text((px, py), f"{label}:", fill=(255, 255, 255), font=get_font(14, bold=True))
        draw.text((px + 140, py), val, fill=(148, 163, 184), font=get_font(14, bold=False))

    return cv2.cvtColor(np.array(img), cv2.COLOR_RGB2BGR)

print("--- Step 2: Composing Demo Video Frames ---")

# Define 60s Video Scenes
# 30 fps * 60 sec = 1800 frames
fps = 30
scenes = [
    ("01_login", "0:00 – 0:05", "PotholeX Road Portal — Citizen & Municipal Officer Gateway", "Citizens submit road defect evidence; verified officers manage spatial jurisdictions.", 5),
    ("02_citizen_dashboard", "0:05 – 0:15", "Citizen Dashboard — My Road Reports & Overview", "Citizens track report status, active hazards, and nearby community dispatches.", 10),
    ("03_upload", "0:15 – 0:25", "Evidence Ingestion & AI Detection Engine", "Submitting defect image with GPS coordinates triggers YOLOv8 ONNX model inference.", 10),
    ("04_ai_result", "0:15 – 0:25", "YOLOv8 AI Inference & Visual Severity Scoring", "AI detects bounding boxes, calculates visual severity score (HIGH), and resolves NDMC jurisdiction.", 10),
    ("05_map", "0:25 – 0:35", "Interactive GIS Defect Map & Spatial Telematics", "PostGIS spatial queries filter defect markers dynamically across municipal road networks.", 10),
    ("07_officer_dashboard", "0:35 – 0:45", "Verified Officer Workspace & Spatial Review Queue", "Verified officers review incoming reports within assigned spatial jurisdiction.", 10),
    ("08_notifications", "0:45 – 0:55", "Real-Time Milestone Notifications & Status Progress", "Citizens receive persistent in-app notifications as reports progress through RESOLVED status.", 10),
]

video_frames_path = os.path.join(TEMP_DIR, "video_frames")
os.makedirs(video_frames_path, exist_ok=True)

frame_counter = 0

for key, timecode, header_title, footer_sub, duration_sec in scenes:
    img_path = screenshots[key]
    base_img = cv2.imread(img_path)
    base_img = cv2.resize(base_img, (1280, 720))

    annotated = draw_header_banner(base_img, f"{header_title} ({timecode})", "PotholeX Production Application")
    annotated = draw_subtitle_footer(annotated, footer_sub)

    num_frames = duration_sec * fps
    for f in range(num_frames):
        out_f = os.path.join(video_frames_path, f"frame_{frame_counter:05d}.png")
        cv2.imwrite(out_f, annotated)
        frame_counter += 1

# Add final 5 seconds Architecture slide (55s - 60s)
arch_img = create_architecture_frame(1280, 720)
arch_annotated = draw_header_banner(arch_img, "PotholeX Full-Stack Technical Architecture (0:55 – 1:00)", "Production-Ready AI & Spatial Telematics Road Platform")
arch_annotated = draw_subtitle_footer(arch_annotated, "React 18 | Spring Boot 3.2 | FastAPI | PostgreSQL/PostGIS | MinIO S3 | Docker")

for f in range(5 * fps):
    out_f = os.path.join(video_frames_path, f"frame_{frame_counter:05d}.png")
    cv2.imwrite(out_f, arch_annotated)
    frame_counter += 1

print(f"Total video frames generated: {frame_counter}")

# Encode MP4 using FFmpeg
mp4_output = os.path.join(OUTPUT_DIR, "potholex-demo.mp4")
print(f"--- Step 3: Encoding MP4 Video to {mp4_output} ---")

ffmpeg_cmd = [
    "/opt/homebrew/bin/ffmpeg", "-y",
    "-framerate", str(fps),
    "-i", os.path.join(video_frames_path, "frame_%05d.png"),
    "-c:v", "libx264",
    "-pix_fmt", "yuv420p",
    "-crf", "18",
    mp4_output
]

subprocess.run(ffmpeg_cmd, check=True)
print("MP4 Video generated successfully!")

# Compose 12-Frame Looping GIF
gif_output = os.path.join(OUTPUT_DIR, "potholex-workflow.gif")
print(f"--- Step 4: Composing 12-Frame Looping GIF to {gif_output} ---")

gif_frames = []
# Create 12 distinct workflow storyboard frames
storyboard_frames = [
    ("Frame 1: PotholeX Platform", "Citizen & Officer Civic Maintenance Portal", "01_login", (214, 245, 61)),
    ("Frame 2: Citizen Evidence Upload", "Capture & Submit Defect Media + GPS Coordinates", "03_upload", (59, 130, 246)),
    ("Frame 3: YOLOv8 AI Detection", "AI Bounding Box Detection & Severity Calculation", "04_ai_result", (214, 245, 61)),
    ("Frame 4: PostGIS Jurisdiction", "Spatial Boundary Resolution & Authority Mapping", "05_map", (16, 185, 129)),
    ("Frame 5: Proximity Deduplication", "15m Spatial Radius & 30-Day Resolution Protection", "06_list", (245, 158, 11)),
    ("Frame 6: Officer Notification", "Real-time Notification Delivered to Jurisdiction Officer", "08_notifications", (168, 85, 247)),
    ("Frame 7: Officer Review Queue", "Verified Officer Workspace & Spatial Review Queue", "07_officer_dashboard", (59, 130, 246)),
    ("Frame 8: Officer Decision", "Official Report Acceptance & Audit Trail Logging", "07_officer_dashboard", (16, 185, 129)),
    ("Frame 9: Work Dispatched", "Remediation Status Transitioned to IN_PROGRESS", "07_officer_dashboard", (59, 130, 246)),
    ("Frame 10: Defect Resolved", "Remediation Complete & Status Marked RESOLVED", "07_officer_dashboard", (16, 185, 129)),
    ("Frame 11: Citizen Notified", "Real-Time Notification Delivered to Citizen Creator", "08_notifications", (214, 245, 61)),
    ("Frame 12: Architecture Overview", "React • Spring Boot • FastAPI • PostGIS • MinIO • Docker", "ARCH", (59, 130, 246))
]

for title, sub, key, color in storyboard_frames:
    if key == "ARCH":
        base = create_architecture_frame(1280, 720)
    else:
        base = cv2.imread(screenshots[key])
        base = cv2.resize(base, (1280, 720))

    annotated = draw_header_banner(base, title, sub)
    annotated = draw_subtitle_footer(annotated, "PotholeX End-to-End Workflow Loop")

    # Convert to PIL Image
    pil_f = Image.fromarray(cv2.cvtColor(annotated, cv2.COLOR_BGR2RGB))
    gif_frames.append(pil_f)

# Save looping GIF (duration per frame = 900ms)
gif_frames[0].save(
    gif_output,
    save_all=True,
    append_images=gif_frames[1:],
    duration=900,
    loop=0,
    optimize=True
)

print(f"GIF Workflow generated successfully at {gif_output}!")

print("\n=== STEP 60 ASSET GENERATION COMPLETE ===")
print(f"1. Video: {mp4_output} ({os.path.getsize(mp4_output)} bytes)")
print(f"2. GIF:   {gif_output} ({os.path.getsize(gif_output)} bytes)")
print("3. Script: docs/DEMO_VIDEO_SCRIPT.md")
print("4. Storyboard: docs/WORKFLOW_GIF_STORYBOARD.md")
