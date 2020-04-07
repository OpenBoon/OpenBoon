import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'

import { FILE_TYPES } from '../DataSourcesAdd/helpers'

export const onSubmitEdit = async ({
  dispatch,
  projectId,
  dataSourceId,
  state: { name, uri, credential, fileTypes, modules },
}) => {
  try {
    await fetcher(
      `/api/v1/projects/${projectId}/data_sources/${dataSourceId}/`,
      {
        method: 'PUT',
        body: JSON.stringify({
          name,
          uri,
          credential,
          file_types: Object.keys(fileTypes)
            .filter((f) => fileTypes[f])
            .flatMap((f) => {
              const { legend: extensions } = FILE_TYPES.find(
                ({ value }) => value === f,
              )
              return extensions.toLowerCase().split(',')
            }),
          modules: Object.keys(modules).filter((m) => modules[m]),
        }),
      },
    )

    Router.push(
      '/[projectId]/data-sources?action=edit-datasource-success',
      `/${projectId}/data-sources?action=edit-datasource-success`,
    )
  } catch (response) {
    try {
      const errors = await response.json()

      const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
        acc[errorKey] = errors[errorKey].join(' ')
        return acc
      }, {})

      dispatch({ errors: parsedErrors })

      window.scrollTo(0, 0)
    } catch (error) {
      dispatch({
        errors: { global: 'Something went wrong. Please try again.' },
      })
    }
  }
}
