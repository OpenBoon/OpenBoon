import { useLocalStorage } from '../LocalStorage/helpers'
import { fetcher, parseResponse, revalidate } from '../Fetch/helpers'

export const SCOPE_OPTIONS = [
  { value: 'TRAIN', label: 'Train' },
  { value: 'TEST', label: 'Test' },
]

const INITIAL_STATE = {
  datasetId: '',
  labels: {},
  isLoading: false,
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

export const useLabelTool = ({ projectId }) => {
  return useLocalStorage({
    key: `AssetLabelingContent.${projectId}`,
    initialState: INITIAL_STATE,
    reducer,
  })
}

export const getIsDisabled = ({ assetId, state, labels }) => {
  // Loading
  if (state.isLoading) return true

  // Unique unlabeled label with any other label input
  if (
    state.labels &&
    Object.values(state.labels).length === 1 &&
    labels.length === 1 &&
    labels[0].label === ''
  ) {
    return false
  }

  // Any unlabeled label with matching label input
  const unsavedLabel = labels.find(({ bbox, label }) => {
    const id = bbox ? JSON.stringify(bbox) : assetId

    if (!label && state.labels[id] && state.labels[id].label) {
      return true
    }

    return false
  })

  return !unsavedLabel
}

export const getLabelState = ({ id, state, labels }) => {
  // Matching label input
  if (state.labels && state.labels[id]) {
    return state.labels[id]
  }

  // Unique other label input
  if (
    state.labels &&
    Object.values(state.labels).length === 1 &&
    labels.length === 1
  ) {
    return Object.values(state.labels)[0]
  }

  // Empty state
  return {
    label: '',
    scope: SCOPE_OPTIONS[0].value,
  }
}

const getBody = ({ assetId, state }) => {
  if (state.datasetType === 'Classification') {
    /**
     * Important to use `assetId` here instead of the
     * `Object.keys(state.labels)[0]` so that we can save
     * the same label across multiple assets:
     */
    const { scope, label } = Object.values(state.labels)[0]

    return { addLabels: [{ assetId, label, scope }] }
  }

  return {
    addLabels: Object.entries(state.labels).map(([bbox, { scope, label }]) => {
      return { assetId, bbox: JSON.parse(bbox), scope, label }
    }),
  }
}

export const onSave = async ({ projectId, assetId, state, dispatch }) => {
  const body = getBody({ assetId, state })

  if (body.addLabels.length === 0) return

  const BASE = `/api/v1/projects/${projectId}/datasets/${state.datasetId}`

  dispatch({ isLoading: true, errors: {} })

  try {
    await fetcher(`${BASE}/add_labels/`, {
      method: 'POST',
      body: JSON.stringify(body),
    })

    await Promise.all([
      revalidate({ key: `${BASE}/get_labels/` }),

      revalidate({ key: `/api/v1/projects/${projectId}/assets/${assetId}/` }),

      revalidate({ key: `${BASE}/label_tool_info/?assetId=${assetId}` }),
    ])

    dispatch({ isLoading: false })
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}

export const onDelete = async ({
  projectId,
  datasetId,
  assetId,
  dispatch,
  labels,
  label: { label, bbox, scope },
}) => {
  const BASE = `/api/v1/projects/${projectId}/datasets/${datasetId}`

  const id = bbox ? JSON.stringify(bbox) : assetId

  dispatch({ isLoading: true, errors: {} })

  try {
    await fetcher(`${BASE}/delete_labels/`, {
      method: 'DELETE',
      body: JSON.stringify({ removeLabels: [{ assetId, label, bbox }] }),
    })

    await Promise.all([
      revalidate({ key: `${BASE}/get_labels/` }),

      revalidate({ key: `/api/v1/projects/${projectId}/assets/${assetId}/` }),

      revalidate({ key: `${BASE}/label_tool_info/?assetId=${assetId}` }),
    ])

    dispatch({
      isLoading: false,
      labels: { ...labels, [id]: { label, bbox, scope } },
    })
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}

export const onFaceDetect = async ({
  projectId,
  datasetId,
  assetId,
  dispatch,
}) => {
  const BASE = `/api/v1/projects/${projectId}/datasets/${datasetId}`

  dispatch({ isLoading: true, errors: {} })

  try {
    await fetcher(
      `/api/v1/projects/${projectId}/assets/${assetId}/detect_faces/`,
      { method: 'PATCH' },
    )

    await Promise.all([
      revalidate({ key: `/api/v1/projects/${projectId}/assets/${assetId}/` }),

      revalidate({ key: `${BASE}/label_tool_info/?assetId=${assetId}` }),
    ])

    dispatch({ isLoading: false })
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}
