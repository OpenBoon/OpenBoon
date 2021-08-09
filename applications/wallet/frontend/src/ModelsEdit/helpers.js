import { mutate } from 'swr'
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
  model,
  state: { name, description },
}) => {
  dispatch({ isLoading: true, errors: {} })

  try {
    await fetcher(`/api/v1/projects/${projectId}/models/${model.id}/`, {
      method: 'PATCH',
      body: JSON.stringify({ name, description }),
    })

    await Promise.all([
      revalidate({
        key: `/api/v1/projects/${projectId}/models/all/`,
      }),

      revalidate({
        key: `/api/v1/projects/${projectId}/models/`,
      }),

      mutate(
        `/api/v1/projects/${projectId}/models/${model.id}/`,
        { ...model, name, description },
        true,
      ),
    ])

    const queryString = getQueryString({ action: 'edit-model-success' })

    Router.push(
      `/[projectId]/models/[modelId]${queryString}`,
      `/${projectId}/models/${model.id}`,
    )
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}
