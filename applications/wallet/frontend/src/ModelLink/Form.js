import { useReducer } from 'react'
import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing } from '../Styles'

import Form from '../Form'
import FlashMessageErrors from '../FlashMessage/Errors'
import SectionTitle from '../SectionTitle'
import Radio from '../Radio'

import ModelLinkExisting from './Existing'
import ModelLinkNew from './New'

const SOURCES = [
  { value: 'EXISTING', label: 'Use Existing Dataset' },
  { value: 'NEW', label: 'Create New' },
]

const INITIAL_STATE = {
  source: SOURCES[0].value,
  datasetId: '',
  name: '',
  description: '',
  type: '',
  isLoading: false,
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const ModelLinkForm = ({ model }) => {
  const {
    query: { projectId },
  } = useRouter()

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  const {
    data: { results: datasetTypes },
  } = useSWR(`/api/v1/projects/${projectId}/datasets/dataset_types/`)

  return (
    <Form>
      <FlashMessageErrors
        errors={state.errors}
        styles={{ marginTop: -spacing.base, paddingBottom: spacing.comfy }}
      />

      <SectionTitle>Link a Dataset</SectionTitle>

      {SOURCES.map(({ value, label }) => {
        return (
          <div key={value} css={{ paddingTop: spacing.normal }}>
            <Radio
              name="source"
              option={{
                value,
                label,
                legend: '',
                initialValue: state.source === value,
              }}
              onClick={({ value: source }) => {
                dispatch({ ...INITIAL_STATE, source })
              }}
            />
          </div>
        )
      })}

      {state.source === SOURCES[0].value ? (
        <ModelLinkExisting
          projectId={projectId}
          model={model}
          state={state}
          dispatch={dispatch}
        />
      ) : (
        <ModelLinkNew
          projectId={projectId}
          model={model}
          datasetTypes={datasetTypes}
          state={state}
          dispatch={dispatch}
        />
      )}
    </Form>
  )
}

ModelLinkForm.propTypes = {
  model: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    datasetId: PropTypes.string,
    runningJobId: PropTypes.string.isRequired,
    state: PropTypes.string.isRequired,
    datasetType: PropTypes.string.isRequired,
  }).isRequired,
}

export default ModelLinkForm
