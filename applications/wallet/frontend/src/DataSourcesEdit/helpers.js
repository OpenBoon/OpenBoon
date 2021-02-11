import Router from 'next/router'

import {
  fetcher,
  revalidate,
  getQueryString,
  parseResponse,
} from '../Fetch/helpers'

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
  dispatch({ isLoading: true, errors: {} })

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

    revalidate({
      key: `/api/v1/projects/${projectId}/data_sources/${dataSourceId}/`,
      paginated: false,
    })

    await revalidate({
      key: `/api/v1/projects/${projectId}/data_sources/`,
      paginated: true,
    })

    const queryString = getQueryString({
      action: 'edit-datasource-success',
      jobId,
    })

    Router.push(
      `/[projectId]/data-sources${queryString}`,
      `/${projectId}/data-sources`,
    )
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}
