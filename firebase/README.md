# Community loadouts: Firebase setup and admin

The Community tab stores published loadouts in a Firebase Realtime Database.
Players never log in: each install signs in anonymously, which gives it an id.

## Setup

1. Create a project at https://console.firebase.google.com (the free Spark plan is enough; it never bills).
2. Build > Authentication > Sign-in method: enable **Anonymous**.
3. Build > Realtime Database: create a database, start in locked mode.
4. Rules tab: paste `database.rules.json` and publish. Or use the Firebase CLI:
   `firebase deploy --only database` with `"database": {"rules": "firebase/database.rules.json"}` in `firebase.json`.
5. Copy the database URL (Realtime Database > Data, top of the tree) and the
   web API key (Project settings > General) into `DATABASE_URL` and `API_KEY` in
   `src/main/java/com/inventoryhover/CommunityLoadouts.java`. The API key is
   public by design; the rules protect the data.

Until both values are set, the Community tab says it is not available.

## Data

```
users/<uid>/<slot 0-9>: { name, created, settings, author?, hidden? }
installs/<owner uid>/<slot>/<installer uid>: true
official/<slot>: { name, created, settings }   (default loadouts, admin only)
banned/<uid>: true
config/writesOpen: false   (optional kill switch)
```

The rules allow each install 10 slots, only its own writes and deletes, names of
3 to 32 letters, numbers, spaces and ' _ . ! ? ( ) -, a server timestamp, and
settings up to 64 KB. Players cannot set any other field.

`author` is the publisher's game name, sent only when they untick Anonymous: 1 to
12 letters, numbers, spaces, `_` or `-`. It cannot be checked against the game, so
someone sending requests by hand could claim any name; hide such entries as below.

Default loadouts live under `official` and show by the author "Default", which
players cannot publish under. Players cannot write there; edit
`firebase/defaults.py`, run it, then upload as an admin (in Git Bash, set
`MSYS_NO_PATHCONV=1` so the path is not rewritten):

```
python firebase/defaults.py
firebase database:set /official firebase/defaults.json -f
```

Their downloads count under `installs/default/<slot>`.

Download counts live under `installs`. Each install may add only its own id, with
the value `true`, under a loadout that exists and is not its own. The owner may
only clear a slot's count, which the plugin does on delete and when a new
loadout takes a free slot. New anonymous accounts can inflate a count, so treat
it as a rough signal.

Name words are checked in the plugin with the public PurgoMalum service
(purgomalum.com) before publishing. The rules cannot call outside services, so
someone sending requests by hand could skip that check; remove or hide such
entries as below.

## Admin

Project: `click-helpers-loadouts-2b22f`, database
`https://click-helpers-loadouts-2b22f-default-rtdb.firebaseio.com`. Console:
https://console.firebase.google.com/project/click-helpers-loadouts-2b22f/database

The console and the CLI act as the project owner and ignore the rules. In Git
Bash on Windows, run `export MSYS_NO_PATHCONV=1` first, or paths like `/users`
are rewritten into Windows paths and the commands fail with "Path must begin with /".

- **Remove a loadout:** Data tab, delete `users/<uid>/<slot>`, or
  `firebase database:remove /users/<uid>/<slot>`.
- **Hide a loadout but keep its slot used:** set `users/<uid>/<slot>/hidden` to `true`.
- **Ban an install:** set `banned/<uid>` to `true`. It can no longer publish or delete.
- **Close publishing:** set `config/writesOpen` to `false`. Browsing keeps working.
- **Back up:** the free plan has no automatic backups. The Windows scheduled task
  "Click Helpers Firebase backup" runs `scripts/backup-firebase.ps1` every Sunday
  at 3 PM (or at next login if the PC was off), saving the whole database to
  `%USERPROFILE%\.click-helpers-backups\loadouts-<date>.json` and keeping the
  newest 12. Results go to `backup.log` in the same folder.
- **Restore:** `firebase database:set / <backup file>` overwrites the whole database
  with a backup.
- **Co-admins:** Project settings > Users and permissions.
