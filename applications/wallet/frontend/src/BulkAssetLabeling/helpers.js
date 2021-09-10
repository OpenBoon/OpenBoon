import Router from 'next/router'

import {
  fetcher,
  parseResponse,
  revalidate,
  getQueryString,
} from '../Fetch/helpers'
import { cleanup, decode } from '../Filters/helpers'

export const onSave = async ({ projectId, query, state, dispatch }) => {
  const BASE = `/api/v1/projects/${projectId}/datasets/${state.datasetId}`

  dispatch({ isLoading: true, errors: {} })

  try {
    await fetcher(`${BASE}/add_labels_by_search_filters/`, {
      method: 'PUT',
      body: JSON.stringify({
        filters: decode({ query: cleanup({ query }) }),
        label: state.lastLabel,
        testRatio: (100 - state.trainPct) / 100,
      }),
    })

    await revalidate({ key: `${BASE}/get_labels/` })

    dispatch({ isLoading: false, labels: {}, errors: {} })

    const action = 'bulk-labeling-success'

    Router.push(
      `/[projectId]/visualizer${getQueryString({ query, action })}`,
      `/${projectId}/visualizer${getQueryString({ query })}`,
    )
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}
