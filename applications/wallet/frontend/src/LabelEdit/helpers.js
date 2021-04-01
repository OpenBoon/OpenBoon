import Router from 'next/router'

import { fetcher, revalidate, parseResponse } from '../Fetch/helpers'

export const onSubmit = async ({
  projectId,
  modelId,
  dispatch,
  state: { label, newLabel },
}) => {
  dispatch({
    isLoading: true,
    errors: {},
  })

  try {
    await fetcher(
      `/api/v1/projects/${projectId}/models/${modelId}/rename_label/`,
      {
        body: JSON.stringify({ label, newLabel }),
        method: 'PUT',
      },
    )

    await revalidate({
      key: `/api/v1/projects/${projectId}/models/${modelId}/get_labels/`,
    })

    Router.push(
      '/[projectId]/models/[modelId]?action=edit-label-success',
      `/${projectId}/models/${modelId}`,
    )
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}
