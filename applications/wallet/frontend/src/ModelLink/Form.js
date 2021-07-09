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

const ModelLinkForm = ({ datasetType }) => {
  const {
    query: { projectId, modelId },
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
              onClick={({ value: source }) => dispatch({ source })}
            />
          </div>
        )
      })}

      {state.source === SOURCES[0].value ? (
        <ModelLinkExisting
          projectId={projectId}
          modelId={modelId}
          datasetType={datasetType}
          state={state}
          dispatch={dispatch}
        />
      ) : (
        <ModelLinkNew
          projectId={projectId}
          modelId={modelId}
          datasetType={datasetType}
          datasetTypes={datasetTypes}
          state={state}
          dispatch={dispatch}
        />
      )}
    </Form>
  )
}

ModelLinkForm.propTypes = {
  datasetType: PropTypes.string.isRequired,
}

export default ModelLinkForm
