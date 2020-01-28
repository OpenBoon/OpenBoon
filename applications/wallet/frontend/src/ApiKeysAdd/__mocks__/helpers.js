/**
 * onSubmit
 */

export const onSubmit = ({ dispatch }) =>
  dispatch({
    apikey: { permissions: ['ApiKeyManage'], secretKey: 'FooBarSecretKey' },
  })

/**
 * onCopy
 */

let mockOnCopy = () => {}

export const __setMockOnCopy = fn => {
  mockOnCopy = fn
}

export const onCopy = (...args) => {
  mockOnCopy(...args)
}
