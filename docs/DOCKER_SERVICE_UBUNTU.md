# Running MedExpertMatch Stack as a System Service (Ubuntu 24.04)

This guide explains how to run the full Docker Compose stack (PostgreSQL, application with embedded docs at /docs) as a systemd service on Ubuntu 24.04. When the service is **enabled**, it **auto-starts on reboot**; you can manage it with `systemctl`.

## Prerequisites

- Ubuntu 24.04 (or compatible)
- Docker Engine and Docker Compose v2 installed
- Project cloned or copied to the host (e.g. `/home/berdachuk/projects-ai/med-expert-match-ce`)

Ensure Docker is installed and your user can run Docker (e.g. in the `docker` group). The systemd service runs as root and uses the system Docker daemon.

## 1. Build the Stack (one-time or after code changes)

From the project directory:

```bash
cd /home/berdachuk/projects-ai/med-expert-match-ce
docker compose build
```

Optional: build without cache for a clean build: `docker compose build --no-cache`.

## 2. Install the Systemd Unit

Copy the service file and reload systemd:

```bash
sudo cp /home/berdachuk/projects-ai/med-expert-match-ce/scripts/medexpertmatch-stack.service /etc/systemd/system/
sudo systemctl daemon-reload
```

### Apply on this host (Ubuntu 24.04, project at default path)

Run these in order (you will be prompted for your password):

```bash
sudo cp /home/berdachuk/projects-ai/med-expert-match-ce/scripts/medexpertmatch-stack.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable medexpertmatch-stack   # enables auto-start on reboot
sudo systemctl start medexpertmatch-stack
sudo systemctl status medexpertmatch-stack
```

After `enable`, the stack will start automatically after every reboot. If the stack was already running via `docker compose up -d`, you can stop it first with `docker compose down` in the project directory, then start it via the service so systemd manages it.

### Using a different project path

If the project is not at `/home/berdachuk/projects-ai/med-expert-match-ce`, edit the unit before copying:

```bash
sudo nano /etc/systemd/system/medexpertmatch-stack.service
```

Change the `WorkingDirectory=` line to your project path, then run `sudo systemctl daemon-reload`.

## 3. Enable and Start the Service

Enable the service so it starts on boot:

```bash
sudo systemctl enable medexpertmatch-stack
```

Start the stack now (without rebooting):

```bash
sudo systemctl start medexpertmatch-stack
```

Check status:

```bash
sudo systemctl status medexpertmatch-stack
```

Containers should be running. Verify:

- App: `curl -s http://localhost:8094/actuator/health`
- Docs: `curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8094/docs/`

### LLM (LM Studio)

The app container uses **host network** so it can reach LM Studio on the host at `127.0.0.1:1234` without requiring LM Studio to bind to all interfaces. Ensure LM Studio is running (e.g. via `lmstudio.service`; see `docs/LMSTUDIO_SYSTEMD.md`). The stack unit is configured to start **after** `lmstudio.service` when both are enabled, so the LLM is up before the app connects.

## 4. Service Commands Reference

| Action | Command |
|--------|---------|
| Start stack | `sudo systemctl start medexpertmatch-stack` |
| Stop stack | `sudo systemctl stop medexpertmatch-stack` |
| Restart stack | `sudo systemctl restart medexpertmatch-stack` |
| Status | `sudo systemctl status medexpertmatch-stack` |
| Enable on boot (auto-start on reboot) | `sudo systemctl enable medexpertmatch-stack` |
| Disable on boot | `sudo systemctl disable medexpertmatch-stack` |
| View logs | `journalctl -u medexpertmatch-stack -f` |

When the service is **enabled**, it auto-starts after every reboot. After `start`, the unit runs `docker compose up -d` and exits; containers keep running under Docker. After `stop`, the unit runs `docker compose down` and removes the containers (data in the volume is preserved).

## 5. Where Database Data Is Stored (Host)

PostgreSQL data is stored in a Docker named volume. On the host filesystem it is located at:

```
/var/lib/docker/volumes/med-expert-match-ce_medexpertmatch-postgres-data/_data
```

- **Volume name (Compose):** `medexpertmatch-postgres-data`
- **Full volume name (Docker):** `med-expert-match-ce_medexpertmatch-postgres-data` (project name + volume name)
- **Path on host:** `/var/lib/docker/volumes/med-expert-match-ce_medexpertmatch-postgres-data/_data`

To list volumes: `docker volume ls | grep medexpertmatch`.  
To inspect (shows Mountpoint): `docker volume inspect med-expert-match-ce_medexpertmatch-postgres-data`.

## 6. Permissions When Running as a Service

- The systemd unit runs as **root**. It executes `docker compose up -d` and `docker compose down`, so Docker has full access to its data directories.
- The **volume** `/var/lib/docker/volumes/.../_data` is created and owned by **root** (Docker daemon). The PostgreSQL process inside the container runs as user `postgres` (inside the container) and writes files into this volume; ownership of files inside `_data` is as seen inside the container (e.g. UID 999 for postgres).
- **Do not change ownership or permissions** of `/var/lib/docker/volumes/med-expert-match-ce_medexpertmatch-postgres-data` or `_data`. Docker and the postgres container manage them. Changing permissions can prevent the container from starting or cause data corruption.
- If you need to **back up** the database:
  - Prefer `pg_dump` from inside the container or from the host using the exposed port (e.g. `pg_dump -h localhost -p 5433 -U medexpertmatch medexpertmatch`).
  - If you copy the volume directory, use root: `sudo tar -czf backup.tar.gz -C /var/lib/docker/volumes/med-expert-match-ce_medexpertmatch-postgres-data _data`. Restore with care; ensure the stack is stopped and permissions are not altered.
- If the stack was previously run by a **non-root user** (e.g. your user in the docker group), the volume is still created by the Docker daemon (root). Switching to the systemd service does not require any permission changes; the same volume is used.

## 7. After Updating the Project

After pulling code or changing images:

1. Rebuild if needed: `docker compose build` (or `build --no-cache`).
2. Restart the service so it recreates containers with the new images:  
   `sudo systemctl restart medexpertmatch-stack`

Database data in the volume persists across restarts and rebuilds.

## 8. Troubleshooting

- **Containers do not start:** Run `docker compose up -d` manually from the project directory and check `docker compose logs`. Fix any build or config errors, then `sudo systemctl restart medexpertmatch-stack`.
- **Permission denied on volume:** Do not chown/chmod the volume. Ensure no other process has changed ownership of `/var/lib/docker/volumes/med-expert-match-ce_medexpertmatch-postgres-data`. If it was modified, restore from backup or re-init the database (volume remove and fresh start; data will be lost).
- **Service fails to start:** Check `journalctl -u medexpertmatch-stack -n 50`. Ensure Docker is running (`systemctl status docker`) and `WorkingDirectory` in the unit points to the correct project path.
- **App cannot connect to LLM:** Ensure LM Studio is running on the host (`curl -s http://127.0.0.1:1234/v1/models`). The app uses host network and connects to `127.0.0.1:1234`. If you use `lmstudio.service`, enable it so it starts before the stack: `sudo systemctl enable lmstudio.service`.
