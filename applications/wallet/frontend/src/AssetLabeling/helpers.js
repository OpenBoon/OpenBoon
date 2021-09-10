import { useLocalStorage } from '../LocalStorage/helpers'
import { fetcher, parseResponse, revalidate } from '../Fetch/helpers'

export const SCOPE_OPTIONS = [
  { value: 'TRAIN', label: 'Train' },
  { value: 'TEST', label: 'Test' },
]

const INITIAL_STATE = {
  datasetId: '',
  lastLabel: '',
  lastScope: 'TRAIN',
  trainPct: 50,
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
  if (labels.length === 1 && labels[0].label === '' && state.lastLabel !== '') {
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
  if (labels.length === 1) {
    return { label: state.lastLabel, scope: state.lastScope }
  }

  // Empty state
  return {
    label: '',
    scope: SCOPE_OPTIONS[0].value,
  }
}

const getBody = ({ assetId, state, labels }) => {
  if (
    labels.length === 1 &&
    Object.keys(state.labels).length === 0 &&
    state.lastLabel !== ''
  ) {
    return {
      addLabels: [
        {
          assetId,
          label: state.lastLabel,
          scope: state.lastScope,
          bbox: labels[0].bbox,
          simhash: labels[0].simhash,
        },
      ],
    }
  }

  if (state.datasetType === 'Classification') {
    const { scope, label } = Object.values(state.labels)[0]

    return { addLabels: [{ assetId, label, scope }] }
  }

  return {
    addLabels: Object.entries(state.labels).map(
      ([bbox, { scope, label, simhash }]) => {
        return { assetId, bbox: JSON.parse(bbox), scope, label, simhash }
      },
    ),
  }
}

export const onSave = async ({
  projectId,
  assetId,
  state,
  labels,
  dispatch,
}) => {
  const body = getBody({ assetId, state, labels })

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

    dispatch({ isLoading: false, labels: {} })
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
  label: { label, scope, bbox, simhash },
}) => {
  const BASE = `/api/v1/projects/${projectId}/datasets/${datasetId}`

  const id = bbox ? JSON.stringify(bbox) : assetId

  dispatch({ isLoading: true, errors: {} })

  try {
    await fetcher(`${BASE}/delete_labels/`, {
      method: 'DELETE',
      body: JSON.stringify({
        removeLabels: [{ assetId, label, scope, bbox, simhash }],
      }),
    })

    await Promise.all([
      revalidate({ key: `${BASE}/get_labels/` }),

      revalidate({ key: `/api/v1/projects/${projectId}/assets/${assetId}/` }),

      revalidate({ key: `${BASE}/label_tool_info/?assetId=${assetId}` }),
    ])

    dispatch({
      isLoading: false,
      labels: { ...labels, [id]: { label, scope, bbox, simhash } },
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
