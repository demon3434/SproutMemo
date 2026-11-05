class AppException(Exception):
    """Base application exception for standardized custom error codes and messages"""
    def __init__(self, code: int = 400, message: str = "请求处理失败"):
        self.code = code
        self.message = message
        super().__init__(self.message)


class AuthException(AppException):
    """Authentication related exception (default code 401)"""
    def __init__(self, message: str = "未授权访问"):
        super().__init__(code=401, message=message)


class ForbiddenException(AppException):
    """Authorization permission denied exception (default code 403)"""
    def __init__(self, message: str = "权限不足，拒绝访问"):
        super().__init__(code=403, message=message)


class NotFoundException(AppException):
    """Resource not found exception (default code 404)"""
    def __init__(self, message: str = "请求的资源不存在"):
        super().__init__(code=404, message=message)


class ConflictException(AppException):
    """Data concurrency or validation conflict exception (default code 409)"""
    def __init__(self, message: str = "数据更新冲突"):
        super().__init__(code=409, message=message)
