/**
 * on
 */

let mockOnFunction = () => {}

export const __setMockOnFunction = fn => {
  mockOnFunction = fn
}

/**
 * off
 */

let mockOffFunction = () => {}

export const __setMockOffFunction = fn => {
  mockOffFunction = fn
}

/**
 * Router
 */

const Router = {
  events: {
    on: (...args) => {
      mockOnFunction(...args)
    },
    off: (...args) => {
      mockOffFunction(...args)
    },
  },
}

export default Router
