# Apikeys
Contains views for getting apikeys from boonsdk.

## Setup for ZMLP ApiKey Testing

ApiKeys only work on a ZMLP-based Wallet instance. Additionally, due to the permissions
and order of operations required to use the keys on Wallet, the initial setup required 
is handled by an automatic database migration. If things appear to have gotten into a
weird state, restart your entire setup to rerun the migrations. 

### Create & Test ApiKeys
1. In the browsable api, click on the projects list endpoint.
2. Now, click on the `apikeys` URL in the Project Zero detail to see the list of available
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
* It is **highly** recommended to only create Projects
using the browsable api/endpoint directly. This will ensure projects are created in both
Wallet and ZMLP. Using the Django admin only creates projects in Wallet, and the project
will need to be synced to ZMLP by using the endpoint like we did in the instructions above.
* When a new Project is created, it will **not** show up in the Project list until you
create a `Membership` to that project in the Django Admin page.
