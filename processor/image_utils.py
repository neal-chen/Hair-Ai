"""图像处理工具函数

提供 EXIF 方向修正、Base64 编解码、哈希计算等图像操作。
"""

import base64
import hashlib
import io
import os

from PIL import Image, ExifTags


def encode_image(image_path):
    """将图像编码为base64字符串，自动处理EXIF方向"""
    try:
        with Image.open(image_path) as img:
            img = fix_image_orientation(img)
            if img.mode != 'RGB':
                img = img.convert('RGB')
            buffer = io.BytesIO()
            img.save(buffer, format='JPEG', quality=95)
            buffer.seek(0)
            return base64.b64encode(buffer.getvalue()).decode('utf-8')
    except Exception as e:
        print(f"处理图像EXIF方向失败，使用原始方法: {e}")
        with open(image_path, "rb") as image_file:
            return base64.b64encode(image_file.read()).decode('utf-8')


def fix_image_orientation(img):
    """根据EXIF信息修正图像方向"""
    try:
        from PIL import ImageOps
        img = ImageOps.exif_transpose(img)
        return img
    except ImportError:
        try:
            exif = img._getexif()
            if exif is not None:
                for tag, value in exif.items():
                    if ExifTags.TAGS.get(tag) == 'Orientation':
                        if value == 2:
                            img = img.transpose(Image.FLIP_LEFT_RIGHT)
                        elif value == 3:
                            img = img.rotate(180, expand=True)
                        elif value == 4:
                            img = img.transpose(Image.FLIP_TOP_BOTTOM)
                        elif value == 5:
                            img = img.transpose(Image.FLIP_LEFT_RIGHT).rotate(90, expand=True)
                        elif value == 6:
                            img = img.rotate(-90, expand=True)
                        elif value == 7:
                            img = img.transpose(Image.FLIP_LEFT_RIGHT).rotate(-90, expand=True)
                        elif value == 8:
                            img = img.rotate(90, expand=True)
                        break
        except Exception as e:
            print(f"修正图像方向失败: {e}")
    return img


def get_file_hash(file_path):
    """计算文件的MD5哈希值"""
    hash_md5 = hashlib.md5()
    try:
        with open(file_path, "rb") as f:
            for chunk in iter(lambda: f.read(4096), b""):
                hash_md5.update(chunk)
        return hash_md5.hexdigest()
    except Exception as e:
        print(f"计算文件哈希失败: {e}")
        return None


def save_image_from_base64(base64_str, data_dir, original_path, image_type, file_hash, update_cache_callback=None):
    """从base64字符串还原图片并保存，使用哈希值命名"""
    try:
        output_dir = os.path.join(data_dir, f"gemini_processed_{image_type}")
        if not os.path.exists(output_dir):
            os.makedirs(output_dir, exist_ok=True)

        original_filename = os.path.basename(original_path)
        name_without_ext = os.path.splitext(original_filename)[0]
        new_filename = f"{name_without_ext}_{file_hash[:8]}_gemini_processed.png"
        filepath = os.path.join(output_dir, new_filename)

        image_data = base64.b64decode(base64_str)
        with open(filepath, "wb") as f:
            f.write(image_data)

        if update_cache_callback:
            update_cache_callback(original_path, filepath, file_hash, image_type)

        return filepath
    except Exception as e:
        print(f"保存图片时出错: {e}")
        return None


def download_image(url, save_path):
    """从URL下载图片"""
    import requests
    try:
        response = requests.get(url)
        if response.status_code == 200:
            with open(save_path, 'wb') as f:
                f.write(response.content)
            return True
        else:
            print(f"Failed to download {url}")
            return False
    except Exception as e:
        print(f"Error downloading {url}: {e}")
        return False


def resize_image_for_word(image_path, max_width=2.5):
    """调整图片尺寸以适应Word文档"""
    try:
        with Image.open(image_path) as img:
            width, height = img.size
            aspect_ratio = height / width
            if width > max_width * 96:  # 96 DPI default
                new_width = max_width
                new_height = new_width * aspect_ratio
                return new_width, new_height
            else:
                return width / 96, height / 96
    except:
        return max_width, max_width


def create_combined_image(hairstyle_path, user_path, result_paths, output_path):
    """创建合成图：发型参考 + 用户照片 + 所有生成结果并排"""
    try:
        hairstyle_img = Image.open(hairstyle_path)
        user_img = Image.open(user_path)

        result_imgs = []
        for result_path in result_paths:
            if os.path.exists(result_path):
                result_imgs.append(Image.open(result_path))

        all_imgs = [hairstyle_img, user_img] + result_imgs

        for i, img in enumerate(all_imgs):
            if img.mode != 'RGB':
                all_imgs[i] = img.convert('RGB')

        target_height = max(512, min(img.height for img in all_imgs))

        def resize_to_height(img, target_height):
            aspect_ratio = img.width / img.height
            target_width = int(target_height * aspect_ratio)
            return img.resize((target_width, target_height), Image.Resampling.LANCZOS)

        resized_imgs = [resize_to_height(img, target_height) for img in all_imgs]
        total_width = sum(img.width for img in resized_imgs)

        combined_img = Image.new('RGB', (total_width, target_height), (255, 255, 255))
        x_offset = 0
        for img in resized_imgs:
            combined_img.paste(img, (x_offset, 0))
            x_offset += img.width

        combined_img.save(output_path, 'PNG', quality=95)
        print(f"合成图已保存: {output_path}")
        return True
    except Exception as e:
        print(f"创建合成图失败: {e}")
        return False
