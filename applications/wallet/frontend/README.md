# Welcome to the "Wallet Frontend"

The `wallet` project provides monitoring and control tools for Boon AI. This
version uses the [Next.js](https://nextjs.org/) framework to facilitate
server-side rendering and enforce best practices. It relies very much on the
opinions of the underlying tools described below.

- [Tooling](#tooling)
- [Conventions](#conventions)
- [Getting Started](#getting-started)
- [Libraries](#libraries)

## Tooling

[ESLint](#eslint) and [Prettier](#prettier) are used for formatting.
[Jest](#jest) is used for testing, as well as
[react-test-renderer](https://reactjs.org/docs/test-renderer.html). All rules
are enforced with [pre-commit hooks](#pre-commit-hooks). Dependencies are
managed with [npm](https://www.npmjs.com/get-npm).

### ESLint

[ESLint](http://eslint.org/) is a linting utility for JavaScript. We almost
completely adhere to the
[Airbnb JavaScript Style Guide](https://github.com/airbnb/javascript) through
the
[eslint-config-airbnb](https://github.com/airbnb/javascript/tree/master/packages/eslint-config-airbnb)
plugin. The only exception being that we don't use `.jsx` file extensions.
Please get familiar with this style guide.

Linting is checked with a [pre-commit hook](#pre-commit-hooks), but not
automatically fixed. You can run it with `npm run lint`.

### Prettier

[Prettier](https://prettier.io/) is an
[opinionated](http://jlongster.com/A-Prettier-Formatter) JavaScript formatter.
It removes all original styling and ensures that all outputted JavaScript
conforms to a consistent style.

Prettier formatting is enforced with a [pre-commit hook](#pre-commit-hooks) that
will automatically reformat your code as you commit it. We strongly suggest you
install a
[plugin for your favorite editor](https://prettier.io/docs/en/editors.html) that
reformats your code every time you save it.

### Jest testing

[Jest](https://jestjs.io/) is a modern, low configuration testing platform, that
comes with built-in code coverage reports. Use `npm test` to run tests in watch
mode as you code, and `npm run test:cover` to generate a code coverage report.
Tests are also run as a [pre-commit hook](#pre-commit-hooks).

### Pre-commit hooks

[Husky](https://github.com/typicode/husky) is our git-hooks mechanism.
[lint-staged](https://github.com/okonet/lint-staged) allows us to run linters
and tests only against staged git files. Make sure your pre-commit hooks are
working.

## Conventions

There are two main folders at the root of the application: `/pages` and `/src`.

#### pages

`pages` are a convention dictated by the Next.js framework and correspond to
routed `urls`.

#### src

The `src` folder contains everything else, and assumes that everything is a
component. Not necessarily in the sense of a React component, but more in the
sense of a unit of domain context. For a lengthy explanation of how to structure
and name files and components, please consult
[this article](https://link.medium.com/fmSm5hOEsS).

The gist is that it is better to group related things together, close to the
root folder, than to have a deeply nested file structure where we prematurely
try to organize things. Better discoverability and lower mental overhead are
key.

## Getting Started

### Installation

Clone the project:
`git clone git@gitlab.com:zorroa-zvi/zmlp.git && cd zmlp/applications/wallet/frontend`

Install dependencies: `npm install` (or `npm ci`)

### Environment Variables

Copy the `.env.template` file over to `.env` by using
`cp -prv .env.template .env`

Request any empty environment variable you might need.

### Development

#### Anonymous / Offline / Mocked Mode

Run `npm run dev:mocked` to start a local development node server on
http://localhost:3000, where every API call will be mocked instead of going to
the network. This is very convenient when you are working on some UI code for
instance, and don't want to have to log in and authenticate every time your
session ends. It should also allow you to work offline.

#### Local Mode

To run a local environment of Boon AI Console, you need to build the docker
setup (`docker-compose build`) and run it (`docker-compose up`). Refer to the
overall `wallet` or `wallet/app` docs for further information.

Run `npm run dev` to start a local development node server on
http://localhost:3000. This starts the node.js / next.js SSR development server
and includes a local development proxy to avoid CORS issues when making API
calls to `localhost`.

### Production

Run `npm run prod` to build and run a production build, or `npm run build` and
`npm start` to do each step individually.

## Libraries

### SWR

[SWR](https://swr.now.sh/) is our primary data fetching mechanism. It's a React
Hooks library that first returns the data from cache (stale), then sends the
fetch request (revalidate), and finally comes with the up-to-date data again. In
the future we might feel the need for some sort of global state management
mechanism, at which point we will consider the use of React Context.

### Emotion

[Emotion](https://emotion.sh/) is our library of choice for `css-in-js` styling.
We use `Plain Old JavaScript Objects` rather than `Tagged Template Literals`
inside the `css` prop.

### SVGR

[svgr](https://github.com/smooth-code/svgr) allows us to import `svg` files
direclty into our React components. They will render inline and can be styled
like any other component.
