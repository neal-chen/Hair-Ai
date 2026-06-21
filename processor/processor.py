"""发型/发色 AI 处理引擎主类

组合所有子模块，提供完整的批处理流水线：
图像预处理 → 上传 → RunningHub 任务 → 轮询 → 结果下载 → 报告生成
"""

import json
import os
import random
import shutil
import threading
import time
import concurrent.futures

from processor.config import ensure_data_directory, env_bool
from processor.runninghub import RunningHubClient
from processor.gemini import GeminiProcessor
from processor.cache import CacheManager
from processor.image_utils import download_image, create_combined_image
from processor.report import create_word_document as _create_word_document
from processor.pai import PaiClientFactory
from processor.volcengine import VolcengineClientFactory


class HairstyleProcessor:
    """发型/发色 AI 处理引擎主类"""

    def __init__(self, api_key=None, webapp_id=None, color_webapp_id=None,
                 max_workers=30, task_timeout=600):
        # ── 数据目录 ──
        self.data_dir = ensure_data_directory()

        # ── API 凭证 ──
        self.api_key = api_key or os.environ.get('RUNNINGHUB_API_KEY')
        if not self.api_key:
            raise ValueError("需要设置 RUNNINGHUB_API_KEY")

        self.webapp_id = webapp_id or os.environ.get('RUNNINGHUB_WEBAPP_ID')
        self.hairstyle_user_node_id = os.environ.get('RUNNINGHUB_HAIRSTYLE_USER_NODE_ID', '77')
        self.hairstyle_hair_node_id = os.environ.get('RUNNINGHUB_HAIRSTYLE_HAIR_NODE_ID', '24')

        self.color_webapp_id = color_webapp_id or os.environ.get('RUNNINGHUB_COLOR_WEBAPP_ID')
        self.color_user_node_id = os.environ.get('RUNNINGHUB_COLOR_USER_NODE_ID', '76')
        self.color_hair_node_id = os.environ.get('RUNNINGHUB_COLOR_HAIR_NODE_ID', '81')
        self.color_pre_webapp_id = os.environ.get('RUNNINGHUB_COLOR_PRE_WEBAPP_ID')

        self.webapp_3d_id = os.environ.get('RUNNINGHUB_3D_WEBAPP_ID')

        # ── 3D 提供商选择 ──
        self.video_3d_provider = os.environ.get('VIDEO_3D_PROVIDER', 'auto').strip().lower()

        # ── 子客户端 ──
        self.runninghub = RunningHubClient(
            host="www.runninghub.cn",
            api_key=self.api_key,
        )
        self.gemini = GeminiProcessor(
            api_key=os.environ.get('OPENROUTER_API_KEY'),
            data_dir=self.data_dir,
        )
        self.cache = CacheManager(self.data_dir)
        self.pai = PaiClientFactory.from_env()
        self.volcengine = VolcengineClientFactory.from_env()

        # ── 结果集 ──
        self.results = []
        self.results_lock = threading.Lock()
        self.max_workers = max_workers
        self.task_timeout = task_timeout
        self.timeout_count = 0

    # ═══════════════════════════════════════════════
    #  3D 提供商路由
    # ═══════════════════════════════════════════════

    def is_volcengine_3d_enabled(self):
        return self.volcengine is not None and self.volcengine.is_enabled()

    def is_pai_3d_enabled(self):
        return self.pai is not None and self.pai.is_enabled()

    def is_runninghub_3d_enabled(self):
        return bool(self.webapp_3d_id)

    def is_3d_enabled(self):
        return self.get_3d_provider() is not None

    def get_3d_provider(self):
        provider = self.video_3d_provider
        aliases = {
            'pai': 'pai', 'pai_ai': 'pai', 'pai-video': 'pai', 'pixverse': 'pai',
            'volc': 'volcengine', 'ark': 'volcengine', 'volcengine': 'volcengine',
            'runninghub': 'runninghub', 'running_hub': 'runninghub',
        }
        if provider != 'auto':
            selected = aliases.get(provider)
            if selected == 'pai':
                return 'pai' if self.is_pai_3d_enabled() else None
            if selected == 'volcengine':
                return 'volcengine' if self.is_volcengine_3d_enabled() else None
            if selected == 'runninghub':
                return 'runninghub' if self.is_runninghub_3d_enabled() else None
            print(f"未知 VIDEO_3D_PROVIDER '{provider}'，回退到自动选择")

        if self.is_pai_3d_enabled():
            return 'pai'
        if self.is_volcengine_3d_enabled():
            return 'volcengine'
        if self.is_runninghub_3d_enabled():
            return 'runninghub'
        return None

    # ═══════════════════════════════════════════════
    #  API 委托方法（兼容旧接口）
    # ═══════════════════════════════════════════════

    def upload_image(self, image_path):
        return self.runninghub.upload_image(image_path)

    def run_hairstyle_task(self, hairstyle_filename, user_filename,
                           max_retries=10, retry_delay=20, cancel_check_func=None):
        return self.runninghub.run_hairstyle_task(
            self.webapp_id, self.hairstyle_hair_node_id, self.hairstyle_user_node_id,
            hairstyle_filename, user_filename, max_retries, retry_delay, cancel_check_func
        )

    def run_color_task(self, hair_filename, user_filename,
                       max_retries=10, retry_delay=20, cancel_check_func=None):
        return self.runninghub.run_color_task(
            self.color_webapp_id, self.color_user_node_id, self.color_hair_node_id,
            hair_filename, user_filename, max_retries, retry_delay, cancel_check_func
        )

    def run_3d_task(self, user_filename, max_retries=10, retry_delay=20, cancel_check_func=None):
        return self.runninghub.run_3d_task(
            self.webapp_3d_id, user_filename, max_retries, retry_delay, cancel_check_func
        )

    def run_color_preprocess_task(self, image_filename, max_retries=10,
                                  retry_delay=20, cancel_check_func=None):
        return self.runninghub.run_color_preprocess_task(
            self.color_pre_webapp_id, image_filename, max_retries, retry_delay, cancel_check_func
        )

    def call_runninghub_color_preprocess(self, image_filename):
        return self.runninghub.call_runninghub_color_preprocess(
            self.color_pre_webapp_id, image_filename
        )

    def check_task_status(self, task_id):
        return self.runninghub.check_task_status(task_id)

    def get_task_results(self, task_id):
        return self.runninghub.get_task_results(task_id)

    def cancel_task(self, task_id):
        return self.runninghub.cancel_task(task_id)

    # ── 3D API 委托 ──

    def run_3d_task_with_pai(self, image_path, cancel_check_func=None):
        if not self.pai:
            raise ValueError("Pai AI 客户端未配置")
        return self.pai.run_3d_task(image_path, cancel_check_func)

    def run_3d_task_with_volcengine(self, image_url, cancel_check_func=None):
        if not self.volcengine:
            raise ValueError("火山引擎客户端未配置")
        return self.volcengine.run_3d_task(image_url, cancel_check_func)

    def check_3d_task_status(self, task_id):
        provider = self.get_3d_provider()
        if provider == 'pai' and self.pai:
            return self.pai.check_task_status(task_id)
        if provider == 'volcengine' and self.volcengine:
            return self.volcengine.check_task_status(task_id)
        return self.check_task_status(task_id)

    def get_3d_task_results(self, task_id):
        provider = self.get_3d_provider()
        if provider == 'pai' and self.pai:
            return self.pai.get_task_results(task_id)
        if provider == 'volcengine' and self.volcengine:
            return self.volcengine.get_task_results(task_id)
        return self.get_task_results(task_id)

    def cancel_3d_task(self, task_id):
        provider = self.get_3d_provider()
        if provider == 'volcengine' and self.volcengine:
            return self.volcengine.cancel_task(task_id)
        # RunningHub cancel
        return self.cancel_task(task_id)

    # ── Gemini 预处理委托 ──

    def preprocess_images_concurrently(self, user_image_path, hairstyle_image_path):
        return self.gemini.preprocess_concurrently(user_image_path, hairstyle_image_path)

    # ═══════════════════════════════════════════════
    #  图像处理委托
    # ═══════════════════════════════════════════════

    def encode_image(self, image_path):
        from processor.image_utils import encode_image as _e
        return _e(image_path)

    def fix_image_orientation(self, img):
        from processor.image_utils import fix_image_orientation as _f
        return _f(img)

    def get_file_hash(self, file_path):
        from processor.image_utils import get_file_hash as _g
        return _g(file_path)

    def save_image_from_base64(self, base64_str, original_path, image_type, file_hash):
        from processor.image_utils import save_image_from_base64 as _s
        return _s(base64_str, self.data_dir, original_path, image_type, file_hash,
                  update_cache_callback=self.cache.update_cache_index)

    def download_image(self, url, save_path):
        return download_image(url, save_path)

    def resize_image_for_word(self, image_path, max_width=2.5):
        from processor.image_utils import resize_image_for_word as _r
        return _r(image_path, max_width)

    def create_combined_image(self, hairstyle_path, user_path, result_paths, output_path):
        return create_combined_image(hairstyle_path, user_path, result_paths, output_path)

    # ── 缓存委托 ──

    def update_cache_index(self, original_path, processed_path, file_hash, image_type):
        self.cache.update_cache_index(original_path, processed_path, file_hash, image_type)

    def get_cached_processed_path(self, original_path, image_type):
        return self.cache.get_cached_processed_path(original_path, image_type)

    def get_cache_info(self):
        return self.cache.get_cache_info()

    def clean_old_cache(self, max_age_hours=24, max_total_size_mb=100):
        return self.cache.clean_old_cache(max_age_hours, max_total_size_mb)

    def get_disk_usage(self):
        return self.cache.get_disk_usage()

    def delete_cache_file(self, file_path, image_type):
        return self.cache.delete_cache_file(file_path, image_type)

    def get_cache_files_detailed(self):
        return self.cache.get_cache_files_detailed()

    # ═══════════════════════════════════════════════
    #  批处理 - 换发型
    # ═══════════════════════════════════════════════

    def process_single_combination_with_timeout(self, task_info):
        """处理单个用户×发型组合（带超时控制）"""
        start_time = time.time()
        thread_name = threading.current_thread().name
        user_file = task_info[2]
        hairstyle_file = task_info[3]

        try:
            print(f"[{thread_name}] 开始处理任务 (超时: {self.task_timeout}s): "
                  f"{user_file} + {hairstyle_file}")
            result = self.process_single_combination(task_info)
            elapsed = time.time() - start_time

            if elapsed > self.task_timeout:
                self.timeout_count += 1
                print(f"[{thread_name}] ⚠️ 超时 ({elapsed:.2f}s): {user_file} + {hairstyle_file}")
                return None

            print(f"[{thread_name}] 完成，耗时: {elapsed:.2f}s")
            return result
        except Exception as e:
            elapsed = time.time() - start_time
            print(f"[{thread_name}] ❌ 异常 ({elapsed:.2f}s): {user_file} + {hairstyle_file}")
            print(f"[{thread_name}] 详情: {e}")
            return None

    def process_single_combination(self, task_info):
        """处理单个用户×发型组合"""
        user_full_path, hairstyle_full_path, user_file, hairstyle_file, gender_name, results_dir = task_info
        thread_name = threading.current_thread().name
        print(f"[{thread_name}] Processing: {user_file} + {hairstyle_file}")

        try:
            # Step 1: 上传图片
            print(f"[{thread_name}] 上传图片...")
            user_filename = self.upload_image(user_full_path)
            if not user_filename:
                return None
            hairstyle_filename = self.upload_image(hairstyle_full_path)
            if not hairstyle_filename:
                return None

            # Step 2: 提交任务
            print(f"[{thread_name}] 提交换发型任务...")
            task_id = self.run_hairstyle_task(hairstyle_filename, user_filename)
            if not task_id:
                return None

            # Step 3: 轮询
            print(f"[{thread_name}] 任务 {task_id} 启动完成，等待处理...")
            status = None
            for _ in range(500):  # max 1000s
                status = self.check_task_status(task_id)
                if status == "SUCCESS":
                    break
                if status in ("FAILED", "CANCELLED"):
                    print(f"[{thread_name}] 任务失败: {status}")
                    return None
                time.sleep(2)

            if status != "SUCCESS":
                print(f"[{thread_name}] 任务未成功完成: {status}")
                return None

            # Step 4: 获取结果
            print(f"[{thread_name}] 获取结果...")
            results = self.get_task_results(task_id)
            if not results:
                return None

            # Step 5: 下载结果图
            result_paths = []
            result_filenames = []
            for i, result in enumerate(results):
                result_url = result.get("fileUrl")
                if result_url:
                    ext = os.path.splitext(result_url.split('?')[0])[1] or ".jpg"
                    result_filename = f"{user_file}_result_{i}{ext}"
                    result_path = os.path.join(results_dir, result_filename)
                    if self.download_image(result_url, result_path):
                        result_paths.append(result_path)
                        result_filenames.append(result_filename)

            if not result_paths:
                print(f"[{thread_name}] 未下载到结果图片")
                return None

            # Step 6: 创建合成图
            combined_filename = f"{user_file}_combined.png"
            combined_path = os.path.join(results_dir, combined_filename)
            self.create_combined_image(hairstyle_full_path, user_full_path,
                                       result_paths, combined_path)

            # Step 7: 记录结果
            result_data = {
                "gender": gender_name,
                "user_filename": user_file,
                "hairstyle_filename": hairstyle_file,
                "user_image": user_full_path,
                "hairstyle_image": hairstyle_full_path,
                "result_images": result_paths,
                "result_filenames": result_filenames,
                "combined_image": combined_path if os.path.exists(combined_path) else None,
            }
            with self.results_lock:
                self.results.append(result_data)

            print(f"[{thread_name}] ✓ 处理完成: {user_file} + {hairstyle_file}")
            return result_data

        except Exception as e:
            print(f"[{thread_name}] 处理失败: {e}")
            return None

    def process_gender_folder(self, gender_path, gender_name):
        """批量处理所有用户图 × 所有发型图的组合"""
        user_dir = os.path.join(gender_path, "user")
        hairstyle_dir = os.path.join(gender_path, "hairstyle")
        results_dir = os.path.join(gender_path, "results")

        for d in (user_dir, hairstyle_dir):
            if not os.path.exists(d):
                print(f"目录不存在: {d}")
                return

        os.makedirs(results_dir, exist_ok=True)

        # 收集文件
        user_images = sorted([f for f in os.listdir(user_dir)
                              if f.lower().endswith(('.png', '.jpg', '.jpeg'))])
        hairstyle_images = sorted([f for f in os.listdir(hairstyle_dir)
                                    if f.lower().endswith(('.png', '.jpg', '.jpeg'))])

        if not user_images:
            print(f"用户图片目录为空: {user_dir}")
            return
        if not hairstyle_images:
            print(f"发型图片目录为空: {hairstyle_dir}")
            return

        # 随机采样用户图
        if len(user_images) > 5:
            user_images = random.sample(user_images, 5)

        tasks = []
        for user_img in user_images:
            for hairstyle_img in hairstyle_images:
                tasks.append((
                    os.path.join(user_dir, user_img),
                    os.path.join(hairstyle_dir, hairstyle_img),
                    os.path.splitext(user_img)[0],
                    os.path.splitext(hairstyle_img)[0],
                    gender_name,
                    results_dir,
                ))

        total = len(tasks)
        print(f"\n{'='*60}")
        print(f"批量处理 [{gender_name}]：{len(user_images)} × {len(hairstyle_images)} = {total} 组合")
        print(f"{'='*60}\n")

        with concurrent.futures.ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            futures = {executor.submit(self.process_single_combination_with_timeout, task): task
                       for task in tasks}
            done = 0
            for future in concurrent.futures.as_completed(futures):
                done += 1
                task = futures[future]
                try:
                    future.result()
                except Exception as e:
                    print(f"任务异常: {task[2]} + {task[3]} → {e}")
                print(f"进度: [{done}/{total}]")

        print(f"\n批量处理完成! 成功: {len(self.results)}/{total}")

    # ═══════════════════════════════════════════════
    #  批处理 - 换发色
    # ═══════════════════════════════════════════════

    def process_single_color_combination_with_timeout(self, task_info):
        """处理单个用户×发色组合（带超时控制）"""
        start_time = time.time()
        thread_name = threading.current_thread().name
        user_file = task_info[2]
        color_file = task_info[3]

        try:
            print(f"[{thread_name}] 开始发色任务 (超时: {self.task_timeout}s): "
                  f"{user_file} + {color_file}")
            result = self.process_single_color_combination(task_info)
            elapsed = time.time() - start_time

            if elapsed > self.task_timeout:
                self.timeout_count += 1
                print(f"[{thread_name}] ⚠️ 超时 ({elapsed:.2f}s): {user_file} + {color_file}")
                return None

            print(f"[{thread_name}] 发色完成，耗时: {elapsed:.2f}s")
            return result
        except Exception as e:
            elapsed = time.time() - start_time
            print(f"[{thread_name}] ❌ 异常 ({elapsed:.2f}s): {user_file} + {color_file}: {e}")
            return None

    def process_single_color_combination(self, task_info):
        """处理单个用户×发色组合"""
        user_full_path, color_full_path, user_file, color_file, results_dir = task_info
        thread_name = threading.current_thread().name
        print(f"[{thread_name}] Processing Color: {user_file} + {color_file}")

        try:
            # Step 1: 上传用户图
            print(f"[{thread_name}] 上传图片...")
            user_dir_path = os.path.dirname(user_full_path)
            user_name = os.path.basename(user_full_path)
            if '.' in user_name:
                parts = user_name.split('.')
                user_name_new = '.'.join(parts[:-1]) + '.' + parts[-1] if len(parts) > 2 else user_name
                user_full_path_new = os.path.join(user_dir_path, user_name_new)
                if user_full_path_new != user_full_path:
                    shutil.copy(user_full_path, user_full_path_new)
            else:
                user_full_path_new = user_full_path

            user_filename = self.upload_image(user_full_path_new)
            if not user_filename:
                return None

            color_filename = self.upload_image(color_full_path)
            if not color_filename:
                return None

            # Step 2: 提交任务
            print(f"[{thread_name}] 提交换发色任务...")
            task_id = self.run_color_task(color_filename, user_filename)
            if not task_id:
                return None

            # Step 3: 轮询
            print(f"[{thread_name}] 发色任务 {task_id} 等待处理...")
            status = None
            for _ in range(100):  # max 1000s
                status = self.check_task_status(task_id)
                if status == "SUCCESS":
                    break
                if status in ("FAILED", "CANCELLED"):
                    print(f"[{thread_name}] 发色任务失败: {status}")
                    return None
                time.sleep(10)

            if status != "SUCCESS":
                print(f"[{thread_name}] 发色任务未成功完成: {status}")
                return None

            # Step 4: 获取结果
            print(f"[{thread_name}] 获取发色结果...")
            results = self.get_task_results(task_id)
            if not results:
                return None

            # Step 5: 下载结果图
            result_paths = []
            result_filenames = []
            for i, result in enumerate(results):
                result_url = result.get("fileUrl")
                if result_url:
                    ext = os.path.splitext(result_url.split('?')[0])[1] or ".jpg"
                    result_filename = f"{user_file}_color_result_{i}{ext}"
                    result_path = os.path.join(results_dir, result_filename)
                    if self.download_image(result_url, result_path):
                        result_paths.append(result_path)
                        result_filenames.append(result_filename)

            if not result_paths:
                print(f"[{thread_name}] 未下载到发色结果图")
                return None

            # Step 6: 创建合成图
            combined_filename = f"{user_file}_color_combined.png"
            combined_path = os.path.join(results_dir, combined_filename)
            self.create_combined_image(color_full_path, user_full_path,
                                       result_paths, combined_path)

            # Step 7: 记录结果
            result_data = {
                "gender": "color",
                "user_filename": user_file,
                "hairstyle_filename": color_file,
                "user_image": user_full_path,
                "hairstyle_image": color_full_path,
                "result_images": result_paths,
                "result_filenames": result_filenames,
                "combined_image": combined_path if os.path.exists(combined_path) else None,
            }
            with self.results_lock:
                self.results.append(result_data)

            print(f"[{thread_name}] ✓ 发色处理完成: {user_file} + {color_file}")
            return result_data

        except Exception as e:
            print(f"[{thread_name}] 发色处理失败: {e}")
            return None

    def process_color_folder(self, user_dir, color_dir):
        """批量处理所有用户图 × 所有发色图的组合"""
        results_dir = os.path.join(os.path.dirname(user_dir), "color_results")

        if not os.path.exists(user_dir):
            print(f"用户目录不存在: {user_dir}")
            return
        if not os.path.exists(color_dir):
            print(f"发色目录不存在: {color_dir}")
            return

        os.makedirs(results_dir, exist_ok=True)

        user_images = sorted([f for f in os.listdir(user_dir)
                              if f.lower().endswith(('.png', '.jpg', '.jpeg'))])
        color_images = sorted([f for f in os.listdir(color_dir)
                                if f.lower().endswith(('.png', '.jpg', '.jpeg'))])

        if not user_images:
            print(f"用户图片为空: {user_dir}")
            return
        if not color_images:
            print(f"发色图片为空: {color_dir}")
            return

        # 随机采样
        if len(user_images) > 10:
            user_images = random.sample(user_images, 10)

        tasks = []
        for user_img in user_images:
            for color_img in color_images:
                tasks.append((
                    os.path.join(user_dir, user_img),
                    os.path.join(color_dir, color_img),
                    os.path.splitext(user_img)[0],
                    os.path.splitext(color_img)[0],
                    results_dir,
                ))

        total = len(tasks)
        print(f"\n{'='*60}")
        print(f"批量发色处理：{len(user_images)} × {len(color_images)} = {total} 组合")
        print(f"{'='*60}\n")

        with concurrent.futures.ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            futures = {executor.submit(self.process_single_color_combination_with_timeout, task): task
                       for task in tasks}
            done = 0
            for future in concurrent.futures.as_completed(futures):
                done += 1
                task = futures[future]
                try:
                    future.result()
                except Exception as e:
                    print(f"发色任务异常: {task[2]} + {task[3]} → {e}")
                print(f"进度: [{done}/{total}]")

        print(f"\n批量发色处理完成! 成功: {len(self.results)}/{total}")

    # ═══════════════════════════════════════════════
    #  报告生成
    # ═══════════════════════════════════════════════

    def create_word_document(self, output_path="hairstyle_results.docx"):
        """生成 Word 结果报告"""
        _create_word_document(self.results, output_path)

    # ═══════════════════════════════════════════════
    #  统计
    # ═══════════════════════════════════════════════

    def get_average_task_time(self):
        """输出所有任务耗时统计"""
        runninghub_stats = self.runninghub.get_stats()
        gemini_stats = self.gemini.get_stats()

        print(f"\n=== RunningHub 任务统计 ===")
        print(f"总任务数: {runninghub_stats['count']}")
        print(f"平均耗时: {runninghub_stats['avg']:.2f}秒")
        if runninghub_stats['count'] > 0:
            print(f"最短: {runninghub_stats['min']:.2f}秒")
            print(f"最长: {runninghub_stats['max']:.2f}秒")

        if gemini_stats['count'] > 0:
            print(f"\n=== Gemini 预处理统计 ===")
            print(f"总预处理请求数: {gemini_stats['count']}")
            print(f"成功率: {gemini_stats['success'] / max(1, gemini_stats['count']) * 100:.1f}%")
            print(f"平均耗时: {gemini_stats['avg']:.2f}秒")

        total = len(self.results)
        if total > 0 or self.timeout_count > 0:
            attempts = total + self.timeout_count
            print(f"\n=== 综合统计 ===")
            print(f"处理组合数: {total}")
            print(f"超时数: {self.timeout_count}")
            if attempts > 0:
                print(f"成功率: {total / attempts * 100:.1f}%")
            print(f"超时限制: {self.task_timeout}秒")

        return runninghub_stats['avg']
