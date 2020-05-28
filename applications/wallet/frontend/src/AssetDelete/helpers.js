import { cache } from 'swr'
import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'
import { formatUrl } from '../Filters/helpers'

export const onDelete = async ({
  projectId,
  assetId,
  query,
  setIsLoading,
  setError,
}) => {
  try {
    setIsLoading(true)

    await fetcher(`/api/v1/projects/${projectId}/assets/${assetId}/`, {
      method: 'DELETE',
    })

    cache.clear()

    return Router.push(
      {
        pathname: '/[projectId]/visualizer',
        query: { query, id: '', action: 'delete-asset-success' },
      },
      `/${projectId}/visualizer${formatUrl({
        id: '',
        query,
        action: 'delete-asset-success',
      })}`,
    )
  } catch (error) {
    setIsLoading(false)
    return setError('There was an error. Please try again.')
  }
}
