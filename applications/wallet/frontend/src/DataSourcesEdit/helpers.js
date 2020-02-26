import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'

import { FILE_TYPES } from '../DataSourcesAdd/helpers'

export const onSubmit = async ({
  dispatch,
  dataSourceId,
  projectId,
  state: { name, uri, credential, fileTypes, modules },
}) => {
  try {
    await fetcher(
      `/api/v1/projects/${projectId}/datasources/${dataSourceId}/`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          name,
          uri,
          credential,
          file_types: Object.keys(fileTypes)
            .filter(f => fileTypes[f])
            .flatMap(f => {
              const { legend: extensions } = FILE_TYPES.find(
                ({ value }) => value === f,
              )
              return extensions.toLowerCase().split(',')
            }),
          modules: Object.keys(modules).filter(m => modules[m]),
        }),
      },
    )

    Router.push(
      '/[projectId]/data-sources?action=edit-datasource-success',
      `/${projectId}/data-sources?action=edit-datasource-success`,
    )
  } catch (response) {
    const errors = await response.json()

    const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
      acc[errorKey] = errors[errorKey].join(' ')
      return acc
    }, {})

    dispatch({ errors: parsedErrors })
  }
}
