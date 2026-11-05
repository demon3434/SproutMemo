from typing import Any, Dict
from fastapi.responses import JSONResponse


def make_response(code: int, msg: str, data: Any = None, status_code: int = 200) -> JSONResponse:
    """Helper to construct standardized envelope response: {code, msg, data}"""
    return JSONResponse(
        status_code=status_code,
        content={
            "code": code,
            "msg": msg,
            "data": data
        }
    )


def ok(data: Any = None, msg: str = "success") -> Dict[str, Any]:
    """Standard success dict format for FastAPI route returns"""
    return {
        "code": 200,
        "msg": msg,
        "data": data
    }


def fail(code: int, msg: str, data: Any = None) -> Dict[str, Any]:
    """Standard failure dict format for FastAPI route returns"""
    return {
        "code": code,
        "msg": msg,
        "data": data
    }
