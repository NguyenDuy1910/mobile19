from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from auth.dependencies import get_current_user
from auth.models import User
from database.session import get_db
from notifications.schemas import (
    DeviceRegistration,
    DeviceResponse,
    NotificationResponse,
    NotificationSettingsResponse,
    NotificationSettingsUpdate,
)
from notifications.service import create_test_notification, register_device, update_settings

router = APIRouter(prefix="/notifications", tags=["notifications"])


@router.post("/register-device", response_model=DeviceResponse, status_code=201)
def post_device(
    payload: DeviceRegistration,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> DeviceResponse:
    return register_device(db, user, payload)


@router.patch("/settings", response_model=NotificationSettingsResponse)
def patch_settings(
    payload: NotificationSettingsUpdate,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> NotificationSettingsResponse:
    return update_settings(db, user, payload)


@router.post("/test", response_model=NotificationResponse, status_code=201)
def test_notification(
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> NotificationResponse:
    return create_test_notification(db, user)
