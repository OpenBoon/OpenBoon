/**
 * authenticateUser()
 */
let mockAuthenticateUser = () => {}

export const __setMockAuthenticateUser = (fn) => {
  mockAuthenticateUser = fn
}

export const authenticateUser = () => (...args) => {
  mockAuthenticateUser(...args)
}

/**
 * logout()
 */
export const logout = () => {}
