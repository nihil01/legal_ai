# Legal AI Platform

Модульный монолит для RAG-ingestion юридических документов: защищённая загрузка, фоновое извлечение текста, очистка, распознавание структуры, юридический chunking, embeddings, PostgreSQL/pgvector и диагностический semantic search.

## Возможности MVP

- HTTP Basic Auth, credentials только через environment.
- DOC, DOCX, PDF, HTML и UTF-8 TXT до 25 MB.
- SHA-256 дедупликация и хранение оригиналов через `DocumentStorage`.
- Фоновая очередь PostgreSQL с `FOR UPDATE SKIP LOCKED`, retry, heartbeat, lease timeout и UUID fencing token против stale workers.
- AZ/RU маркеры: `Bölmə/Раздел`, `Fəsil/Глава`, `Maddə/Статья`, числовые пункты и подпункты.
- Chunks сохраняют parent path, article/clause, content, embedding content, token estimate и JSON metadata.
- pgvector `VECTOR(1536)` и cosine similarity search.
- Local deterministic lexical embeddings для запуска без внешнего API; OpenAI-compatible provider для production.
- Полноценная адаптивная Thymeleaf admin UI: dashboard, документы, drag-and-drop upload, status/chunks, reprocess/delete и semantic search.
- Голосовой ввод в semantic search через browser `MediaRecorder` и защищённый speech-to-text endpoint.
- OpenAI-compatible adapters для text generation и speech-to-text; без ключа функции остаются явно отключёнными.
- Quartz cron job для будущей синхронизации законов с e-qanun и подготовленный `.doc` parsing extension point.

## Быстрый запуск

```bash
cp .env.example .env
# Обязательно замените DATABASE_PASSWORD и ADMIN_PASSWORD

docker compose up -d --build
docker compose ps
```

Открыть dashboard: <http://127.0.0.1:8080/admin>

Браузер покажет стандартный Basic Auth dialog. Логин и пароль берутся из `.env`.

## Embedding providers

### Local development

```env
EMBEDDING_PROVIDER=local
```

Local provider создаёт нормализованные hashing vectors размерности 1536. Он позволяет полностью проверить ingestion и pgvector без секретов, но не заменяет качественную multilingual embedding model.

### OpenAI-compatible API

```env
EMBEDDING_PROVIDER=openai
EMBEDDING_BASE_URL=https://api.openai.com/v1
EMBEDDING_API_KEY=...
EMBEDDING_MODEL=text-embedding-3-small
EMBEDDING_CONNECT_TIMEOUT_SECONDS=10
EMBEDDING_READ_TIMEOUT_SECONDS=60
```

Модель обязана возвращать 1536 измерений: размер согласован с Flyway schema.

## OpenAI text generation и speech-to-text

Обе интеграции используют отдельный общий набор настроек и не зависят от выбранного embedding provider:

```env
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_API_KEY=...
OPENAI_TEXT_MODEL=gpt-4.1-mini
OPENAI_TRANSCRIPTION_MODEL=gpt-4o-mini-transcribe
OPENAI_CONNECT_TIMEOUT_SECONDS=10
OPENAI_READ_TIMEOUT_SECONDS=120
OPENAI_MAX_AUDIO_BYTES=10485760
```

Если `OPENAI_API_KEY` пуст, приложение продолжает запускаться. Вызов optional capability возвращает понятную ошибку `503`, а не падает при старте.

Extension points:

- `TextGenerationService` / `OpenAiTextGenerationService` — chat completion для будущего RAG answer generation;
- `SpeechToTextService` / `OpenAiSpeechToTextService` — multipart transcription;
- `POST /admin/api/transcriptions` — защищённый Basic Auth и CSRF endpoint для браузерной записи.

На `/admin/search` кнопка микрофона использует `getUserMedia` + `MediaRecorder`, автоматически останавливает запись через 60 секунд, отправляет WebM/Ogg/MP4 и добавляет распознанный текст в вопрос. Для доступа к микрофону production-сайт должен работать по HTTPS; localhost браузеры считают безопасным контекстом.

## Quartz и будущая синхронизация e-qanun

```env
EQANUN_SYNC_ENABLED=true
EQANUN_SYNC_CRON=0 0 3 * * ?
EQANUN_SYNC_TIME_ZONE=Asia/Baku
```

По умолчанию Quartz вызывает `EqanunLawSyncJob` каждый день в 03:00 по Баку. `@DisallowConcurrentExecution` не допускает параллельные запуски одной синхронизации. Cron использует Quartz format с полем секунд, например каждые 15 минут:

```env
EQANUN_SYNC_CRON=0 0/15 * * * ?
```

Текущий `PlaceholderEqanunLawSyncService` намеренно не имитирует внешний источник. В нём оставлен `TODO(eq-anun)` для реализации API/DB actuality check. Подготовлены:

- `EqanunLawSyncService` — вызываемый scheduler-ом orchestration port;
- `EqanunLawCandidate` — внешний ID, title, source URL и дата изменения;
- `EqanunDocParser` / `DefaultEqanunDocParser` — extraction старого бинарного `.doc` через Apache POI HWPF, legal cleanup и structure parsing;
- `EqanunParsedLaw` — результат, готовый для дальнейшего version mapping и ingestion.

При реализации источника замените placeholder-сервис, загрузите изменённые `.doc`, передайте stream в `EqanunDocParser`, затем свяжите e-qanun metadata с `document_group_id`, `version_number`, `valid_from`, `valid_to`, `is_current` и поставьте документ в ingestion queue.

## Endpoints

| Method   | Path                                | Назначение                       |
| -------- | ----------------------------------- | -------------------------------- |
| GET      | `/admin`                            | Dashboard и метрики              |
| GET      | `/admin/documents`                  | Список документов                |
| GET/POST | `/admin/documents/upload`           | Форма и загрузка                 |
| GET      | `/admin/documents/{id}`             | Статус и chunks                  |
| POST     | `/admin/documents/{id}/reprocess`   | Повторная обработка              |
| POST     | `/admin/documents/{id}/delete`      | Удаление                         |
| GET      | `/admin/search`                     | Semantic search + microphone STT |
| POST     | `/admin/api/transcriptions`         | Audio → text                     |
| GET      | `/actuator/health`                  | Health check без auth            |

Удаление сделано POST, а не HTTP DELETE, потому что UI использует обычные HTML forms с CSRF protection.

## Сборка и тесты

Если локально нет Java 21/Maven:

```bash
docker run --rm -v "$PWD:/workspace" -w /workspace   maven:3.9.11-eclipse-temurin-21 mvn clean test
```

Обычная локальная сборка:

```bash
mvn clean package
```

## Архитектурные границы

- `document/controller` — только HTTP/MVC flow.
- `document/service` — upload/query/lifecycle/state transitions.
- `storage` — оригинальные файлы.
- `ingestion/extractor` — format-specific extraction into blocks.
- `ingestion/cleaner` — conservative legal text cleanup.
- `ingestion/parser` — legal hierarchy.
- `ingestion/chunker` — context-aware chunks.
- `embedding` — provider abstraction and batch calls.
- `ai/generation` — OpenAI-compatible text generation port/adapter.
- `ai/transcription` — speech-to-text port, adapter и browser HTTP endpoint.
- `eqanun/scheduler` — Quartz job/trigger без concurrent execution.
- `eqanun/service` и `eqanun/parser` — extension points для actuality sync и `.doc` parsing.
- `job` — durable PostgreSQL ingestion queue and scheduled worker.
- `DocumentChunkStore` — explicit JDBC boundary for pgvector operations.

Подробный план: [`docs/plans/2026-07-23-legal-ai-mvp.md`](docs/plans/2026-07-23-legal-ai-mvp.md).

## Production notes

- Поменяйте оба пароля; `.env` игнорируется Git.
- Используйте `EMBEDDING_PROVIDER=openai` или отдельную multilingual model для реального semantic retrieval.
- Публичную публикацию делайте через HTTPS reverse proxy; приложение по умолчанию слушает только `127.0.0.1` host port.
- Для большой коллекции добавьте HNSW index после накопления representative data и измерения recall/latency.
