import Router from 'next/router'

import {
  fetcher,
  getQueryString,
  parseResponse,
  revalidate,
} from '../Fetch/helpers'

export const getScopeLabelsCount = ({ labels, scope }) => {
  const scopeCount = `${scope.toLowerCase()}Count`

  const count = labels.reduce((acc, label) => {
    if (label[scopeCount] > 0) return acc + 1
    return acc
  }, 0)

  return count
}

export const onDelete = async ({
  query: { projectId, datasetId, query: q, page },
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
      `/api/v1/projects/${projectId}/datasets/${datasetId}/delete_labels/`,
      {
        method: 'DELETE',
        body: JSON.stringify(body),
      },
    )

    setErrors({})

    await revalidate({ key: url })

    Router.push(
      `/[projectId]/datasets/[datasetId]/labels${getQueryString({
        query: q,
        page,
        action: 'remove-asset-success',
      })}`,
      `/${projectId}/datasets/${datasetId}/labels${getQueryString({
        query: q,
        page,
      })}`,
    )
  } catch (response) {
    const errors = await parseResponse({ response })

    setErrors(errors)
  }
}
