import { mutate, cache } from 'swr'
import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'
import { formatUrl } from '../Filters/helpers'

/* istanbul ignore next */
const KEYS_TO_UPDATE = cache.keys().filter((key) => {
  return key.includes('/searches')
})

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

    setIsLoading(false)

    /* istanbul ignore next */
    KEYS_TO_UPDATE.forEach((key) =>
      mutate(
        key,
        fetch(key).then((res) => res.json()),
      ),
    )

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
