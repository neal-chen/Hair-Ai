"""Gemini 图像预处理

使用 Gemini 模型对用户照片和发型参考图进行预处理
（统一构图、光线校正等），提升后续 AI 换装效果。
"""

import asyncio
import os
import threading
import time

from openai import AsyncOpenAI

from processor.cache import CacheManager
from processor.image_utils import encode_image, get_file_hash, save_image_from_base64


class GeminiProcessor:
    """Gemini 图像预处理器"""

    def __init__(self, api_key, data_dir):
        self.api_key = api_key
        self.cache = CacheManager(data_dir)
        self.data_dir = data_dir

        # 统计
        self.times = []
        self.success_count = 0
        self.fail_count = 0
        self.lock = threading.Lock()

    def is_enabled(self):
        return bool(self.api_key)

    def get_stats(self):
        with self.lock:
            if not self.times:
                return {"count": 0, "avg": 0}
            return {
                "count": len(self.times),
                "success": self.success_count,
                "fail": self.fail_count,
                "avg": sum(self.times) / len(self.times),
            }

    async def _preprocess_single(self, image_path, image_type):
        """使用 Gemini 预处理单张图片（异步）"""
        thread_name = threading.current_thread().name
        start_time = time.time()

        try:
            cached = self.cache.get_cached_processed_path(image_path, image_type)
            if cached:
                print(f"[{thread_name}] ✓ 缓存命中: {os.path.basename(cached)}")
                return cached

            if not self.api_key:
                print(f"[{thread_name}] 未设置 API_KEY，跳过 Gemini 预处理")
                with self.lock:
                    self.fail_count += 1
                return image_path

            file_hash = get_file_hash(image_path)
            if not file_hash:
                with self.lock:
                    self.fail_count += 1
                return image_path

            base64_image = encode_image(image_path)

            prompt_map = {
                "user": "保持人物一致性，保持服饰和发型不变，身材不要太胖，改为半身证件照，光线充足，露出黑色腰带。",
                "hairstyle": "保持人物一致性，保持服饰和发型发色不变，保持发型纹理清晰，光照条件与原图一致，改为半身证件照，露出黑色腰带。",
            }
            prompt_text = prompt_map.get(image_type, prompt_map["hairstyle"])

            async with AsyncOpenAI(
                base_url="https://openrouter.ai/api/v1",
                api_key=self.api_key,
            ) as client:
                completion = await client.chat.completions.create(
                    model="google/gemini-2.5-flash-image-preview",
                    messages=[{
                        "role": "user",
                        "content": [
                            {"type": "text", "text": prompt_text},
                            {"type": "image_url", "image_url": {"url": f"data:image/jpeg;base64,{base64_image}"}}
                        ]
                    }]
                )

                elapsed = time.time() - start_time
                with self.lock:
                    self.times.append(elapsed)
                print(f"[{thread_name}] Gemini 预处理耗时: {elapsed:.2f}秒")

                return await self._process_response(
                    completion, image_path, image_type, file_hash,
                    thread_name, client, prompt_text, base64_image, attempt=1
                )

        except Exception as e:
            elapsed = time.time() - start_time
            with self.lock:
                self.times.append(elapsed)
                self.fail_count += 1
            print(f"[{thread_name}] Gemini 预处理出错: {e}，使用原图")
            return image_path

    async def _process_response(self, completion, image_path, image_type, file_hash,
                                thread_name, client, prompt_text, base64_image, attempt=1):
        """处理 Gemini API 响应，含重试"""
        max_attempts = 2

        if (hasattr(completion.choices[0].message, 'images')
                and completion.choices[0].message.images):
            image_url = completion.choices[0].message.images[0]["image_url"]['url']
            if image_url.startswith("data:image/"):
                base64_data = image_url.split(",")[1]
                result_path = save_image_from_base64(
                    base64_data, self.data_dir, image_path, image_type, file_hash,
                    update_cache_callback=self.cache.update_cache_index
                )
                if result_path:
                    with self.lock:
                        self.success_count += 1
                    print(f"[{thread_name}] ✓ Gemini 预处理成功: {os.path.basename(result_path)}")
                    return result_path
                else:
                    with self.lock:
                        self.fail_count += 1
                    return image_path
            else:
                with self.lock:
                    self.fail_count += 1
                return image_path
        else:
            if attempt < max_attempts:
                print(f"[{thread_name}] 响应无图片数据，第 {attempt + 1} 次重试...")
                await asyncio.sleep(1)
                try:
                    retry_completion = await client.chat.completions.create(
                        model="google/gemini-2.5-flash-image-preview",
                        messages=[{
                            "role": "user",
                            "content": [
                                {"type": "text", "text": prompt_text},
                                {"type": "image_url", "image_url": {"url": f"data:image/jpeg;base64,{base64_image}"}}
                            ]
                        }]
                    )
                    return await self._process_response(
                        retry_completion, image_path, image_type, file_hash,
                        thread_name, client, prompt_text, base64_image, attempt + 1
                    )
                except Exception as e:
                    print(f"[{thread_name}] 重试失败: {e}，使用原图")
                    with self.lock:
                        self.fail_count += 1
                    return image_path
            else:
                print(f"[{thread_name}] 达到最大重试次数，使用原图")
                with self.lock:
                    self.fail_count += 1
                return image_path

    def preprocess_concurrently(self, user_image_path, hairstyle_image_path):
        """并发预处理用户图和发型图（同步接口）"""
        thread_name = threading.current_thread().name
        try:
            print(f"[{thread_name}] 开始并发预处理图像...")
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            try:
                processed_user, processed_hair = loop.run_until_complete(
                    asyncio.gather(
                        self._preprocess_single(user_image_path, "user"),
                        self._preprocess_single(hairstyle_image_path, "hairstyle"),
                        return_exceptions=True
                    )
                )
                if isinstance(processed_user, Exception):
                    processed_user = user_image_path
                if isinstance(processed_hair, Exception):
                    processed_hair = hairstyle_image_path
                print(f"[{thread_name}] 图像预处理完成")
                return processed_user, processed_hair
            finally:
                loop.close()
        except Exception as e:
            print(f"[{thread_name}] 并发预处理失败: {e}，使用原图")
            return user_image_path, hairstyle_image_path
