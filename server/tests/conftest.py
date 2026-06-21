"""pytest 共享配置与 fixtures"""

import os
import sys
import tempfile

# 确保 server 目录在 Python 路径中
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from database import Base, get_db
from models import Hairstyle, HairColor
from main import app


# ── 临时内存数据库 ──

@pytest.fixture(scope="session")
def db_engine():
    """使用内存 SQLite 运行测试"""
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False})
    Base.metadata.create_all(bind=engine)
    return engine


@pytest.fixture
def db_session(db_engine):
    """每个测试独立的数据库会话"""
    connection = db_engine.connect()
    transaction = connection.begin()
    session = sessionmaker(bind=connection)()
    yield session
    session.close()
    transaction.rollback()
    connection.close()


@pytest.fixture
def client(db_session):
    """FastAPI TestClient，使用测试数据库"""

    def override_get_db():
        yield db_session

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()


# ── 种子数据 ──

@pytest.fixture
def seeded_db(db_session):
    """插入测试种子数据"""
    hs = Hairstyle(
        id="test-h-001", category="韩式", name="测试发型",
        description="测试描述", gender="女", tags='["卷发","韩式"]',
        sort_order=1, version=1,
    )
    hc = HairColor(
        id="test-c-001", category="温感色系", name="测试发色",
        color_hex="#C4925A", sort_order=1, version=1,
    )
    db_session.add_all([hs, hc])
    db_session.commit()
    return db_session


@pytest.fixture
def seeded_client(seeded_db):
    """带种子数据的 TestClient"""

    def override_get_db():
        yield seeded_db

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()
