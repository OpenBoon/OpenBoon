/**
 * fetch
 */
require('jest-fetch-mock').enableMocks()

beforeEach(() => {
  fetch.resetMocks()
  require('swr').__setMockUseSWRResponse({ data: {} })
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

/**
 * scrollTo
 */
beforeEach(() => {
  if (typeof global.window !== 'undefined') {
    global.window.scrollTo = () => {}
  }
})

/**
 * clipboard
 */
beforeEach(() => {
  if (typeof global.navigator !== 'undefined') {
    global.navigator.clipboard = { writeText: () => {} }
  }
})
