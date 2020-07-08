/**
 * fetch
 */
require('jest-fetch-mock').enableMocks()

beforeEach(() => {
  fetch.resetMocks()
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

/**
 * localStorage
 */

beforeEach(() => {
  if (typeof localStorage !== 'undefined') {
    localStorage.clear()
  }
})
