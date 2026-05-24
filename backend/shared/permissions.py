from uuid import UUID

from shared.errors import forbidden


def ensure_owner(resource_user_id: UUID, current_user_id: UUID) -> None:
    if resource_user_id != current_user_id:
        raise forbidden()
