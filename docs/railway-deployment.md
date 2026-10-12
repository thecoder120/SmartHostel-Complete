# Deploy Smart Hostel on Railway

This project uses three Railway services: the managed MySQL template, the Spring Boot backend, and the Nginx/React frontend. Only the frontend should receive a public domain. The backend connects to MySQL and the frontend over Railway's private network.

## Create the project

1. In Railway, create a project from the public GitHub repository `thecoder120/SmartHostel-Complete`.
2. Add the Railway MySQL template/service. Keep its database variables private.
3. Add a backend service from the same GitHub repository. Set its root directory to `/backend` and deploy with the included `Dockerfile`.
4. Add a frontend service from the same repository. Set its root directory to `/frontend` and deploy with the included `Dockerfile`.

If Railway asks for GitHub access beyond the public repository, review the requested permissions before granting them.

## Backend variables

Set these in the backend service's Variables. Use Railway variable references for the MySQL service (named `MySQL` here; adjust the reference prefix if you named it differently):

```dotenv
SERVER_ADDRESS=0.0.0.0
DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=${{MySQL.MYSQLUSER}}
DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
SESSION_COOKIE_SECURE=true
```

Railway supplies `PORT` automatically. Do not set `DB_PASSWORD` in GitHub or commit it to the repository. `STAFF_INVITE_CODE` is optional because registration is open; leave it unset unless you specifically want a bootstrap code for the first account.

Login sessions are stored in the MySQL database with Spring Session JDBC, so they remain consistent across backend instances and restarts. The backend initializes the Spring Session tables on startup.

## Frontend variables and public URL

Set this variable on the frontend service:

```dotenv
BACKEND_ORIGIN=http://backend.railway.internal:8080
```

Replace `backend` in the hostname if the backend service has another name. Generate a public domain for the frontend service in Railway. Keep the backend service private. After Railway gives the frontend URL, set the backend's `CORS_ALLOWED_ORIGINS` to that exact HTTPS origin, for example `https://your-app.up.railway.app` (no trailing slash). Redeploy the backend after changing variables.

The frontend Nginx configuration reads `BACKEND_ORIGIN` at container startup. It defaults to `http://backend:8080` for Docker Compose local deployments.

## Verify the deployment

Wait for MySQL and the backend to become healthy, then open the frontend's HTTPS URL. Staff registration is open: the first account becomes the administrator, and later accounts are staff by default. Anyone who can reach the public site can register and access hostel records. Administrators can still issue optional, single-use staff invites from the app. Confirm sign-out and sign-in work before entering real resident information.

Railway is usage based. Review the current plan, usage, and spending controls in Railway before deploying; enable a spending limit if one is available on your plan. Back up the database regularly and treat backups as sensitive resident data.
