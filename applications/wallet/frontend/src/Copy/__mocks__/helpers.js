/**
 * onCopy
 */

let mockOnCopy = () => {}

export const __setMockOnCopy = (fn) => {
  mockOnCopy = fn
}

export const onCopy = (...args) => {
  mockOnCopy(...args)
}
