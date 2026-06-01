"""initial MinLish MVP schema

Revision ID: 20260531_0001
Revises:
Create Date: 2026-05-31
"""
from collections.abc import Sequence

from alembic import op
import sqlalchemy as sa

revision: str = "20260531_0001"
down_revision: str | None = None
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

UUID = sa.Uuid()
TZ_DATETIME = sa.DateTime(timezone=True)


def upgrade() -> None:
    op.create_table(
        "users",
        sa.Column("email", sa.String(320), nullable=False),
        sa.Column("password_hash", sa.String(255), nullable=False),
        sa.Column("provider", sa.String(32), nullable=False),
        sa.Column("id", UUID, nullable=False),
        sa.Column("created_at", TZ_DATETIME, nullable=False),
        sa.Column("updated_at", TZ_DATETIME, nullable=False),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("email"),
    )
    op.create_index("ix_users_email", "users", ["email"])

    op.create_table(
        "user_profiles",
        sa.Column("user_id", UUID, nullable=False),
        sa.Column("name", sa.String(120), nullable=False),
        sa.Column("learning_goal", sa.String(120)),
        sa.Column("english_level", sa.String(20)),
        sa.Column("daily_new_words", sa.Integer(), nullable=False),
        sa.Column("daily_review_limit", sa.Integer(), nullable=False),
        sa.Column("notification_time", sa.Time()),
        sa.Column("timezone", sa.String(80), nullable=False),
        sa.Column("notifications_enabled", sa.Boolean(), nullable=False),
        sa.Column("id", UUID, nullable=False),
        sa.Column("created_at", TZ_DATETIME, nullable=False),
        sa.Column("updated_at", TZ_DATETIME, nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id"),
    )
    op.create_index("ix_user_profiles_user_id", "user_profiles", ["user_id"])

    op.create_table(
        "vocab_decks",
        sa.Column("user_id", UUID, nullable=False),
        sa.Column("name", sa.String(160), nullable=False),
        sa.Column("description", sa.Text()),
        sa.Column("tags", sa.JSON(), nullable=False),
        sa.Column("id", UUID, nullable=False),
        sa.Column("created_at", TZ_DATETIME, nullable=False),
        sa.Column("updated_at", TZ_DATETIME, nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_vocab_decks_user_id", "vocab_decks", ["user_id"])

    op.create_table(
        "vocab_items",
        sa.Column("deck_id", UUID, nullable=False),
        sa.Column("word", sa.String(160), nullable=False),
        sa.Column("pronunciation", sa.String(255)),
        sa.Column("meaning", sa.Text(), nullable=False),
        sa.Column("description_en", sa.Text()),
        sa.Column("example", sa.Text()),
        sa.Column("collocations", sa.JSON(), nullable=False),
        sa.Column("related_words", sa.JSON(), nullable=False),
        sa.Column("note", sa.Text()),
        sa.Column("part_of_speech", sa.String(80)),
        sa.Column("phonetic", sa.String(255)),
        sa.Column("audio_url", sa.Text()),
        sa.Column("synonyms", sa.JSON(), nullable=False),
        sa.Column("antonyms", sa.JSON(), nullable=False),
        sa.Column("source", sa.String(80), nullable=False),
        sa.Column("id", UUID, nullable=False),
        sa.Column("created_at", TZ_DATETIME, nullable=False),
        sa.Column("updated_at", TZ_DATETIME, nullable=False),
        sa.ForeignKeyConstraint(["deck_id"], ["vocab_decks.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_vocab_items_deck_id", "vocab_items", ["deck_id"])
    op.create_index("ix_vocab_items_word", "vocab_items", ["word"])

    op.create_table(
        "user_vocab_progress",
        sa.Column("user_id", UUID, nullable=False),
        sa.Column("vocab_item_id", UUID, nullable=False),
        sa.Column("status", sa.String(20), nullable=False),
        sa.Column("repetitions", sa.Integer(), nullable=False),
        sa.Column("interval_days", sa.Integer(), nullable=False),
        sa.Column("ease_factor", sa.Numeric(4, 2), nullable=False),
        sa.Column("due_at", TZ_DATETIME, nullable=False),
        sa.Column("last_reviewed_at", TZ_DATETIME),
        sa.Column("total_reviews", sa.Integer(), nullable=False),
        sa.Column("correct_count", sa.Integer(), nullable=False),
        sa.Column("wrong_count", sa.Integer(), nullable=False),
        sa.Column("id", UUID, nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["vocab_item_id"], ["vocab_items.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", "vocab_item_id"),
    )
    op.create_index("ix_user_vocab_progress_user_id", "user_vocab_progress", ["user_id"])
    op.create_index("ix_user_vocab_progress_vocab_item_id", "user_vocab_progress", ["vocab_item_id"])
    op.create_index("ix_user_vocab_progress_due_at", "user_vocab_progress", ["due_at"])

    op.create_table(
        "review_logs",
        sa.Column("user_id", UUID, nullable=False),
        sa.Column("vocab_item_id", UUID, nullable=False),
        sa.Column("rating", sa.String(20), nullable=False),
        sa.Column("old_interval", sa.Integer(), nullable=False),
        sa.Column("new_interval", sa.Integer(), nullable=False),
        sa.Column("old_ease_factor", sa.Numeric(4, 2), nullable=False),
        sa.Column("new_ease_factor", sa.Numeric(4, 2), nullable=False),
        sa.Column("reviewed_at", TZ_DATETIME, nullable=False),
        sa.Column("id", UUID, nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["vocab_item_id"], ["vocab_items.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_review_logs_user_id", "review_logs", ["user_id"])
    op.create_index("ix_review_logs_vocab_item_id", "review_logs", ["vocab_item_id"])
    op.create_index("ix_review_logs_reviewed_at", "review_logs", ["reviewed_at"])

    op.create_table(
        "learning_sessions",
        sa.Column("user_id", UUID, nullable=False),
        sa.Column("started_at", TZ_DATETIME, nullable=False),
        sa.Column("ended_at", TZ_DATETIME),
        sa.Column("new_words_count", sa.Integer(), nullable=False),
        sa.Column("review_words_count", sa.Integer(), nullable=False),
        sa.Column("correct_count", sa.Integer(), nullable=False),
        sa.Column("wrong_count", sa.Integer(), nullable=False),
        sa.Column("duration_seconds", sa.Integer()),
        sa.Column("id", UUID, nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_learning_sessions_user_id", "learning_sessions", ["user_id"])

    op.create_table(
        "practice_answers",
        sa.Column("user_id", UUID, nullable=False),
        sa.Column("vocab_item_id", UUID, nullable=False),
        sa.Column("question_type", sa.String(40), nullable=False),
        sa.Column("question", sa.Text(), nullable=False),
        sa.Column("user_answer", sa.Text(), nullable=False),
        sa.Column("correct_answer", sa.Text(), nullable=False),
        sa.Column("is_correct", sa.Boolean(), nullable=False),
        sa.Column("ai_feedback", sa.Text()),
        sa.Column("created_at", TZ_DATETIME, nullable=False),
        sa.Column("id", UUID, nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["vocab_item_id"], ["vocab_items.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_practice_answers_user_id", "practice_answers", ["user_id"])
    op.create_index("ix_practice_answers_vocab_item_id", "practice_answers", ["vocab_item_id"])
    op.create_index("ix_practice_answers_created_at", "practice_answers", ["created_at"])

    op.create_table(
        "notifications",
        sa.Column("user_id", UUID, nullable=False),
        sa.Column("type", sa.String(40), nullable=False),
        sa.Column("title", sa.String(160), nullable=False),
        sa.Column("body", sa.Text(), nullable=False),
        sa.Column("scheduled_at", TZ_DATETIME, nullable=False),
        sa.Column("sent_at", TZ_DATETIME),
        sa.Column("status", sa.String(20), nullable=False),
        sa.Column("id", UUID, nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_notifications_user_id", "notifications", ["user_id"])
    op.create_index("ix_notifications_scheduled_at", "notifications", ["scheduled_at"])

    op.create_table(
        "notification_devices",
        sa.Column("user_id", UUID, nullable=False),
        sa.Column("platform", sa.String(20), nullable=False),
        sa.Column("device_token", sa.Text(), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False),
        sa.Column("id", UUID, nullable=False),
        sa.Column("created_at", TZ_DATETIME, nullable=False),
        sa.Column("updated_at", TZ_DATETIME, nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("device_token"),
    )
    op.create_index("ix_notification_devices_user_id", "notification_devices", ["user_id"])

    op.create_table(
        "agent_conversations",
        sa.Column("user_id", UUID, nullable=False),
        sa.Column("title", sa.String(160)),
        sa.Column("id", UUID, nullable=False),
        sa.Column("created_at", TZ_DATETIME, nullable=False),
        sa.Column("updated_at", TZ_DATETIME, nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_agent_conversations_user_id", "agent_conversations", ["user_id"])

    op.create_table(
        "agent_messages",
        sa.Column("conversation_id", UUID, nullable=False),
        sa.Column("role", sa.String(20), nullable=False),
        sa.Column("content", sa.Text(), nullable=False),
        sa.Column("metadata", sa.JSON()),
        sa.Column("created_at", TZ_DATETIME, nullable=False),
        sa.Column("id", UUID, nullable=False),
        sa.ForeignKeyConstraint(["conversation_id"], ["agent_conversations.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_agent_messages_conversation_id", "agent_messages", ["conversation_id"])


def downgrade() -> None:
    op.drop_table("agent_messages")
    op.drop_table("agent_conversations")
    op.drop_table("notification_devices")
    op.drop_table("notifications")
    op.drop_table("practice_answers")
    op.drop_table("learning_sessions")
    op.drop_table("review_logs")
    op.drop_table("user_vocab_progress")
    op.drop_table("vocab_items")
    op.drop_table("vocab_decks")
    op.drop_table("user_profiles")
    op.drop_table("users")
