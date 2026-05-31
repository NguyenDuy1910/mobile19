from sqlalchemy import select
from sqlalchemy.orm import Session

from auth.models import User
from notifications.models import Notification, NotificationDevice
from notifications.schemas import DeviceRegistration, NotificationSettingsResponse, NotificationSettingsUpdate
from shared.datetime import utc_now
from users.service import get_profile


def register_device(db: Session, user: User, payload: DeviceRegistration) -> NotificationDevice:
    device = db.scalar(select(NotificationDevice).where(NotificationDevice.device_token == payload.device_token))
    if device:
        device.user_id = user.id
        device.platform = payload.platform
        device.is_active = True
    else:
        device = NotificationDevice(user_id=user.id, **payload.model_dump())
        db.add(device)
    db.commit()
    db.refresh(device)
    return device


def update_settings(
    db: Session,
    user: User,
    payload: NotificationSettingsUpdate,
) -> NotificationSettingsResponse:
    profile = get_profile(db, user)
    for field, value in payload.model_dump(exclude_unset=True).items():
        setattr(profile, field, value)
    db.commit()
    return NotificationSettingsResponse(
        notifications_enabled=profile.notifications_enabled,
        notification_time=profile.notification_time,
        timezone=profile.timezone,
    )


def create_test_notification(db: Session, user: User) -> Notification:
    notification = Notification(
        user_id=user.id,
        type="test",
        title="MinLish test reminder",
        body="Your vocabulary reminder is configured.",
        scheduled_at=utc_now(),
        status="pending",
    )
    db.add(notification)
    db.commit()
    db.refresh(notification)
    return notification
