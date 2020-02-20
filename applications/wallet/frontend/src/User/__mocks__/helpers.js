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
 * setUser()
 */

let setUserFunction = () => () => {}

export const __setSetUserFunction = fn => {
  setUserFunction = fn
}

export const setUser = (...args) => {
  return setUserFunction(...args)
}
