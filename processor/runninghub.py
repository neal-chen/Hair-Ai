"""RunningHub API 客户端

提供与 RunningHub API 的交互：
- 图片上传
- 换发型 / 换发色 / 3D / 发色预处理 任务提交
- 任务状态轮询与结果获取
"""

import http.client
import json
import mimetypes
import os
import time
import threading
from codecs import encode


class RunningHubClient:
    """RunningHub API 客户端，封装所有 API 调用"""

    def __init__(self, host, api_key):
        self.host = host
        self.api_key = api_key

        # 任务统计
        self.task_times = []
        self.task_count = 0
        self.lock = threading.Lock()

    # ── 统计 ──

    def _record_task_time(self, elapsed):
        with self.lock:
            self.task_times.append(elapsed)
            self.task_count += 1

    def get_stats(self):
        with self.lock:
            if not self.task_times:
                return {"count": 0, "avg": 0, "min": 0, "max": 0}
            return {
                "count": self.task_count,
                "avg": sum(self.task_times) / len(self.task_times),
                "min": min(self.task_times),
                "max": max(self.task_times),
            }

    # ── 图片上传 ──

    def upload_image(self, image_path):
        """上传图片到 RunningHub 服务器，返回 fileName"""
        conn = http.client.HTTPSConnection(self.host)
        boundary = 'wL36Yn8afVp8Ag7AmP8qZ0SA4n1v9T'
        dataList = []

        dataList.append(encode('--' + boundary))
        dataList.append(encode('Content-Disposition: form-data; name="apiKey"'))
        dataList.append(encode('Content-Type: text/plain'))
        dataList.append(encode(''))
        dataList.append(encode(self.api_key))

        dataList.append(encode('--' + boundary))
        filename = os.path.basename(image_path)
        dataList.append(encode(f'Content-Disposition: form-data; name="file"; filename="{filename}"'))
        fileType = mimetypes.guess_type(image_path)[0] or 'application/octet-stream'
        dataList.append(encode(f'Content-Type: {fileType}'))
        dataList.append(encode(''))
        with open(image_path, 'rb') as f:
            dataList.append(f.read())

        dataList.append(encode('--' + boundary))
        dataList.append(encode('Content-Disposition: form-data; name="fileType"'))
        dataList.append(encode('Content-Type: text/plain'))
        dataList.append(encode(''))
        dataList.append(encode("image"))
        dataList.append(encode('--' + boundary + '--'))
        dataList.append(encode(''))

        body = b'\r\n'.join(dataList)
        headers = {
            'Host': self.host,
            'Content-type': f'multipart/form-data; boundary={boundary}'
        }

        try:
            conn.request("POST", "/task/openapi/upload", body, headers)
            res = conn.getresponse()
            data = res.read()
            result = json.loads(data.decode("utf-8"))
            if result.get("code") == 0:
                print(f"上传成功: {image_path} → {result['data']['fileName']}")
                return result["data"]["fileName"]
            else:
                print(f"上传失败: {image_path} → {result}")
                return None
        except Exception as e:
            print(f"上传出错: {image_path} → {e}")
            return None
        finally:
            conn.close()

    # ── 通用任务提交（含重试+队列满处理）──

    def _submit_task(self, payload, task_name, max_retries=10, retry_delay=20, cancel_check_func=None):
        """通用任务提交，含 TASK_QUEUE_MAXED 重试"""
        start_time = time.time()
        headers = {'Host': self.host, 'Content-Type': 'application/json'}

        for attempt in range(max_retries):
            if cancel_check_func and cancel_check_func():
                print(f"[{task_name}] 任务在排队阶段被取消 (attempt {attempt + 1}/{max_retries})")
                return None

            conn = http.client.HTTPSConnection(self.host)
            try:
                conn.request("POST", "/task/openapi/ai-app/run", payload, headers)
                res = conn.getresponse()
                data = res.read()
                result = json.loads(data.decode("utf-8"))

                if result.get("code") == 0:
                    elapsed = time.time() - start_time
                    self._record_task_time(elapsed)
                    print(f"[{task_name}] 任务启动成功: {result['data']['taskId']} (耗时: {elapsed:.2f}秒)")
                    return result["data"]["taskId"]

                msg = result.get("msg", "")
                if msg in ("TASK_QUEUE_MAXED", "TASK_INSTANCE_MAXED"):
                    print(f"[{task_name}] 队列已满 (attempt {attempt + 1}/{max_retries}), 等待 {retry_delay}s 重试...")
                    if attempt < max_retries - 1:
                        for i in range(retry_delay):
                            if cancel_check_func and cancel_check_func():
                                print(f"[{task_name}] 等待重试期间被取消")
                                return None
                            time.sleep(1)
                        continue
                    else:
                        elapsed = time.time() - start_time
                        self._record_task_time(elapsed)
                        print(f"[{task_name}] 达到最大重试次数，队列仍满 (总耗时: {elapsed:.2f}秒)")
                        return None
                else:
                    elapsed = time.time() - start_time
                    self._record_task_time(elapsed)
                    print(f"[{task_name}] 任务失败: {result}")
                    return None
            except Exception as e:
                elapsed = time.time() - start_time
                self._record_task_time(elapsed)
                print(f"[{task_name}] 请求异常 (attempt {attempt + 1}/{max_retries}): {e}")
                if attempt < max_retries - 1:
                    time.sleep(retry_delay)
                    continue
                return None
            finally:
                conn.close()
        return None

    # ── 换发型 ──

    def run_hairstyle_task(self, webapp_id, hairstyle_hair_node_id, hairstyle_user_node_id,
                           hairstyle_filename, user_filename, max_retries=10, retry_delay=20,
                           cancel_check_func=None):
        """提交换发型任务"""
        payload = json.dumps({
            "webappId": webapp_id,
            "apiKey": self.api_key,
            "nodeInfoList": [
                {"nodeId": hairstyle_hair_node_id, "fieldName": "image",
                 "fieldValue": hairstyle_filename, "description": "hair"},
                {"nodeId": hairstyle_user_node_id, "fieldName": "image",
                 "fieldValue": user_filename, "description": "user"}
            ],
            "instanceType": "plus",
            "usePersonalQueue": "true"
        })
        return self._submit_task(payload, "换发型", max_retries, retry_delay, cancel_check_func)

    # ── 换发色 ──

    def run_color_task(self, color_webapp_id, color_user_node_id, color_hair_node_id,
                       hair_filename, user_filename, max_retries=10, retry_delay=20,
                       cancel_check_func=None):
        """提交换发色任务"""
        if not color_webapp_id:
            raise ValueError("需要设置 RUNNINGHUB_COLOR_WEBAPP_ID 环境变量")
        payload = json.dumps({
            "webappId": color_webapp_id,
            "apiKey": self.api_key,
            "nodeInfoList": [
                {"nodeId": color_user_node_id, "fieldName": "image",
                 "fieldValue": user_filename, "description": "user"},
                {"nodeId": color_hair_node_id, "fieldName": "image",
                 "fieldValue": hair_filename, "description": "hair"}
            ],
            "instanceType": "plus",
            "usePersonalQueue": "true"
        })
        return self._submit_task(payload, "换发色", max_retries, retry_delay, cancel_check_func)

    # ── 3D 视频 (RunningHub) ──

    def run_3d_task(self, webapp_3d_id, user_filename, max_retries=10, retry_delay=20,
                    cancel_check_func=None):
        """提交 3D 视频生成任务"""
        payload = json.dumps({
            "webappId": webapp_3d_id,
            "apiKey": self.api_key,
            "nodeInfoList": [
                {"nodeId": "96", "fieldName": "image",
                 "fieldValue": user_filename, "description": "user"}
            ],
            "instanceType": "plus",
            "usePersonalQueue": "true"
        })
        return self._submit_task(payload, "3D视频", max_retries, retry_delay, cancel_check_func)

    # ── 发色预处理 ──

    def run_color_preprocess_task(self, color_pre_webapp_id, image_filename,
                                  max_retries=10, retry_delay=20, cancel_check_func=None):
        """运行发色预处理任务"""
        if not color_pre_webapp_id:
            print("RUNNINGHUB_COLOR_PRE_WEBAPP_ID 未设置，跳过发色预处理")
            return None

        start_time = time.time()
        payload = json.dumps({
            "webappId": color_pre_webapp_id,
            "apiKey": self.api_key,
            "nodeInfoList": [
                {"nodeId": "19", "fieldName": "image",
                 "fieldValue": image_filename, "description": "image"},
                {"nodeId": "33", "fieldName": "text",
                 "fieldValue": "hair color process", "description": "text"}
            ],
        })
        headers = {'Host': self.host, 'Content-Type': 'application/json'}

        for attempt in range(max_retries):
            if cancel_check_func and cancel_check_func():
                print(f"发色预处理在排队阶段被取消 (attempt {attempt + 1}/{max_retries})")
                return None

            conn = http.client.HTTPSConnection(self.host)
            try:
                conn.request("POST", "/task/openapi/ai-app/run", payload, headers)
                res = conn.getresponse()
                data = res.read()
                result = json.loads(data.decode("utf-8"))

                if result.get("code") == 0:
                    elapsed = time.time() - start_time
                    print(f"发色预处理任务启动成功: {result['data']['taskId']} (耗时: {elapsed:.2f}秒)")
                    return result["data"]["taskId"]
                elif result.get("msg") in ("TASK_QUEUE_MAXED", "TASK_INSTANCE_MAXED"):
                    print(f"发色预处理队列满 (attempt {attempt + 1}/{max_retries}), 等待 {retry_delay}s...")
                    if attempt < max_retries - 1:
                        for i in range(retry_delay):
                            if cancel_check_func and cancel_check_func():
                                return None
                            time.sleep(1)
                        continue
                    return None
                else:
                    print(f"发色预处理失败: {result}")
                    return None
            except Exception as e:
                print(f"发色预处理出错 (attempt {attempt + 1}/{max_retries}): {e}")
                if attempt < max_retries - 1:
                    time.sleep(retry_delay)
                    continue
                return None
            finally:
                conn.close()
        return None

    def call_runninghub_color_preprocess(self, color_pre_webapp_id, image_filename):
        """调用发色预处理（非阻塞，直接获取结果）"""
        if not color_pre_webapp_id:
            return None
        payload = json.dumps({
            "webappId": color_pre_webapp_id,
            "apiKey": self.api_key,
            "nodeInfoList": [
                {"nodeId": "19", "fieldName": "image",
                 "fieldValue": image_filename, "description": "image"},
                {"nodeId": "33", "fieldName": "text",
                 "fieldValue": "hair color process", "description": "text"}
            ],
        })
        headers = {'Host': self.host, 'Content-Type': 'application/json'}
        conn = http.client.HTTPSConnection(self.host)
        try:
            conn.request("POST", "/task/openapi/ai-app/run", payload, headers)
            res = conn.getresponse()
            data = res.read()
            result = json.loads(data.decode("utf-8"))
            if result.get("code") == 0:
                return result["data"]["taskId"]
            print(f"发色预处理调用失败: {result}")
            return None
        except Exception as e:
            print(f"发色预处理调用出错: {e}")
            return None
        finally:
            conn.close()

    # ── 任务状态 / 结果 / 取消 ──

    def check_task_status(self, task_id):
        """查询任务状态"""
        payload = json.dumps({"apiKey": self.api_key, "taskId": task_id})
        headers = {'Host': self.host, 'Content-Type': 'application/json'}
        conn = http.client.HTTPSConnection(self.host)
        try:
            conn.request("POST", "/task/openapi/ai-app/status", payload, headers)
            res = conn.getresponse()
            data = res.read()
            result = json.loads(data.decode("utf-8"))
            if result.get("code") == 0:
                return result["data"]["status"]
            print(f"查询任务状态失败: {result}")
            return None
        except Exception as e:
            print(f"查询任务状态出错: {e}")
            return None
        finally:
            conn.close()

    def get_task_results(self, task_id):
        """获取任务结果列表"""
        payload = json.dumps({"apiKey": self.api_key, "taskId": task_id})
        headers = {'Host': self.host, 'Content-Type': 'application/json'}
        conn = http.client.HTTPSConnection(self.host)
        try:
            conn.request("POST", "/task/openapi/ai-app/result", payload, headers)
            res = conn.getresponse()
            data = res.read()
            result = json.loads(data.decode("utf-8"))
            if result.get("code") == 0:
                return result["data"]["nodeInfoList"]
            print(f"获取任务结果失败: {result}")
            return None
        except Exception as e:
            print(f"获取任务结果出错: {e}")
            return None
        finally:
            conn.close()

    def cancel_task(self, task_id):
        """取消任务"""
        payload = json.dumps({"apiKey": self.api_key, "taskId": task_id})
        headers = {'Host': self.host, 'Content-Type': 'application/json'}
        conn = http.client.HTTPSConnection(self.host)
        try:
            conn.request("POST", "/task/openapi/ai-app/cancel", payload, headers)
            res = conn.getresponse()
            data = res.read()
            result = json.loads(data.decode("utf-8"))
            if result.get("code") == 0:
                print(f"任务已取消: {task_id}")
                return True
            print(f"取消任务失败: {result}")
            return False
        except Exception as e:
            print(f"取消任务出错: {e}")
            return False
        finally:
            conn.close()
