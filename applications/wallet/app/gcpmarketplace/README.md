# GCP Marketplace
This app handles integration with the GCP Marketplace.`The app handles the Pub/Sub event
handling, sign up flow and usage reporting. Below are the components in the app and how they
relate to these integration points.

## Models

### MarketplaceAccount
Tracks which Users were created via GCP Marketplace and keeps a record of the associated 
marketplace account ID.

### MarketplaceEntitlement
Tracks which Projects were created via GCP Marketplace and a keeps a record of the associated
marketplace entitlement ID.

## Views

### /marketplace/signup
Once a user purchases a subscription via marketplace they will be directed to this page to 
create an account. Marketplace sends a POST request with a JWT in the form data with 
information on the Marketplace account. The view detects this type of request and renders
a form for the user to enter their email address. This form also has a hidden input
with the JWT. When the view gets a post with the email address and token it creates 
MarketplaceAccount in the DB and reports back to marketplace via Pub/Sub that the signup 
flow is complete.

### /marketplace/signup_success
The signup page above redirects to this page on success. It is a static page with a success 
message that directs the user to return to marketplace to complete setup.

## Management Commands
There are two management commands which start services that are responsible for the main 
integrations with marketplace. These commands are deployed in containers in the 
gcp-marketplace-integration service in the kubernetes cluster.

### gcpmarketplace-pubsub
Starts a service that listens for Pub/Sub messages from marketplace and handles them. The
service supports creation of new subscriptions, changes to subscriptions and cancellation.

### gcpmarketlplace-usage-report
Starts a service that sends usage reports for all marketplace-associated projects on an 
hourly basis.

## Configuration (Environment Variables)
The following settings must be configured for the integration to work. They are currently
configured to pull values from environment variables of the same name. All of this info
should be provided by the Google marketplace team.

| Setting | Description |
| ---- | ---- |
| MARKETPLACE_PROJECT_ID | GCP project ID where the marketplace app is configured.|
| MARKETPLACE_PUBSUB_SUBSCRIPTION | Pub/Sub subscription name that will have marketplace events. |
| MARKETPLACE_SERVICE_NAME | Name of the marketplace service. |
| MARKETPLACE_CREDENTIALS | JSON Service Account (SA) key for the SA that has been whitelisted for use with the marketplace APIs. |

## More Info
If you'd like more info on how the entire marketplace integration works here are some good 
resources.

- [How-To Guide for Marketplace Integration](https://cloud.google.com/marketplace/docs/partners/integrated-saas)
- [Codelab for backend integration](https://codelabs.developers.google.com/codelabs/gcp-marketplace-integrated-saas/#0)
- [Procurement API Docs](https://cloud.google.com/marketplace/docs/partners/commerce-procurement-api/reference)
