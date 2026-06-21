"""火山引擎 (豆包) 图生视频客户端

提供基于火山引擎/豆包 Seedance API 的 3D 视频生成功能。
"""

import requests


class VolcengineClient:
    """火山引擎 (豆包 Seedance) 图生视频客户端"""

    def __init__(self, api_key, base_url, model,
                 prompt="", ratio="9:16", duration=6, resolution="1080p"):
        self.api_key = api_key
        self.base_url = base_url.rstrip('/')
        self.model = model
        self.prompt = prompt
        self.ratio = ratio
        self.duration = duration
        self.resolution = resolution

    def is_enabled(self):
        return bool(self.api_key)

    @staticmethod
    def normalize_status(status):
        if not status:
            return None
        normalized = str(status).strip().upper()
        if normalized in {"SUCCEEDED", "SUCCESS", "COMPLETED", "DONE"}:
            return "SUCCESS"
        if normalized in {"FAILED", "FAILURE", "ERROR"}:
            return "FAILED"
        if normalized in {"CANCELLED", "CANCELED"}:
            return "CANCELLED"
        if normalized in {"PENDING", "QUEUED", "SUBMITTED", "CREATED"}:
            return "PENDING"
        if normalized in {"RUNNING", "PROCESSING", "IN_PROGRESS"}:
            return "RUNNING"
        return normalized

    @staticmethod
    def extract_status(payload):
        """从响应 payload 中提取任务状态"""
        if not isinstance(payload, dict):
            return None
        for key in ("status", "task_status", "state"):
            if payload.get(key):
                return payload.get(key)
        data = payload.get("data")
        if isinstance(data, dict):
            for key in ("status", "task_status", "state"):
                if data.get(key):
                    return data.get(key)
        return None

    @staticmethod
    def extract_video_results(payload):
        """从响应 payload 中提取视频结果列表"""
        results = []
        if not isinstance(payload, dict):
            return results

        candidates = []
        for key in ("content", "contents", "data", "result", "output"):
            value = payload.get(key)
            if value:
                candidates.append(value)

        while candidates:
            current = candidates.pop(0)
            if isinstance(current, list):
                candidates.extend(current)
                continue
            if not isinstance(current, dict):
                continue

            video_url = current.get("video_url")
            if isinstance(video_url, dict) and video_url.get("url"):
                results.append({"fileUrl": video_url["url"], "fileType": "video"})
            elif isinstance(video_url, str) and video_url:
                results.append({"fileUrl": video_url, "fileType": "video"})

            file_url = current.get("fileUrl") or current.get("url")
            if file_url and (current.get("type") == "video_url" or str(file_url).lower().endswith(".mp4")):
                results.append({"fileUrl": file_url, "fileType": "video"})

            for value in current.values():
                if isinstance(value, (dict, list)):
                    candidates.append(value)

        # 去重
        seen = set()
        deduped = []
        for r in results:
            if r["fileUrl"] not in seen:
                seen.add(r["fileUrl"])
                deduped.append(r)
        return deduped

    def run_3d_task(self, image_url, cancel_check_func=None):
        """创建图生视频任务，返回 task_id"""
        if not self.is_enabled():
            raise ValueError("需要设置 ARK_API_KEY")
        if not image_url:
            raise ValueError("需要提供 image_url")

        if cancel_check_func and cancel_check_func():
            print("3D任务在火山引擎提交前被取消")
            return None

        payload = {
            "model": self.model,
            "content": [
                {"type": "text", "text": self.prompt},
                {"type": "image_url", "image_url": {"url": image_url}}
            ],
            "ratio": self.ratio,
            "duration": self.duration,
            "resolution": self.resolution,
        }
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {self.api_key}",
        }

        try:
            response = requests.post(
                self.base_url, json=payload, headers=headers, timeout=120
            )
            response.raise_for_status()
            result = response.json()
            task_id = result.get("id")
            if not task_id and isinstance(result.get("data"), dict):
                task_id = result["data"].get("id")
            if task_id:
                print(f"火山引擎 3D 任务启动成功: {task_id}")
                return task_id
            print(f"火山引擎 3D 响应缺少 id: {result}")
            return None
        except Exception as e:
            print(f"火山引擎 3D 任务启动出错: {e}")
            return None

    def check_task_status(self, task_id):
        """查询火山引擎任务状态"""
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {self.api_key}",
        }
        try:
            response = requests.get(
                f"{self.base_url}/{task_id}", headers=headers, timeout=120
            )
            response.raise_for_status()
            result = response.json()
            return self.normalize_status(self.extract_status(result))
        except Exception as e:
            print(f"火山引擎状态查询出错 {task_id}: {e}")
            return None

    def get_task_results(self, task_id):
        """获取火山引擎任务结果"""
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {self.api_key}",
        }
        try:
            response = requests.get(
                f"{self.base_url}/{task_id}", headers=headers, timeout=120
            )
            response.raise_for_status()
            result = response.json()
            outputs = self.extract_video_results(result)
            if not outputs:
                print(f"火山引擎任务暂无视频输出: {result}")
            return outputs
        except Exception as e:
            print(f"火山引擎获取结果出错 {task_id}: {e}")
            return None

    def cancel_task(self, task_id):
        """取消火山引擎任务（待实现）"""
        print(f"火山引擎取消任务 {task_id} 暂未实现")
        return False


class VolcengineClientFactory:
    """从环境变量创建 VolcengineClient 的工厂方法"""
    @staticmethod
    def from_env():
        import os
        api_key = os.environ.get('ARK_API_KEY') or os.environ.get('VOLCENGINE_ARK_API_KEY')
        if not api_key:
            return None
        return VolcengineClient(
            api_key=api_key,
            base_url=os.environ.get(
                'VOLCENGINE_3D_BASE_URL',
                'https://ark.cn-beijing.volces.com/api/v3/contents/generations/tasks'
            ),
            model=os.environ.get('VOLCENGINE_3D_MODEL', 'doubao-seedance-1-5-pro-251215'),
            prompt=os.environ.get(
                'VOLCENGINE_3D_PROMPT',
                '人物优雅地360度转身，以展示其发型。 --duration 6 --camerafixed false --watermark true'
            ),
            ratio=os.environ.get('VOLCENGINE_3D_RATIO', '9:16'),
            duration=int(os.environ.get('VOLCENGINE_3D_DURATION', '6')),
            resolution=os.environ.get('VOLCENGINE_3D_RESOLUTION', '1080p'),
        )
