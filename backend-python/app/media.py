"""Multimodal media preprocessing — image resize/compress/base64 and video frame extraction."""

from __future__ import annotations

import base64
import io
import logging
import uuid
from pathlib import Path
from typing import NamedTuple

from PIL import Image

from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()


class ProcessedMedia(NamedTuple):
    """Result of preprocessing a single uploaded file."""
    original_name: str
    saved_path: str          # path on disk
    mime_type: str
    base64_data_url: str     # "data:image/jpeg;base64,..."
    width: int
    height: int
    file_size_bytes: int


class MediaType(NamedTuple):
    is_image: bool
    is_video: bool


_VALID_IMAGE_MIME = frozenset(
    t.strip() for t in settings.media_allowed_image_types.split(",") if t.strip()
)
_VALID_VIDEO_MIME = frozenset({"video/mp4", "video/webm", "video/quicktime", "video/x-msvideo"})

MAX_SIZE_BYTES = settings.max_image_size_mb * 1024 * 1024
MAX_SIDE = 2048
JPEG_QUALITY = 80


def classify_media(mime_type: str) -> MediaType:
    """Classify MIME type as image/video/other."""
    mime = mime_type.lower().split(";")[0].strip()
    return MediaType(
        is_image=mime in _VALID_IMAGE_MIME,
        is_video=mime in _VALID_VIDEO_MIME,
    )


def preprocess_image(
    file_bytes: bytes,
    original_filename: str,
    mime_type: str,
) -> ProcessedMedia:
    """Resize, compress and base64-encode an uploaded image.

    Returns a ``ProcessedMedia`` ready to be passed to the multimodal LLM.
    """
    mime = mime_type.lower().split(";")[0].strip()
    fmt = mime.split("/")[-1] if "/" in mime else "jpeg"
    # Normalise format name for PIL
    if fmt == "jpeg":
        pil_fmt = "JPEG"
    elif fmt in ("png", "gif", "webp"):
        pil_fmt = "PNG" if fmt == "png" else ("GIF" if fmt == "gif" else "WEBP")
    else:
        pil_fmt = "JPEG"
        fmt = "jpeg"

    # Open and optionally resize
    img = Image.open(io.BytesIO(file_bytes))
    img = img.convert("RGB")  # drop alpha for JPEG safety
    width, height = img.size
    if max(width, height) > MAX_SIDE:
        ratio = MAX_SIDE / max(width, height)
        new_size = (int(width * ratio), int(height * ratio))
        img = img.resize(new_size, Image.Resampling.LANCZOS)
        width, height = new_size

    # Encode to JPEG (universally supported, smaller)
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=JPEG_QUALITY, optimize=True)
    compressed = buf.getvalue()

    # Save to disk
    saved_name = f"{uuid.uuid4().hex}.jpg"
    save_dir = Path(settings.media_upload_dir)
    save_dir.mkdir(parents=True, exist_ok=True)
    saved_path = save_dir / saved_name
    saved_path.write_bytes(compressed)

    b64 = base64.b64encode(compressed).decode("ascii")
    data_url = f"data:image/jpeg;base64,{b64}"

    logger.info(
        "Preprocessed image %s → %s (%dx%d, %d bytes)",
        original_filename,
        saved_name,
        width,
        height,
        len(compressed),
    )
    return ProcessedMedia(
        original_name=original_filename,
        saved_path=str(saved_path),
        mime_type="image/jpeg",
        base64_data_url=data_url,
        width=width,
        height=height,
        file_size_bytes=len(compressed),
    )


def extract_video_frames(
    file_bytes: bytes,
    original_filename: str,
    max_frames: int = 8,
) -> list[ProcessedMedia]:
    """Extract key frames from a video file and return them as ``ProcessedMedia``.

    Tries ffmpeg first (lighter, no numpy dependency), then OpenCV (cv2),
    and falls back to a placeholder if neither is available.
    """
    # 1) Try ffmpeg
    frames = _extract_frames_ffmpeg(file_bytes, original_filename, max_frames)
    if frames:
        return frames

    # 2) Try OpenCV
    frames = _extract_frames_cv2(file_bytes, original_filename, max_frames)
    if frames:
        return frames

    # 3) Placeholder
    logger.warning("Neither ffmpeg nor opencv-python available – returning placeholder for video")
    return [_video_placeholder(original_filename)]


def _extract_frames_ffmpeg(
    file_bytes: bytes,
    original_filename: str,
    max_frames: int = 8,
) -> list[ProcessedMedia] | None:
    """Use ffmpeg CLI to extract frames. Returns None if ffmpeg is unavailable."""
    import subprocess
    import tempfile
    import shutil

    if shutil.which("ffmpeg") is None:
        return None

    with tempfile.NamedTemporaryFile(suffix=".mp4", delete=False) as tmp:
        tmp.write(file_bytes)
        tmp_path = tmp.name

    out_dir = Path(tempfile.mkdtemp())
    try:
        # Get video duration
        probe = subprocess.run(
            [
                "ffprobe", "-v", "error", "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1", tmp_path,
            ],
            capture_output=True, text=True, timeout=30,
        )
        try:
            duration = float(probe.stdout.strip())
        except (ValueError, TypeError):
            duration = 10.0  # guess

        if duration <= 0:
            return None

        # Extract frames at even intervals
        interval = duration / max_frames
        timestamps = [interval * (i + 0.5) for i in range(max_frames)]

        results: list[ProcessedMedia] = []
        for i, ts in enumerate(timestamps):
            out_path = out_dir / f"frame_{i:03d}.jpg"
            proc = subprocess.run(
                [
                    "ffmpeg", "-y", "-ss", str(ts), "-i", tmp_path,
                    "-vframes", "1", "-q:v", "3",
                    "-vf", f"scale='min({MAX_SIDE},iw)':'min({MAX_SIDE},ih)':force_original_aspect_ratio=decrease",
                    str(out_path),
                ],
                capture_output=True, timeout=15,
            )
            if proc.returncode != 0 or not out_path.exists():
                continue

            img_bytes = out_path.read_bytes()
            img = Image.open(io.BytesIO(img_bytes))
            img = img.convert("RGB")
            width, height = img.size

            # Re-encode for consistency
            buf = io.BytesIO()
            img.save(buf, format="JPEG", quality=JPEG_QUALITY, optimize=True)
            compressed = buf.getvalue()

            saved_name = f"{uuid.uuid4().hex}.jpg"
            save_dir = Path(settings.media_upload_dir)
            save_dir.mkdir(parents=True, exist_ok=True)
            saved_path = save_dir / saved_name
            saved_path.write_bytes(compressed)

            b64 = base64.b64encode(compressed).decode("ascii")
            data_url = f"data:image/jpeg;base64,{b64}"
            results.append(
                ProcessedMedia(
                    original_name=f"{original_filename}#frame{i}",
                    saved_path=str(saved_path),
                    mime_type="image/jpeg",
                    base64_data_url=data_url,
                    width=width,
                    height=height,
                    file_size_bytes=len(compressed),
                )
            )

        if results:
            logger.info(
                "ffmpeg extracted %d frames from video %s (duration %.1fs)",
                len(results), original_filename, duration,
            )
        return results or None
    except Exception as exc:
        logger.warning("ffmpeg frame extraction failed: %s", exc)
        return None
    finally:
        Path(tmp_path).unlink(missing_ok=True)
        _rmtree(out_dir)


def _extract_frames_cv2(
    file_bytes: bytes,
    original_filename: str,
    max_frames: int = 8,
) -> list[ProcessedMedia] | None:
    """Use OpenCV to extract frames. Returns None if OpenCV is unavailable."""

    import tempfile

    try:
        import cv2  # type: ignore[import-untyped]
        import numpy as np  # noqa: F401
    except ImportError:
        return None

    with tempfile.NamedTemporaryFile(suffix=".mp4", delete=False) as tmp:
        tmp.write(file_bytes)
        tmp_path = tmp.name

    try:
        cap = cv2.VideoCapture(tmp_path)
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        fps = cap.get(cv2.CAP_PROP_FPS)

        if total_frames <= 0:
            return None

        num_frames = min(max_frames, total_frames)
        interval = max(1, total_frames // num_frames)
        frame_indices = [i * interval for i in range(num_frames)]

        results: list[ProcessedMedia] = []
        for idx in frame_indices:
            cap.set(cv2.CAP_PROP_POS_FRAMES, idx)
            ret, frame_bgr = cap.read()
            if not ret:
                continue
            frame_rgb = cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2RGB)
            pil_img = Image.fromarray(frame_rgb)

            width, height = pil_img.size
            if max(width, height) > MAX_SIDE:
                ratio = MAX_SIDE / max(width, height)
                new_size = (int(width * ratio), int(height * ratio))
                pil_img = pil_img.resize(new_size, Image.Resampling.LANCZOS)

            buf = io.BytesIO()
            pil_img.save(buf, format="JPEG", quality=JPEG_QUALITY, optimize=True)
            compressed = buf.getvalue()

            saved_name = f"{uuid.uuid4().hex}.jpg"
            save_dir = Path(settings.media_upload_dir)
            save_dir.mkdir(parents=True, exist_ok=True)
            saved_path = save_dir / saved_name
            saved_path.write_bytes(compressed)

            b64 = base64.b64encode(compressed).decode("ascii")
            data_url = f"data:image/jpeg;base64,{b64}"
            results.append(
                ProcessedMedia(
                    original_name=f"{original_filename}#frame{idx}",
                    saved_path=str(saved_path),
                    mime_type="image/jpeg",
                    base64_data_url=data_url,
                    width=pil_img.size[0],
                    height=pil_img.size[1],
                    file_size_bytes=len(compressed),
                )
            )

        cap.release()
        logger.info(
            "OpenCV extracted %d frames from video %s (%.1f fps, %d total frames)",
            len(results), original_filename, fps, total_frames,
        )
        return results or None
    except Exception as exc:
        logger.warning("OpenCV frame extraction failed: %s", exc)
        return None
    finally:
        Path(tmp_path).unlink(missing_ok=True)


def _video_placeholder(original_filename: str) -> ProcessedMedia:
    """Return a placeholder when video frames cannot be extracted."""
    return ProcessedMedia(
        original_name=original_filename,
        saved_path="",
        mime_type="text/plain",
        base64_data_url="",
        width=0,
        height=0,
        file_size_bytes=0,
    )


def _rmtree(path: Path) -> None:
    """Remove a directory tree, ignoring errors."""
    import shutil

    try:
        shutil.rmtree(str(path), ignore_errors=True)
    except Exception:
        pass


def make_multimodal_content(
    user_text: str,
    media_list: list[ProcessedMedia],
) -> str | list[dict[str, object]]:
    """Build the ``content`` payload for the LLM chat API.

    Returns a plain string when there are no images, or a multimodal content
    array of ``{"type": "text"/"image_url", ...}`` blocks otherwise.
    """
    images = [m for m in media_list if m.base64_data_url]
    if not images:
        return user_text

    parts: list[dict[str, object]] = []
    if user_text.strip():
        parts.append({"type": "text", "text": user_text})
    else:
        parts.append({"type": "text", "text": "请分析我上传的图片内容。"})

    for img in images:
        parts.append(
            {
                "type": "image_url",
                "image_url": {"url": img.base64_data_url, "detail": "auto"},
            }
        )
    return parts
