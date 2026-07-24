(() => {
  const button = document.querySelector("[data-stt-button]");
  if (!button) return;

  const label = button.querySelector("[data-stt-label]");
  const timer = document.querySelector("[data-recording-time]");
  const message = document.querySelector("[data-stt-message]");
  const query = document.querySelector("#search-query");
  const language = document.querySelector("[data-stt-language]");
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

  let recorder;
  let stream;
  let phase = "idle";
  let startedAt;
  let timerId;

  const setMessage = (text, error = false) => {
    message.textContent = text;
    message.classList.toggle("error", error);
    message.classList.toggle("hidden", !text);
  };
  const formatDuration = (seconds) => `${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`;
  const preferredMimeType = () => ["audio/webm;codecs=opus", "audio/webm", "audio/ogg;codecs=opus", "audio/mp4"].find((type) => MediaRecorder.isTypeSupported(type)) || "";
  const extensionFor = (type) => type.includes("ogg") ? "ogg" : type.includes("mp4") ? "m4a" : "webm";

  const resetRecorderUi = (activeStream) => {
    clearInterval(timerId);
    activeStream?.getTracks().forEach((track) => track.stop());
    if (stream === activeStream) stream = undefined;
    recorder = undefined;
    button.classList.remove("recording");
    timer.classList.add("hidden");
  };

  const setIdle = () => {
    phase = "idle";
    button.disabled = false;
    label.textContent = "Голосовой ввод";
  };

  const upload = async (blob) => {
    phase = "uploading";
    setMessage("Распознаю запись…");
    button.disabled = true;
    label.textContent = "Распознавание…";
    const form = new FormData();
    form.append("audio", blob, `recording.${extensionFor(blob.type)}`);
    form.append("language", language?.value || "");
    const headers = {};
    if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
    try {
      const response = await fetch("/admin/api/transcriptions", { method: "POST", body: form, headers });
      const payload = await response.json().catch(() => ({ error: "Некорректный ответ сервера" }));
      if (!response.ok) throw new Error(payload.error || "Не удалось распознать речь");
      query.value = [query.value.trim(), payload.text].filter(Boolean).join(" ");
      query.focus();
      setMessage("Текст распознан и добавлен в запрос.");
    } catch (error) {
      setMessage(error.message || "Ошибка распознавания речи", true);
    } finally {
      setIdle();
    }
  };

  const stop = () => {
    if (phase !== "recording" || recorder?.state !== "recording") return;
    phase = "stopping";
    button.disabled = true;
    label.textContent = "Останавливаю…";
    recorder.stop();
  };

  const start = async () => {
    if (phase !== "idle") return;
    if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
      setMessage("Этот браузер не поддерживает запись с микрофона.", true);
      return;
    }

    phase = "requesting";
    button.disabled = true;
    label.textContent = "Доступ к микрофону…";
    setMessage("");
    try {
      const activeStream = await navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true } });
      const mimeType = preferredMimeType();
      const activeRecorder = new MediaRecorder(activeStream, mimeType ? { mimeType } : undefined);
      const chunks = [];
      stream = activeStream;
      recorder = activeRecorder;

      activeRecorder.addEventListener("dataavailable", (event) => {
        if (event.data.size > 0) chunks.push(event.data);
      });
      activeRecorder.addEventListener("error", () => {
        resetRecorderUi(activeStream);
        setIdle();
        setMessage("Браузер прервал запись с микрофона.", true);
      }, { once: true });
      activeRecorder.addEventListener("stop", async () => {
        const blob = new Blob(chunks, { type: activeRecorder.mimeType || mimeType || "audio/webm" });
        resetRecorderUi(activeStream);
        if (blob.size > 0) await upload(blob);
        else {
          setIdle();
          setMessage("Запись получилась пустой.", true);
        }
      }, { once: true });

      activeRecorder.start(500);
      phase = "recording";
      startedAt = Date.now();
      button.disabled = false;
      button.classList.add("recording");
      label.textContent = "Остановить запись";
      timer.classList.remove("hidden");
      timer.textContent = "00:00";
      timerId = setInterval(() => {
        const elapsed = Math.floor((Date.now() - startedAt) / 1000);
        timer.textContent = formatDuration(elapsed);
        if (elapsed >= 60) stop();
      }, 500);
    } catch (error) {
      stream?.getTracks().forEach((track) => track.stop());
      stream = undefined;
      recorder = undefined;
      setIdle();
      setMessage(error.name === "NotAllowedError" ? "Разрешите доступ к микрофону в браузере." : "Не удалось открыть микрофон.", true);
    }
  };

  button.addEventListener("click", () => phase === "recording" ? stop() : start());
  window.addEventListener("pagehide", () => stream?.getTracks().forEach((track) => track.stop()));
})();
