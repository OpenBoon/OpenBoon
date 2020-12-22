import Router from 'next/router'

import {
  fetcher,
  getQueryString,
  parseResponse,
  revalidate,
} from '../Fetch/helpers'

export const onDelete = async ({
  query: { projectId, modelId, query: q, page },
  assetId,
  label,
  url,
  setErrors,
}) => {
  const body = {
    removeLabels: [{ assetId, label }],
  }

  try {
    await fetcher(
      `/api/v1/projects/${projectId}/models/${modelId}/delete_labels/`,
      {
        method: 'DELETE',
        body: JSON.stringify(body),
      },
    )

    setErrors({})

    await revalidate({ key: url })

    Router.push(
      `/[projectId]/models/[modelId]/assets${getQueryString({
        query: q,
        page,
        action: 'remove-asset-success',
      })}`,
      `/${projectId}/models/${modelId}/assets${getQueryString({
        query: q,
        page,
      })}`,
    )
  } catch (response) {
    const errors = await parseResponse({ response })

    setErrors(errors)
  }
}
