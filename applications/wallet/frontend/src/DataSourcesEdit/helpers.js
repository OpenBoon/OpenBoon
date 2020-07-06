import Router from 'next/router'

import { fetcher, formatQueryParams } from '../Fetch/helpers'

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
  state: { name, uri, fileTypes, modules, credentials },
}) => {
  dispatch({ isLoading: true })

  try {
    const { jobId } = await fetcher(
      `/api/v1/projects/${projectId}/data_sources/${dataSourceId}/`,
      {
        method: 'PUT',
        body: JSON.stringify({
          name,
          uri,
          fileTypes: Object.keys(fileTypes).filter((f) => fileTypes[f]),
          modules: Object.keys(modules).filter((m) => modules[m]),
          credentials,
        }),
      },
    )

    const queryParams = formatQueryParams({
      action: 'edit-datasource-success',
      jobId,
    })

    Router.push(
      `/[projectId]/data-sources${queryParams}`,
      `/${projectId}/data-sources${queryParams}`,
    )
  } catch (response) {
    try {
      const errors = await response.json()

      const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
        acc[errorKey] = errors[errorKey].join(' ')
        return acc
      }, {})

      dispatch({ isLoading: false, errors: parsedErrors })

      window.scrollTo(0, 0)
    } catch (error) {
      dispatch({
        isLoading: false,
        errors: { global: 'Something went wrong. Please try again.' },
      })
    }
  }
}
