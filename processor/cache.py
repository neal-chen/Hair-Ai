"""缓存管理模块

管理 Gemini 预处理图像的本地缓存（基于文件哈希的索引）。
"""

import json
import os
import time
from datetime import datetime
from processor.image_utils import get_file_hash


class CacheManager:
    """预处理图像缓存管理器"""

    def __init__(self, data_dir):
        self.data_dir = data_dir

    # ── 缓存索引操作 ──

    def _cache_index_path(self, image_type):
        cache_dir = os.path.join(self.data_dir, f"gemini_processed_{image_type}")
        return os.path.join(cache_dir, "cache_index.json")

    def update_cache_index(self, original_path, processed_path, file_hash, image_type):
        """更新缓存索引文件"""
        try:
            cache_dir = os.path.join(self.data_dir, f"gemini_processed_{image_type}")
            cache_index_path = os.path.join(cache_dir, "cache_index.json")

            cache_index = {}
            if os.path.exists(cache_index_path):
                try:
                    with open(cache_index_path, 'r', encoding='utf-8') as f:
                        cache_index = json.load(f)
                except:
                    pass

            cache_index[file_hash] = {
                "processed_path": processed_path,
                "original_path": original_path,
                "timestamp": datetime.now().isoformat(),
                "original_filename": os.path.basename(original_path)
            }

            with open(cache_index_path, 'w', encoding='utf-8') as f:
                json.dump(cache_index, f, ensure_ascii=False, indent=2)
        except Exception as e:
            print(f"更新缓存索引失败: {e}")

    def get_cached_processed_path(self, original_path, image_type):
        """基于文件哈希检查是否已有缓存的预处理图片"""
        try:
            file_hash = get_file_hash(original_path)
            if not file_hash:
                return None

            cache_index_path = self._cache_index_path(image_type)
            if not os.path.exists(cache_index_path):
                return None

            try:
                with open(cache_index_path, 'r', encoding='utf-8') as f:
                    cache_index = json.load(f)
            except:
                return None

            if file_hash in cache_index:
                cached_info = cache_index[file_hash]
                cached_path = cached_info["processed_path"]
                if os.path.exists(cached_path):
                    return cached_path
                else:
                    del cache_index[file_hash]
                    with open(cache_index_path, 'w', encoding='utf-8') as f:
                        json.dump(cache_index, f, ensure_ascii=False, indent=2)
            return None
        except Exception as e:
            print(f"检查缓存失败: {e}")
            return None

    # ── 缓存维护 ──

    def get_cache_info(self):
        """获取所有缓存信息"""
        try:
            cache_dirs = [d for d in os.listdir(self.data_dir) if d.startswith("gemini_processed_")]
            info = {}
            for cache_dir_name in cache_dirs:
                cache_dir = os.path.join(self.data_dir, cache_dir_name)
                image_type = cache_dir_name.replace("gemini_processed_", "")
                files = os.listdir(cache_dir)
                total_size = sum(os.path.getsize(os.path.join(cache_dir, f)) for f in files if os.path.isfile(os.path.join(cache_dir, f)))
                info[image_type] = {
                    "path": cache_dir,
                    "file_count": len([f for f in files if os.path.isfile(os.path.join(cache_dir, f))]),
                    "total_size_mb": total_size / (1024 * 1024),
                    "cache_index": os.path.exists(self._cache_index_path(image_type))
                }
            return info
        except Exception as e:
            print(f"获取缓存信息失败: {e}")
            return {}

    def clean_old_cache(self, max_age_hours=24, max_total_size_mb=100):
        """清理旧的缓存文件"""
        try:
            now = time.time()
            total_cleaned = 0
            cache_info = self.get_cache_info()

            for image_type, info in cache_info.items():
                cache_dir = info["path"]
                if not os.path.exists(cache_dir):
                    continue

                # 先清理超过时效的文件
                for filename in os.listdir(cache_dir):
                    filepath = os.path.join(cache_dir, filename)
                    if filename == "cache_index.json" or not os.path.isfile(filepath):
                        continue
                    file_age_hours = (now - os.path.getmtime(filepath)) / 3600
                    if file_age_hours > max_age_hours:
                        os.remove(filepath)
                        total_cleaned += 1
                        print(f"清理过期缓存: {filename} (已{file_age_hours:.1f}小时)")

                # 如果总大小超过限制，清理最旧的文件
                if info["total_size_mb"] > max_total_size_mb:
                    files = []
                    for filename in os.listdir(cache_dir):
                        filepath = os.path.join(cache_dir, filename)
                        if filename == "cache_index.json" or not os.path.isfile(filepath):
                            continue
                        files.append((filepath, os.path.getmtime(filepath)))
                    files.sort(key=lambda x: x[1])  # 按修改时间排序，最旧的在前

                    while info["total_size_mb"] > max_total_size_mb and files:
                        filepath, _ = files.pop(0)
                        file_size_mb = os.path.getsize(filepath) / (1024 * 1024)
                        os.remove(filepath)
                        info["total_size_mb"] -= file_size_mb
                        total_cleaned += 1
                        print(f"清理超限缓存: {os.path.basename(filepath)}")

            if total_cleaned > 0:
                print(f"缓存清理完成，共清理 {total_cleaned} 个文件")
            return total_cleaned

        except Exception as e:
            print(f"清理缓存失败: {e}")
            return 0

    def get_disk_usage(self):
        """获取磁盘使用信息"""
        try:
            import shutil
            total, used, free = shutil.disk_usage(self.data_dir)
            return {
                "total_gb": total / (1024 ** 3),
                "used_gb": used / (1024 ** 3),
                "free_gb": free / (1024 ** 3),
                "usage_percent": (used / total) * 100
            }
        except Exception as e:
            print(f"获取磁盘使用信息失败: {e}")
            return {}

    def delete_cache_file(self, file_path, image_type):
        """删除指定缓存文件及其索引"""
        try:
            if os.path.exists(file_path):
                file_hash = get_file_hash(file_path)
                os.remove(file_path)
                print(f"已删除缓存文件: {file_path}")

                # 同时清理索引
                if file_hash:
                    cache_index_path = self._cache_index_path(image_type)
                    if os.path.exists(cache_index_path):
                        try:
                            with open(cache_index_path, 'r', encoding='utf-8') as f:
                                cache_index = json.load(f)
                            if file_hash in cache_index:
                                del cache_index[file_hash]
                                with open(cache_index_path, 'w', encoding='utf-8') as f:
                                    json.dump(cache_index, f, ensure_ascii=False, indent=2)
                        except:
                            pass
                return True
            return False
        except Exception as e:
            print(f"删除缓存文件失败: {e}")
            return False

    def get_cache_files_detailed(self):
        """获取所有缓存文件详细信息"""
        try:
            cache_info = self.get_cache_info()
            all_files = []
            for image_type, info in cache_info.items():
                cache_dir = info["path"]
                if not os.path.exists(cache_dir):
                    continue
                cache_index_path = self._cache_index_path(image_type)
                cache_index = {}
                if os.path.exists(cache_index_path):
                    try:
                        with open(cache_index_path, 'r', encoding='utf-8') as f:
                            cache_index = json.load(f)
                    except:
                        pass

                for filename in sorted(os.listdir(cache_dir)):
                    filepath = os.path.join(cache_dir, filename)
                    if filename == "cache_index.json" or not os.path.isfile(filepath):
                        continue
                    stat = os.stat(filepath)
                    file_hash = filename.split('_')[-3] if '_' in filename else None
                    all_files.append({
                        "image_type": image_type,
                        "filename": filename,
                        "path": filepath,
                        "size_mb": stat.st_size / (1024 * 1024),
                        "modified": datetime.fromtimestamp(stat.st_mtime).isoformat(),
                        "in_index": file_hash in cache_index if file_hash else False
                    })
            return all_files
        except Exception as e:
            print(f"获取缓存文件详情失败: {e}")
            return []
