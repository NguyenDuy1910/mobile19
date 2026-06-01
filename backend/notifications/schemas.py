from datetime import datetime, time
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class DeviceRegistration(BaseModel):
    platform: Literal["android", "ios"]
    device_token: str = Field(min_length=1)


class NotificationSettingsUpdate(BaseModel):
    notifications_enabled: bool | None = None
    notification_time: time | None = None
    timezone: str | None = Field(default=None, min_length=1, max_length=80)


class NotificationSettingsResponse(BaseModel):
    notifications_enabled: bool
    notification_time: time | None
    timezone: str


class DeviceResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    platform: str
    device_token: str
    is_active: bool


class NotificationResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    type: str
    title: str
    body: str
    scheduled_at: datetime
    status: str
    message: str = "Test notification simulated; a notification record was created."
