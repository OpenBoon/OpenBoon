# Apikeys
Contains views for getting apikeys from ZMLP.

## Setup for ZMLP ApiKey Testing

ApiKeys only work on a ZMLP-based Wallet instance. Additionally, due to the permissions
and order of operations required to use the keys on Wallet, the initial setup required 
is very specific.

### Build and Prep Docker
1. Run `./buildx` in the root of the repository to create up to date container images.
2. Make sure you're not using a Docker Compose Override file (likely named 
"docker-compose.override.yml" and sitting next to the Wallet Dockerfile). If one exists
either delete it or rename it so that it is not used. The standard docker-compose file is 
setup to create a ZMLP based Wallet instance and the override is not needed.
3. To simplify things it is advised that at least the first time you run this you start
with a completely fresh docker-compose instance. The easiest way to do this is to verify
that docker is not running any containers (`docker-compose down` if so), and to remove
any previously created volumes. You can do this with `docker volume prune`. **This will
remove any stored data in your instances.**
4. In the root of the repo, run `docker-compose up` or `docker-compose up -d` to daemonize
the processes to run in the background.

### Setup Project Zero & Membership
1. Confirm the compose environment has come up successfully by visiting  `localhost` in
your browser. You should see the login page.
2. Login with the development `admin/admin` user/pass. This should land you in a Wallet
Job view with no projects.
3. Visit `localhost/admin` in your browser to go to the Django Admin page.
4. Click on `Projects` and confirm there are no projects (assuming this is a fresh
instance). 
5. Create the initial "Project Zero" project. Click `Add Project +` in the upper right.
For the `Id` use `00000000-0000-0000-0000-000000000000`, and for the `Name` call it 
`Project Zero`. The name can technically be anything, but this will help make it clear.
Save the Project. **This has created the project in Wallet, but NOT in ZMLP.** 
6. Generate the base64 encoded api key we'll use for a membership to Project Zero. On the
command line navigate to `dev/config/keys` in the repository. Use the `dump-key.py` script
on the `inception-key.json` to get the encoded key. The command you should run will be
`./dump-key.py inception-key.json`. Record the output of the script for the next step.
7. Back in the Django Admin, create a new Membership. For the `User` select `admin`
from the drop down.  For the `Project`, select `Project Zero`. For the `Apikey`, copy and
paste the value generated in Step 9. Save the Membership.
8. Visit the browsable API at `localhost/api/v1` in your browser. 
9. Click on the URL value for `Projects`. You should see the Project Zero project in the
list of `results`.

### Sync Project Zero to ZMLP
1. From the Project list in the browsable api, we need to sync Project Zero to also exist
in ZMLP. Do this by using the `HTML Form` at the bottom of the screen and sending the `ID`
and `Name` of Project Zero through a POST request. You should receive a 201 response.
2. Navigate back to the Project List. You can do this clicking the `GET` button in the top
right.

### Create & Test ApiKeys
1. Now click on the `apikeys` URL in the Project Zero detail to see the list of available
keys. At this point you'll likely see only one Api Key in the response for a `job-runner`.
2. You can now create an apikey for this Project with the browsable API. Switch to the
`Raw Data` view and change the media type to `application/json`. You're only required
to send the `name` and `permissions` arguments with the request. The available permissions
can be found at the `permissions` endpoint on the Project Detail view. Let's use the 
following example json body and POST the request:
    ```json
    {"name":  "My ApiKey", "permissions": ["ApiKeyManage", "ProjectManage"]}
    ```
3. If you navigate back to the list of ApiKeys you should see your newly created ApiKey
in the list of keys.

#### Notes
* After Project Zero is created, it is **highly** recommended to only create Projects
using the browsable api/endpoint directly. This will ensure projects are created in both
Wallet and ZMLP. Using the Django admin only creates projects in Wallet, and the project
will need to be synced to ZMLP by using the endpoint like we did in the instructions above.
* When a new Project is created, it will **not** show up in the Project list until you
create a `Membership` to that project in the Django Admin page.