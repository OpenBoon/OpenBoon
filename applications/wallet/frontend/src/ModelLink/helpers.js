import { mutate } from 'swr'
import Router from 'next/router'

import {
  fetcher,
  revalidate,
  getQueryString,
  parseResponse,
} from '../Fetch/helpers'

export const onExistingLink = async ({
  projectId,
  datasetId,
  model,
  dispatch,
}) => {
  dispatch({ isLoading: true, errors: {} })

  try {
    await fetcher(`/api/v1/projects/${projectId}/models/${model.id}/`, {
      method: 'PATCH',
      body: JSON.stringify({ datasetId }),
    })

    await Promise.all([
      revalidate({
        key: `/api/v1/projects/${projectId}/datasets/${datasetId}/`,
      }),

      revalidate({
        key: `/api/v1/projects/${projectId}/datasets/${datasetId}/get_labels/`,
      }),
    ])

    const queryString = getQueryString({
      action: 'link-dataset-success',
    })

    Router.push(
      `/[projectId]/models/[modelId]${queryString}`,
      `/${projectId}/models/${model.id}`,
    )

    mutate(
      `/api/v1/projects/${projectId}/models/${model.id}/`,
      { ...model, datasetId },
      true,
    )
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}

export const onNewLink = async ({
  projectId,
  model,
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

    onExistingLink({ projectId, datasetId, model, dispatch })
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}
