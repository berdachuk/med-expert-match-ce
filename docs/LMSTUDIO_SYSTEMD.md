# LM Studio Headless on Ubuntu (systemd)

To have LM Studio come up after reboot and keep models available/loaded on Ubuntu 24.04, run the headless daemon via systemd and preload models at boot. Reference: [LM Studio Headless](https://lmstudio.ai/docs/developer/core/headless).

## Option A (recommended): systemd service

### 1. Install headless daemon and CLI

```bash
curl -fsSL https://lmstudio.ai/install.sh | bash
~/.lmstudio/bin/lms --help
```

### 2. Manual test once

Model paths for `lms load` must match the names shown by `lms ls` (on-disk paths). They can differ from the API model `id` returned by `/v1/models`. Run `lms ls` and use the exact path for each model you want to preload.

```bash
~/.lmstudio/bin/lms ls
~/.lmstudio/bin/lms load <path-from-ls>   # e.g. qwen/qwen3-4b-2507
~/.lmstudio/bin/lms server start
curl http://127.0.0.1:1234/v1/models
~/.lmstudio/bin/lms server stop
```

### 3. Install the systemd unit

From the project root:

```bash
sudo cp scripts/lmstudio.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now lmstudio.service
systemctl status lmstudio
curl http://127.0.0.1:1234/v1/models
```

The unit preloads the models used by MedExpertMatch (chat, embedding, tool-calling). Edit `scripts/lmstudio.service` to change model IDs or add/remove `lms load` lines. **Model paths in `lms load` must match the output of `lms ls`**; if a load fails (e.g. "Cannot find a model matching the provided path"), run `lms ls` and use the exact path shown. The MedGemma line is commented out in the default unit because its on-disk path may differ from the API id `medgemma-1.5-4b-it@q4_k_m`; uncomment and fix the path after running `lms ls` if you want it preloaded.

### 4. Useful commands

- `systemctl status lmstudio` – check status
- `sudo systemctl stop lmstudio` – stop server
- `sudo systemctl start lmstudio` – start server
- `journalctl -u lmstudio -f` – follow logs

## Option B: GUI and "start on login"

On a desktop Ubuntu with LM Studio GUI installed, you can enable "run the LLM server on login" in app settings. The server keeps running when the app is minimized to tray. This depends on a user login session, not boot-time systemd.

## Model "loaded" vs "downloaded"

Depending on LM Studio settings (JIT loading), `/v1/models` may list downloaded models, not only models currently in memory. The systemd unit uses explicit `lms load` so models are loaded into memory at boot. If you need different models or JIT-only, adjust or remove the `ExecStartPre ... lms load ...` lines.

## Headless vs desktop

- **Headless (no GUI):** Use Option A with the systemd unit as given (system-wide, `multi-user.target`). Preloading ensures models are ready before MedExpertMatch or Docker stack starts.
- **Desktop (GUI):** Option A still works; alternatively use Option B. If you use systemd, consider `systemd --user` and `WantedBy=default.target` so the service runs in your user session; the project unit is written for system-wide boot.
