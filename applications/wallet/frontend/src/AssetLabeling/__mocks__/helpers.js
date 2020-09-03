/**
 * onSubmit()
 */
let mockOnSubmit = () => {}

export const __setMockOnSubmit = (fn) => {
  mockOnSubmit = fn
}

export const onSubmit = (...args) => {
  mockOnSubmit(...args)
}
