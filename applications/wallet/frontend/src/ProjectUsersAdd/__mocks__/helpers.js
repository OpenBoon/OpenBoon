/**
 * onSubmit
 */

export const onSubmit = ({ dispatch }) =>
  dispatch({
    succeeded: [
      {
        email: 'jane@zorroa.com',
        permissions: [
          'SuperAdmin',
          'ProjectAdmin',
          'AssetsRead',
          'AssetsImport',
        ],
      },
    ],
    failed: [
      {
        email: 'joe@zorroa.com',
        permissions: [
          'SuperAdmin',
          'ProjectAdmin',
          'AssetsRead',
          'AssetsImport',
        ],
      },
    ],
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
