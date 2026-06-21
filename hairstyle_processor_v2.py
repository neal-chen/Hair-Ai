"""
发型 / 发色 AI 处理引擎

⚠️ 此文件已重构为入口点，实际实现在 processor/ 包中。

使用方法：
    python3 hairstyle_processor_v2.py

或从其他模块导入：
    from processor import HairstyleProcessor
"""

import os
import random

from processor import HairstyleProcessor


def main():
    """批量跑发色换装任务（示例入口）"""
    hair_base_path = "/Users/alex_wu/work/hair"
    random.seed(42)

    processor = HairstyleProcessor(max_workers=1, task_timeout=600)

    # 批量跑发色换装
    user_dir_for_color = "/Users/alex_wu/work/hair/woman/wanghong"
    color_dir = "/Users/alex_wu/work/hair/color"
    if os.path.exists(user_dir_for_color) and os.path.exists(color_dir):
        print("Starting color transfer processing...")
        processor.process_color_folder(user_dir_for_color, color_dir)

    # 输出统计
    processor.get_average_task_time()


if __name__ == "__main__":
    main()
