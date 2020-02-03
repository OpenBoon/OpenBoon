/**
 * getUser()
 */

let mockUser = {}

export const __setMockUser = data => {
  mockUser = data
}

export const getUser = () => {
  return mockUser
}

/**
 * initializeUserstorer()
 */

export const initializeUserstorer = () => {}

/**
 * isUserAuthenticated()
 */

export const isUserAuthenticated = ({ refreshToken }) => {
  return refreshToken
}

/**
 * clearUser()
 */
export const clearUser = () => {}

/**
 * storeTokens()
 */
export const storeTokens = () => {}

/**
 * getTokenTimeout()
 */
export const getTokenTimeout = () => {
  return 10000
}

/**
 * authenticateUser()
 */
let mockAuthenticateUser = () => {}

export const __setMockAuthenticateUser = fn => {
  mockAuthenticateUser = fn
}

export const authenticateUser = () => (...args) => {
  mockAuthenticateUser(...args)
}

/**
 * logout()
 */
export const logout = () => {}

/**
 * fetcher()
 */
export const fetcher = () => () => {}
