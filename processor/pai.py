"""Pai AI (PixVerse) 图生视频客户端

提供基于 Pai AI / PixVerse API 的 3D 视频生成功能：
图片上传 → 视频生成任务提交 → 状态轮询 → 结果获取
"""

import mimetypes
import os
import time
import requests
import uuid


class PaiClient:
    """Pai AI (PixVerse) 图生视频客户端"""

    def __init__(self, api_key, base_url, model="v6",
                 upload_timeout=300, duration=6, quality="1080p",
                 motion_mode="normal", template_id=0, seed=0,
                 prompt="", negative_prompt="", style=None,
                 camera_movement=None, generate_audio=False,
                 generate_multi_clip=False):
        self.api_key = api_key
        self.base_url = base_url.rstrip('/')
        self.model = model
        self.upload_timeout = upload_timeout
        self.duration = duration
        self.quality = quality
        self.motion_mode = motion_mode
        self.template_id = template_id
        self.seed = seed
        self.prompt = prompt
        self.negative_prompt = negative_prompt
        self.style = style
        self.camera_movement = camera_movement
        self.generate_audio = generate_audio
        self.generate_multi_clip = generate_multi_clip

    def is_enabled(self):
        return bool(self.api_key)

    def _headers(self, include_json_content_type=False):
        headers = {
            "API-KEY": self.api_key,
            "Ai-trace-id": str(uuid.uuid4()),
        }
        if include_json_content_type:
            headers["Content-Type"] = "application/json"
        return headers

    def _extract_response(self, payload, operation_name):
        if not isinstance(payload, dict):
            print(f"Pai AI {operation_name} 返回非对象响应: {payload}")
            return None
        err_code = payload.get("ErrCode")
        if str(err_code) == "0":
            return payload.get("Resp") or {}
        print(f"Pai AI {operation_name} 失败: {payload}")
        return None

    @staticmethod
    def normalize_status(status):
        if status is None:
            return None
        normalized = str(status).strip().upper()
        if normalized in {"1", "SUCCESS", "SUCCEEDED", "COMPLETED", "DONE"}:
            return "SUCCESS"
        if normalized in {"7", "8", "FAILED", "FAILURE", "ERROR"}:
            return "FAILED"
        if normalized in {"CANCELLED", "CANCELED"}:
            return "CANCELLED"
        if normalized in {"5", "6", "RUNNING", "PROCESSING", "IN_PROGRESS"}:
            return "RUNNING"
        if normalized in {"0", "2", "3", "4", "PENDING", "QUEUED", "SUBMITTED", "CREATED"}:
            return "PENDING"
        return normalized

    def upload_image(self, image_path):
        """上传本地图片到 Pai AI，返回 img_id"""
        if not self.is_enabled():
            raise ValueError("需要设置 PAI_VIDEO_API_KEY")
        if not image_path:
            raise ValueError("需要提供 image_path")

        upload_url = f"{self.base_url}/openapi/v2/image/upload"
        file_type = mimetypes.guess_type(image_path)[0] or "image/jpeg"

        try:
            with open(image_path, "rb") as image_file:
                files = {"image": (os.path.basename(image_path), image_file, file_type)}
                response = requests.post(
                    upload_url, headers=self._headers(), files=files,
                    timeout=self.upload_timeout
                )
            response.raise_for_status()
            result = response.json()
            resp = self._extract_response(result, "图片上传")
            if not resp:
                return None
            img_id = resp.get("img_id")
            if img_id is None:
                print(f"Pai AI 上传响应缺少 img_id: {result}")
                return None
            print(f"Pai AI 图片上传成功: {image_path} → img_id={img_id}")
            return img_id
        except Exception as e:
            print(f"Pai AI 图片上传出错: {e}")
            return None

    def run_3d_task(self, image_path, cancel_check_func=None):
        """创建 Pai AI 图生视频任务，返回 video_id"""
        if not self.is_enabled():
            raise ValueError("需要设置 PAI_VIDEO_API_KEY")
        if not image_path:
            raise ValueError("需要提供 image_path")

        if cancel_check_func and cancel_check_func():
            print("3D任务在Pai AI图片上传前被取消")
            return None

        img_id = self.upload_image(image_path)
        if img_id is None:
            return None

        if cancel_check_func and cancel_check_func():
            print("3D任务在Pai AI生成提交前被取消")
            return None

        payload = {
            "duration": self.duration,
            "img_id": img_id,
            "model": self.model,
            "template_id": self.template_id,
            "motion_mode": self.motion_mode,
            "negative_prompt": self.negative_prompt,
            "prompt": self.prompt,
            "quality": self.quality,
            "seed": self.seed,
            "generate_audio_switch": self.generate_audio,
            "generate_multi_clip_switch": self.generate_multi_clip,
        }
        if self.style:
            payload["style"] = self.style
        if self.camera_movement:
            payload["camera_movement"] = self.camera_movement

        try:
            response = requests.post(
                f"{self.base_url}/openapi/v2/video/img/generate",
                json=payload,
                headers=self._headers(include_json_content_type=True),
                timeout=120
            )
            response.raise_for_status()
            result = response.json()
            resp = self._extract_response(result, "图生视频")
            if not resp:
                return None
            video_id = resp.get("video_id")
            if video_id is None:
                print(f"Pai AI 视频生成响应缺少 video_id: {result}")
                return None
            print(f"Pai AI 3D 任务启动成功: {video_id}")
            return str(video_id)
        except Exception as e:
            print(f"Pai AI 3D 任务启动出错: {e}")
            return None

    def check_task_status(self, task_id):
        """查询 Pai AI 视频生成任务状态"""
        try:
            response = requests.get(
                f"{self.base_url}/openapi/v2/video/result/{task_id}",
                headers=self._headers(),
                timeout=120
            )
            response.raise_for_status()
            result = response.json()
            resp = self._extract_response(result, "视频状态查询")
            if resp is None:
                return None
            return self.normalize_status(resp.get("status"))
        except Exception as e:
            print(f"Pai AI 状态查询出错 {task_id}: {e}")
            return None

    def get_task_results(self, task_id):
        """获取 Pai AI 视频生成结果"""
        try:
            response = requests.get(
                f"{self.base_url}/openapi/v2/video/result/{task_id}",
                headers=self._headers(),
                timeout=120
            )
            response.raise_for_status()
            result = response.json()
            resp = self._extract_response(result, "视频结果")
            if resp is None:
                return None
            video_url = resp.get("url")
            if not video_url:
                print(f"Pai AI 任务尚未生成视频: {result}")
                return []
            return [{"fileUrl": video_url, "fileType": "video"}]
        except Exception as e:
            print(f"Pai AI 获取结果出错 {task_id}: {e}")
            return None


class PaiClientFactory:
    """从环境变量创建 PaiClient 的工厂方法"""
    @staticmethod
    def from_env():
        import os
        api_key = os.environ.get('PAI_VIDEO_API_KEY')
        if not api_key:
            return None
        return PaiClient(
            api_key=api_key,
            base_url=os.environ.get('PAI_VIDEO_BASE_URL', 'https://app-api.pixverseai.cn'),
            model=os.environ.get('PAI_VIDEO_MODEL', 'v6'),
            upload_timeout=int(os.environ.get('PAI_VIDEO_UPLOAD_TIMEOUT', '300')),
            duration=int(os.environ.get('PAI_VIDEO_DURATION', '6')),
            quality=os.environ.get('PAI_VIDEO_QUALITY', '1080p'),
            motion_mode=os.environ.get('PAI_VIDEO_MOTION_MODE', 'normal'),
            template_id=int(os.environ.get('PAI_VIDEO_TEMPLATE_ID', '0')),
            seed=int(os.environ.get('PAI_VIDEO_SEED', '0')),
            prompt=os.environ.get(
                'PAI_VIDEO_PROMPT',
                '单镜头无剪切，人物站在*纯白*的背景下，360度转身，以展示其发型。'
            ),
            negative_prompt=os.environ.get('PAI_VIDEO_NEGATIVE_PROMPT', ''),
            style=os.environ.get('PAI_VIDEO_STYLE'),
            camera_movement=os.environ.get('PAI_VIDEO_CAMERA_MOVEMENT'),
            generate_audio=os.environ.get('PAI_VIDEO_GENERATE_AUDIO', 'false').lower() in ('1', 'true', 'yes'),
            generate_multi_clip=os.environ.get('PAI_VIDEO_GENERATE_MULTI_CLIP', 'false').lower() in ('1', 'true', 'yes'),
        )
