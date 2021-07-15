import Router from 'next/router'

import {
  fetcher,
  revalidate,
  getQueryString,
  parseResponse,
} from '../Fetch/helpers'

export const onExistingLink = async ({
  projectId,
  modelId,
  datasetId,
  dispatch,
}) => {
  dispatch({ isLoading: true, errors: {} })

  try {
    await fetcher(`/api/v1/projects/${projectId}/models/${modelId}/`, {
      method: 'PATCH',
      body: JSON.stringify({ datasetId }),
    })

    await revalidate({
      key: `/api/v1/projects/${projectId}/models/${modelId}/`,
    })

    const queryString = getQueryString({
      action: 'link-dataset-success',
    })

    Router.push(
      `/[projectId]/models/[modelId]${queryString}`,
      `/${projectId}/models/${modelId}`,
    )
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}

export const onNewLink = async ({
  projectId,
  modelId,
  state: { name, description, type },
  dispatch,
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

    onExistingLink({ projectId, modelId, datasetId, dispatch })
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}
