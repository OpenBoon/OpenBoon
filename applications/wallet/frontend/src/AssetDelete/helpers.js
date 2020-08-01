import { cache } from 'swr'
import Router from 'next/router'

import { fetcher, getQueryString } from '../Fetch/helpers'

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

    cache
      .keys()
      .filter((key) => key.includes('/searches'))
      .forEach((key) => cache.delete(key))

    await new Promise((resolve) => setTimeout(resolve, 500))

    return Router.push(
      `/[projectId]/visualizer${getQueryString({
        query,
        action: 'delete-asset-success',
      })}`,
      `/${projectId}/visualizer${getQueryString({ query })}`,
    )
  } catch (error) {
    setIsLoading(false)
    return setError('There was an error. Please try again.')
  }
}
