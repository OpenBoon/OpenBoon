import Router from 'next/router'

import {
  fetcher,
  revalidate,
  getQueryString,
  parseResponse,
} from '../Fetch/helpers'

export const onSubmit = async ({
  dispatch,
  projectId,
  datasetId,
  state: { name, description, type },
}) => {
  dispatch({ isLoading: true, errors: {} })

  try {
    await fetcher(`/api/v1/projects/${projectId}/datasets/${datasetId}/`, {
      method: 'PUT',
      body: JSON.stringify({ id: datasetId, name, description, type }),
    })

    await Promise.all([
      revalidate({
        key: `/api/v1/projects/${projectId}/datasets/all/`,
      }),

      revalidate({
        key: `/api/v1/projects/${projectId}/datasets/`,
      }),

      revalidate({
        key: `/api/v1/projects/${projectId}/datasets/${datasetId}/`,
      }),
    ])

    const queryString = getQueryString({ action: 'edit-dataset-success' })

    Router.push(
      `/[projectId]/datasets/[datasetId]${queryString}`,
      `/${projectId}/datasets/${datasetId}`,
    )
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}
