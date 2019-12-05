# Terraform Deployment of the Zorroa Machine Learning PLatform (ZMLP) on Google Kubernetes Engine (GKE)
This directory contains terraform modules for each of the individual services
required by ZMLP and a main script that imports all services and deploys them.

### NOTE: Deployment is a work-in-progress and currently only deploys the wallet service.

## Deploying a new instance of ZMLP to GKE
Below is a step-by-step guide to deploying a new instance of ZMLP on GKE.

1. Run `gcloud auth application-default login`
1. Make sure your google user has "Storage Admin" permissions in the "zorroa-deploy" project.
1. Download and install [terraform](https://www.terraform.io/downloads.html).
1. Create a service account (with json key) for terraform in the GCP project where ZVI will be deployed.
1. Create a file in the root of this repo call "terraform.tfvars"
1. Read through "variables.tf" file in the root of the repo and add all required variables 
to the "<PROJECT>.auto.tfvars" file. You can also add any of the optional variables if you need
to adjust the deployment. The syntax is `variable-name = "value"`, one per line.
1. Run `terraform init`.
1. Run `terraform workspace new <GCP_PROJECT_NAME>`
1. Run `terraform plan` and sanity check the output to make sure everything makes sense.
1. Run `terraform apply`.
1. Store your <PROJECT>.auto.tfvars file in gs://zorroa-deploy-state/terraform/tfvars 
bucket so that other team members are able to manage the state of the deployment.
 
## Creating Custom Deployments
If you need to customize your plan with additional modules/services that were created for 
your deployment, use the root `main.tf` in this directory as a reference, and create 
your own under the `plans` directory.

As-is, the `main.tf` right here creates a basic ZMLP deployment in GCP. 

## Re-Pulling images for services.
You will likely want your services to repull an image tag such as `development` or `qa`.
Kubernetes does not offer a standard way of doing this but you can trigger a repull of the
code by changing something else about the deployment. Below are instructions for the 
workaround.

Prerequisites:
1. `gcloud` installed and configured for your project.

1. Configure kubectl. In the GCP console there will be a "connect" button next to your GKE 
cluster with a command to configure. It should look like this - `gcloud container clusters get-credentials zorroa --zone us-central1-a --project <GCP_PROJECT_NAME>`.
1. Run `kubectl patch deployment <DEPLOYMENT_NAME> -p  "{\"spec\":{\"template\":{\"metadata\":{\"labels\":{\"date\":\"`date +'%s'`\"}}}}}"`.
 DEPLOYMENT_NAME would be archivist, analyst, wallet, or elasticsearch.

