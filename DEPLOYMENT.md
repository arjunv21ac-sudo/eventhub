# 🚀 Deploying EventHub (free)

| Part | Service | Free plan |
|---|---|---|
| Code | GitHub | Free |
| Database | Aiven for MySQL | 1 GB storage, no credit card |
| Backend | Render (Docker web service) | 512 MB RAM, sleeps after 15 min idle |
| Frontend | Vercel | Free for personal projects |

Follow the steps **in order**. Total time: about 45 minutes.

---

## Step 1: Push the code to GitHub

1. Install Git: download it from https://git-scm.com/download/win and keep the default options.
2. Sign in at https://github.com → **New repository** → name it `eventhub` → **Public** → don't add a README → **Create repository**.
3. Open a terminal in the `eventhub` folder and run (replace `YOUR-USERNAME`):

```bash
git init
git add .
git commit -m "EventHub: event ticketing with QR check-in"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/eventhub.git
git push -u origin main
```

The first push opens a browser window to log in to GitHub.

> ✅ Check: `node_modules`, `target` and `dist` must **not** appear on GitHub (`.gitignore` excludes them).

---

## Step 2: Create the MySQL database (Aiven)

1. Sign up at https://aiven.io → **Create service** → **MySQL** → choose the **Free plan** → pick a region close to you → **Create**.
2. Wait until the status is **Running** (2–5 minutes).
3. On the service **Overview** page, note: **Host**, **Port**, **User** (`avnadmin`), **Password**, **Database** (`defaultdb`).
4. Build your JDBC URL from them:

```
jdbc:mysql://HOST:PORT/defaultdb?sslMode=REQUIRED
```

Hibernate creates the tables automatically on the first start.

---

## Step 3: Deploy the backend (Render)

1. Sign up at https://render.com with your GitHub account.
2. **New → Web Service** → select the `eventhub` repository.
3. Settings:
   - **Root Directory:** `backend`
   - **Language / Runtime:** Docker
   - **Instance type:** Free
4. **Environment variables:**

| Key | Value |
|---|---|
| `DB_URL` | the JDBC URL from Step 2 |
| `DB_USERNAME` | `avnadmin` |
| `DB_PASSWORD` | the Aiven password |
| `JWT_SECRET` | a long random string, 40+ characters (never reuse the dev default) |
| `CORS_ORIGINS` | leave as `http://localhost:5173` for now; update in Step 5 |
| `SEED_DATA` | `true` (creates the demo accounts and events once) |

5. **Create Web Service.** The first build takes 5–10 minutes.
6. When it says **Live**, open `https://YOUR-APP.onrender.com/api/events`. You should see JSON with 4 events.
7. Swagger docs are at `https://YOUR-APP.onrender.com/swagger-ui.html`.

> ⏳ The free plan sleeps after 15 minutes without traffic. The first request after that takes about 1 minute. Open the backend link a minute before an interview demo.

---

## Step 4: Deploy the frontend (Vercel)

1. Sign up at https://vercel.com with your GitHub account.
2. **Add New → Project** → import `eventhub`.
3. Settings:
   - **Root Directory:** `frontend`
   - **Framework preset:** Vite (detected automatically)
4. **Environment variable:**

| Key | Value |
|---|---|
| `VITE_API_URL` | `https://YOUR-APP.onrender.com/api` |

5. **Deploy.** You get a URL like `https://eventhub-yourname.vercel.app`.

---

## Step 5: Connect frontend and backend

Back in **Render → your service → Environment**:

| Key | Value |
|---|---|
| `CORS_ORIGINS` | `https://eventhub-yourname.vercel.app` (no slash at the end) |
| `FRONTEND_URL` | `https://eventhub-yourname.vercel.app` |

Save. Render restarts the backend automatically. Then open the Vercel URL and log in with a demo account.

> 📷 The camera scanner works on your phone too, because Vercel serves the site over HTTPS.

---

## Step 6 (optional): Turn on booking emails with Gmail

1. Turn on **2-Step Verification** for your Google account.
2. Go to https://myaccount.google.com/apppasswords → create an app password named "EventHub".
3. Add these in Render → Environment:

| Key | Value |
|---|---|
| `MAIL_ENABLED` | `true` |
| `MAIL_USERNAME` | your Gmail address |
| `MAIL_PASSWORD` | the 16-character app password |

Book a ticket. An email with the QR codes arrives within a few seconds.

> 🔒 Never commit passwords or secrets to GitHub. They belong only in Render's environment variables.

---

## Step 7: Put the links everywhere

Update the **Live demo** links at the top of `README.md`, then add them to:
- Your resume (project section)
- LinkedIn → Featured
- The GitHub repository's **About** box (⚙️ → Website)

## Troubleshooting

| Problem | Fix |
|---|---|
| Frontend shows "Could not reach the server" | Backend is asleep (wait 1 min) or `VITE_API_URL` is wrong. Redeploy Vercel after changing it. |
| Browser console shows a CORS error | `CORS_ORIGINS` on Render must exactly match the Vercel URL, with `https://` and no trailing slash. |
| Render logs: `Communications link failure` | Wrong `DB_URL`; check host, port and `?sslMode=REQUIRED`. |
| Render logs: `Access denied for user` | Wrong `DB_USERNAME` / `DB_PASSWORD`. |
| Refreshing a page on Vercel gives 404 | `frontend/vercel.json` must be committed. |
| No email arrives | Check Render logs for "Could not send confirmation email". Use an App Password, not your Gmail password. |
