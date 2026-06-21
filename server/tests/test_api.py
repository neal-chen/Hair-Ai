"""API 端点集成测试（使用内存数据库）"""

from auth import ADMIN_API_KEY

AUTH_HEADER = {"Authorization": f"Bearer {ADMIN_API_KEY}"}


class TestHealth:
    def test_health_ok(self, client):
        resp = client.get("/api/health")
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "ok"
        assert data["service"] == "hair-library-api"

    def test_favicon(self, client):
        resp = client.get("/favicon.ico")
        assert resp.status_code == 200
        assert resp.headers["content-type"] == "image/svg+xml"


class TestHairstyles:
    def test_list_empty(self, client):
        """数据库为空时返回空列表"""
        resp = client.get("/api/hairstyles?version=0&device_id=test")
        assert resp.status_code == 200
        data = resp.json()
        assert data["success"] is True
        assert data["data"]["hairstyles"] == []

    def test_list_with_data(self, seeded_client):
        resp = seeded_client.get(
            "/api/hairstyles?gender=%E5%A5%B3&version=0&device_id=test"
        )
        assert resp.status_code == 200
        data = resp.json()
        assert len(data["data"]["hairstyles"]) >= 1

    def test_detail_found(self, seeded_client):
        resp = seeded_client.get("/api/hairstyles/test-h-001")
        assert resp.status_code == 200
        assert resp.json()["data"]["name"] == "测试发型"

    def test_detail_not_found(self, client):
        resp = client.get("/api/hairstyles/not-exist")
        assert resp.status_code == 404
        assert resp.json()["success"] is False

    def test_search_by_name(self, seeded_client):
        resp = seeded_client.get(
            "/api/hairstyles?q=%E6%B5%8B%E8%AF%95&version=0&device_id=test"
        )
        assert resp.status_code == 200
        assert len(resp.json()["data"]["hairstyles"]) >= 1

    def test_create_requires_auth(self, client):
        resp = client.post("/api/hairstyles", json={
            "name": "x", "category": "韩式", "gender": "女"
        })
        assert resp.status_code == 401

    def test_create_success(self, seeded_client):
        resp = seeded_client.post(
            "/api/hairstyles",
            json={"name": "新发型", "category": "韩式", "gender": "女"},
            headers=AUTH_HEADER,
        )
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert data["name"] == "新发型"
        assert data["version"] == 2  # version 递增

    def test_update(self, seeded_client):
        resp = seeded_client.put(
            "/api/hairstyles/test-h-001",
            json={"description": "已更新"},
            headers=AUTH_HEADER,
        )
        assert resp.status_code == 200
        assert resp.json()["data"]["description"] == "已更新"

    def test_soft_delete(self, seeded_client):
        resp = seeded_client.delete(
            "/api/hairstyles/test-h-001", headers=AUTH_HEADER
        )
        assert resp.status_code == 200

        # 删除后不可见
        resp = seeded_client.get("/api/hairstyles/test-h-001")
        assert resp.status_code == 404

    def test_incremental_sync(self, seeded_client):
        """version=1 时返回空（种子数据 version=1）"""
        resp = seeded_client.get("/api/hairstyles?version=1&device_id=test")
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert len(data["hairstyles"]) == 0


class TestHairColors:
    def test_list_empty(self, client):
        resp = client.get("/api/hair-colors?version=0&device_id=test")
        assert resp.status_code == 200
        assert resp.json()["data"]["colors"] == []

    def test_list_with_data(self, seeded_client):
        resp = seeded_client.get("/api/hair-colors?version=0&device_id=test")
        assert len(resp.json()["data"]["colors"]) >= 1

    def test_detail_found(self, seeded_client):
        resp = seeded_client.get("/api/hair-colors/test-c-001")
        assert resp.json()["data"]["name"] == "测试发色"

    def test_create_requires_auth(self, client):
        resp = client.post("/api/hair-colors", json={
            "name": "x", "category": "测试色系"
        })
        assert resp.status_code == 401

    def test_create_success(self, seeded_client):
        resp = seeded_client.post(
            "/api/hair-colors",
            json={"name": "新发色", "category": "测试色系", "color_hex": "#FF0000"},
            headers=AUTH_HEADER,
        )
        assert resp.status_code == 200
        assert resp.json()["data"]["color_hex"] == "#FF0000"


class TestAdmin:
    def test_stats_requires_auth(self, client):
        resp = client.get("/api/admin/stats")
        assert resp.status_code == 401

    def test_stats_with_auth(self, seeded_client):
        resp = seeded_client.get("/api/admin/stats", headers=AUTH_HEADER)
        assert resp.status_code == 200
        data = resp.json()["data"]
        assert "hairstyle_count" in data
        assert "hair_color_count" in data

    def test_devices_empty(self, seeded_client):
        resp = seeded_client.get("/api/admin/devices", headers=AUTH_HEADER)
        assert resp.status_code == 200
        assert resp.json()["data"]["devices"] == []


class TestSync:
    def test_report_sync(self, client):
        resp = client.post("/api/library/sync", json={
            "device_id": "test-device",
            "device_name": "测试平板",
            "hairstyle_version": 5,
            "color_version": 3,
        })
        assert resp.status_code == 200
        assert resp.json()["message"] == "sync recorded"

    def test_sync_public_no_auth(self, client):
        """同步上报不需要认证"""
        resp = client.post("/api/library/sync", json={
            "device_id": "pub-device", "hairstyle_version": 1, "color_version": 1,
        })
        assert resp.status_code == 200


class TestErrorHandling:
    def test_404_format(self, client):
        """不存在的路由返回统一错误格式"""
        resp = client.get("/api/hairstyles/not-exist")
        body = resp.json()
        assert body == {"success": False, "message": "发型不存在"}

    def test_401_format(self, client):
        resp = client.get("/api/admin/stats")
        body = resp.json()
        assert body["success"] is False
        assert "message" in body

    def test_invalid_input(self, seeded_client):
        """缺少必填字段"""
        resp = seeded_client.post(
            "/api/hairstyles",
            json={"name": "x"},  # 缺少 category 和 gender
            headers=AUTH_HEADER,
        )
        assert resp.status_code == 422


class TestAdminRedirect:
    def test_admin_redirect(self, client):
        resp = client.get("/admin", follow_redirects=False)
        assert resp.status_code == 307
        assert "/static/admin/index.html" in resp.headers["location"]

    def test_root_redirect(self, client):
        resp = client.get("/", follow_redirects=False)
        assert resp.status_code == 307
