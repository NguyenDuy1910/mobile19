from fastapi import HTTPException, status


def api_error(status_code: int, code: str, message: str) -> HTTPException:
    return HTTPException(status_code=status_code, detail={"code": code, "message": message})


def not_found(message: str = "Resource not found") -> HTTPException:
    return api_error(status.HTTP_404_NOT_FOUND, "NOT_FOUND", message)


def forbidden(message: str = "You do not have access to this resource") -> HTTPException:
    return api_error(status.HTTP_403_FORBIDDEN, "FORBIDDEN", message)


def bad_request(message: str = "Invalid request", code: str = "VALIDATION_ERROR") -> HTTPException:
    return api_error(status.HTTP_400_BAD_REQUEST, code, message)
