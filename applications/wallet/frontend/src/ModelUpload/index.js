import { useReducer } from 'react'
import { useRouter } from 'next/router'

import ModelUploadSelection from './Selection'
import ModelUploadConfirmation from './Confirmation'
import ModelUploadProgress from './Progress'

const INITIAL_STATE = {
  file: undefined,
  isConfirmed: false,
  progress: 0,
  hasFailed: false,
  request: undefined,
}

const reducer = (state, action) => ({ ...state, ...action })

const ModelUpload = () => {
  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  const {
    query: { projectId, modelId },
  } = useRouter()

  if (!state.file) {
    return <ModelUploadSelection dispatch={dispatch} />
  }

  if (state.file && !state.isConfirmed) {
    return (
      <ModelUploadConfirmation
        projectId={projectId}
        modelId={modelId}
        state={state}
        dispatch={dispatch}
      />
    )
  }

  return <ModelUploadProgress state={state} dispatch={dispatch} />
}

export default ModelUpload
