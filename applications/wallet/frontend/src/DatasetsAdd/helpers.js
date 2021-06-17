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
  state: { name, description, type },
}) => {
  dispatch({ isLoading: true, errors: {} })

  try {
    const { id: datasetId } = await fetcher(
      `/api/v1/projects/${projectId}/datasets/`,
      {
        method: 'POST',
        body: JSON.stringify({ name, description, type }),
      },
    )

    await revalidate({
      key: `/api/v1/projects/${projectId}/datasets/`,
    })

    const queryString = getQueryString({
      action: 'add-dataset-success',
      datasetId,
    })

    Router.push(`/[projectId]/datasets${queryString}`, `/${projectId}/datasets`)
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}
