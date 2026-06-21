"""日志配置模块

统一日志输出格式，支持开发和生产环境切换。
"""

import logging
import os
import sys

LOG_LEVEL = os.environ.get("LOG_LEVEL", "INFO").upper()


def setup_logging(name: str = "hair-api") -> logging.Logger:
    """初始化并返回命名 logger"""
    logger = logging.getLogger(name)
    logger.setLevel(LOG_LEVEL)

    # 避免重复添加 handler
    if logger.handlers:
        return logger

    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(LOG_LEVEL)

    formatter = logging.Formatter(
        fmt="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )
    handler.setFormatter(formatter)
    logger.addHandler(handler)

    return logger
