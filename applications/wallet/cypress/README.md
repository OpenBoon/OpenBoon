# Wallet Integration Tests with Cypress

## Getting Started

### Installation

Clone the project:
`git clone git@gitlab.com:zorroa-zvi/zmlp.git && cd zmlp/applications/wallet/cypress`

Install dependencies: `npm install` (or `npm ci`)

Configure the [ESLint](http://eslint.org/) and
[Prettier](https://prettier.io/docs/en/editors.html) plugins in your code
editor.

### Environment Variables

Copy the `cypress.env.json.template` file over to `cypress.env.json` by using
`cp -prv cypress.env.json.template cypress.env.json`

You will need to provide a username and their password for the logged in tests
to pass.

Alternatively, or on a CI environment, you can use environment variables with
the `CYPRESS_` prefix added to variables of the same name. For instance
`{ "BASE_URL": "http://localhost:3000" }` in the `json` file becomes
`CYPRESS_BASE_URL=http://localhost:3000` in the GitLab secrets.

### Running the tests

`npm run gui` will launch the Cypress UI to run the tests on your computer

`npm run cli` will run the tests in a headless browser in a terminal

### Assumptions

- there should be a `cypress@zorroa.com` User
- this User should have access to a `Cypress` Project
- this Project should have at least one Asset in the Visualizer
- there should be at least one Model named `custom-model`
- there should be at least one Asset with a `location.city` of `Long Beach`
- there should be at least one Asset with a `boonai-label-detection` of a
  `daisy`
