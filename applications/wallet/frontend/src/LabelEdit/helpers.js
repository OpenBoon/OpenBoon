import Router from 'next/router'

import { fetcher, revalidate } from '../Fetch/helpers'

export const onSubmit = async ({
  projectId,
  modelId,
  dispatch,
  state: { label, newLabel },
}) => {
  try {
    dispatch({
      isLoading: true,
      error: {},
    })

    await fetcher(
      `/api/v1/projects/${projectId}/models/${modelId}/rename_label/`,
      {
        body: JSON.stringify({ label, newLabel }),
        method: 'PUT',
      },
    )

    await revalidate({
      key: `/api/v1/projects/${projectId}/models/`,
      paginated: true,
    })

    Router.push(
      '/[projectId]/models/[modelId]?action=edit-label-success',
      `/${projectId}/models/${modelId}`,
    )
  } catch (error) {
    dispatch({
      isLoading: false,
      errors: { global: 'Something went wrong. Please try again.' },
    })
  }
}
