import { fetcher, parseResponse, revalidate } from '../Fetch/helpers'
import { decode } from '../Filters/helpers'

export const onSave = async ({ projectId, query, state, dispatch }) => {
  const BASE = `/api/v1/projects/${projectId}/datasets/${state.datasetId}`

  dispatch({ isLoading: true, errors: {} })

  try {
    await fetcher(`${BASE}/add_labels_by_search_filters/`, {
      method: 'PUT',
      body: JSON.stringify({
        filters: decode({ query }),
        label: state.lastLabel,
        scope: state.lastScope,
      }),
    })

    await revalidate({ key: `${BASE}/get_labels/` })

    dispatch({ isLoading: false, labels: {} })
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}
