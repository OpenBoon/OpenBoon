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
