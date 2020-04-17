import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'

export const getInitialModules = ({
  initialState: { modules: existingModules },
  providers,
}) => {
  const results = {}

  providers.forEach(({ categories }) =>
    categories.forEach(({ modules }) =>
      modules.forEach(({ id, name }) => {
        if (existingModules.includes(id)) {
          results[name] = true
        }
      }),
    ),
  )

  return results
}

export const onSubmit = async ({
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
          fileTypes: Object.keys(fileTypes),
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
