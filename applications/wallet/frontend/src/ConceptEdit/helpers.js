import Router from 'next/router'

import { fetcher, revalidate, parseResponse } from '../Fetch/helpers'

export const onSubmit = async ({
  projectId,
  datasetId,
  dispatch,
  state: { label, newLabel },
}) => {
  dispatch({
    isLoading: true,
    errors: {},
  })

  try {
    await fetcher(
      `/api/v1/projects/${projectId}/datasets/${datasetId}/rename_label/`,
      {
        body: JSON.stringify({ label, newLabel }),
        method: 'PUT',
      },
    )

    await revalidate({
      key: `/api/v1/projects/${projectId}/datasets/${datasetId}/get_labels/`,
    })

    Router.push(
      '/[projectId]/datasets/[datasetId]?action=edit-concept-success',
      `/${projectId}/datasets/${datasetId}`,
    )
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}
