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
    label.textContent = "Səslə daxiletmə";
  };

  const upload = async (blob) => {
    phase = "uploading";
    setMessage("Səs yazısı tanınır…");
    button.disabled = true;
    label.textContent = "Tanınma gedir…";
    const form = new FormData();
    form.append("audio", blob, `recording.${extensionFor(blob.type)}`);
    form.append("language", language?.value || "");
    const headers = {};
    if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
    try {
      const response = await fetch("/admin/api/transcriptions", { method: "POST", body: form, headers });
      const payload = await response.json().catch(() => ({ error: "Serverin cavabı düzgün deyil" }));
      if (!response.ok) throw new Error(payload.error || "Nitqi tanımaq mümkün olmadı");
      query.value = [query.value.trim(), payload.text].filter(Boolean).join(" ");
      query.focus();
      setMessage("Mətn tanındı və sorğuya əlavə edildi.");
    } catch (error) {
      setMessage(error.message || "Nitqin tanınması zamanı xəta baş verdi", true);
    } finally {
      setIdle();
    }
  };

  const stop = () => {
    if (phase !== "recording" || recorder?.state !== "recording") return;
    phase = "stopping";
    button.disabled = true;
    label.textContent = "Dayandırılır…";
    recorder.stop();
  };

  const start = async () => {
    if (phase !== "idle") return;
    if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
      setMessage("Bu brauzer mikrofonla səs yazısını dəstəkləmir.", true);
      return;
    }

    phase = "requesting";
    button.disabled = true;
    label.textContent = "Mikrofona giriş gözlənilir…";
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
        setMessage("Brauzer mikrofon yazısını dayandırdı.", true);
      }, { once: true });
      activeRecorder.addEventListener("stop", async () => {
        const blob = new Blob(chunks, { type: activeRecorder.mimeType || mimeType || "audio/webm" });
        resetRecorderUi(activeStream);
        if (blob.size > 0) await upload(blob);
        else {
          setIdle();
          setMessage("Səs yazısı boşdur.", true);
        }
      }, { once: true });

      activeRecorder.start(500);
      phase = "recording";
      startedAt = Date.now();
      button.disabled = false;
      button.classList.add("recording");
      label.textContent = "Yazını dayandır";
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
      setMessage(error.name === "NotAllowedError" ? "Brauzerdə mikrofona giriş icazəsi verin." : "Mikrofonu açmaq mümkün olmadı.", true);
    }
  };

  button.addEventListener("click", () => phase === "recording" ? stop() : start());
  window.addEventListener("pagehide", () => stream?.getTracks().forEach((track) => track.stop()));
})();