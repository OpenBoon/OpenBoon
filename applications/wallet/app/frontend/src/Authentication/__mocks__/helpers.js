/**
 * getTokens()
 */

let mockTokens = {}

export const __setMockTokens = data => {
  mockTokens = data
}

export const getTokens = () => {
  return mockTokens
}

/**
 * isUserAuthenticated()
 */

export const isUserAuthenticated = ({ refreshToken }) => {
  return refreshToken
}

/**
 * clearTokens()
 */
export const clearTokens = () => {}

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
