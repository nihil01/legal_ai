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
- Production-синхронизация кодексов e-qanun через Quartz с source versioning и durable ingestion queue.

## Быстрый запуск MVP

```bash
cp .env.example .env
# Обязательно замените DATABASE_PASSWORD и ADMIN_PASSWORD.
# Для доступа из LAN/VPN укажите APP_BIND_ADDRESS=0.0.0.0 и закройте порт firewall-ом от Internet.

docker compose up -d --build
docker compose ps
```

Открыть страницу входа: <http://127.0.0.1:8080/login>. Браузер перенаправляет все анонимные HTML-запросы на отдельную форму входа; логин и пароль берутся из `.env`. Basic Auth сохранён для smoke-проверок и автоматизации.

После входа доступны:

- dashboard со статусами pipeline;
- загрузка DOC, DOCX, PDF, HTML и TXT;
- список документов с поиском и пагинацией;
- карточка документа со статусом job, metadata и структурными chunks;
- повторная обработка и удаление;
- semantic search по завершённым текущим версиям;
- безопасный logout с CSRF.

Для быстрой демонстрации загрузите `samples/demo-service-agreement.txt`: pipeline распознаёт части, разделы, главы и 12 статей, после чего в карточке отображаются 12 самостоятельных chunks.

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

## Quartz и синхронизация e-qanun

```env
EQANUN_API_BASE_URL=https://api.e-qanun.az
EQANUN_PUBLIC_BASE_URL=https://e-qanun.az/framework/
EQANUN_ALLOWED_DOWNLOAD_HOSTS=frameworks.e-qanun.az
EQANUN_CODEX_IDS=46960,46944,46947,46945,46959,46943,46948,46940,46941,46942,46946,46950,46951,46952,46953,46955,46956,46957,46958,56187
EQANUN_CONNECT_TIMEOUT_SECONDS=10
EQANUN_READ_TIMEOUT_SECONDS=60
EQANUN_MAX_DOCUMENT_BYTES=26214400
EQANUN_SYNC_ENABLED=true
EQANUN_SYNC_CRON=0 0 3 * * ?
EQANUN_SYNC_TIME_ZONE=Asia/Baku
```

По умолчанию Quartz вызывает `EqanunLawSyncJob` каждый день в 03:00 по Баку. `@DisallowConcurrentExecution` не допускает параллельные запуски одной синхронизации. Cron использует Quartz format с полем секунд, например каждые 15 минут:

```env
EQANUN_SYNC_CRON=0 0/15 * * * ?
```

Реальный sync flow:

1. `HttpEqanunApiClient` получает `/getVersions`, разбирает даты `dd.MM.yyyy` и выбирает последнюю редакцию по дате и ID версии.
2. Уже импортированная комбинация `external_source + external_id + external_version_id` пропускается без скачивания.
3. `/downloadWord/{codexId}` возвращает URL документа; разрешены только HTTPS-ссылки от configured allowlist host. Формат определяется по magic bytes, потому что e-qanun может отдавать OOXML/DOCX с расширением `.doc` и MIME `application/msword`.
4. `DatabaseEqanunLawImporter` сохраняет нормализованный оригинал, связывает редакции через `document_group_id`, увеличивает локальный `version_number` и ставит новую неактивную редакцию в durable ingestion queue.
5. Worker вызывает `EqanunDocParser` внутри durable pipeline, затем создаёт legal structure, chunks и embeddings.
6. Только после успешного ingestion новая редакция атомарно становится `is_current=true`; предыдущая остаётся доступной до этого момента и затем получает `is_current=false` и `valid_to`.

Semantic search читает только chunks документов со статусом `COMPLETED` и `is_current=true`, поэтому не смешивает старые, неготовые и актуальные редакции.

Уникальные индексы и PostgreSQL advisory lock защищают от двойного импорта. Каждая новая внешняя version identity сохраняется отдельной локальной редакцией даже при совпадающем checksum; checksum-дедупликация применяется только к ручным загрузкам.

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

Проект фиксирует Maven `3.9.11` через Maven Wrapper и требует JDK 21:

```bash
./mvnw clean verify
```

### IntelliJ IDEA

Открывайте именно корень, содержащий `pom.xml`:

```text
/home/server/projects/legal_ai
```

Проверьте настройки:

1. `File → Project Structure → Project SDK` — JDK 21.
2. `Settings → Build Tools → Maven → Maven home path` — `Use Maven wrapper`.
3. `Settings → Build Tools → Maven → Importing → JDK for importer` — `Project SDK (21)`.
4. `Settings → Build Tools → Maven → Runner → JRE` — `Project SDK (21)`.
5. В Maven tool window нажмите `Reload All Maven Projects`.

Если локально нет JDK 21, сборку можно выполнить контейнером:

```bash
docker run --rm -v "$PWD:/workspace" -w /workspace \
  maven:3.9.11-eclipse-temurin-21 mvn clean verify
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
