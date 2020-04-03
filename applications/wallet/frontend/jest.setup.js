/**
 * fetch
 */
require('jest-fetch-mock').enableMocks()

const { initializeFetcher } = require('./src/Fetch/helpers')

beforeEach(() => {
  fetch.resetMocks()

  initializeFetcher()
})

/**
 * CSRF Cookie
 */

if (typeof document !== 'undefined') {
  Object.defineProperty(document, 'cookie', {
    writable: true,
    value: 'csrftoken=CSRF_TOKEN',
  })
}

/**
 * console
 */
const { error } = console

console.error = (message, ...args) => {
  error.apply(console, [message, ...args])
  throw new Error('Please fix console.error below')
}
